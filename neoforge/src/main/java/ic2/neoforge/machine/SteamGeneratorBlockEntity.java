package ic2.neoforge.machine;

import ic2.core.machine.SteamBoiler;
import ic2.neoforge.api.WorkCapabilities;
import ic2.neoforge.explosion.HeatExplosion;
import ic2.neoforge.fluid.FluidDefinition;
import ic2.neoforge.registration.ModFluids;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.transfer.MachineFluidTank;
import ic2.neoforge.transfer.ResourcePort;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/** Heat draw, fluid delivery and boiler state commit together; explosions happen after commit. */
public final class SteamGeneratorBlockEntity extends MachineBlockEntity implements FluidMachine {
    private SteamBoiler.State state = new SteamBoiler.State(0, 0, Long.MIN_VALUE);
    private SteamBoiler.Settings settings = new SteamBoiler.Settings(0, 0);
    private final StateJournal<SteamBoiler.State> journal =
            new StateJournal<>(() -> state, value -> state = value, this::setChanged);
    private final MachineFluidTank waterTank =
            new MachineFluidTank(10000, this::setChanged, SteamGeneratorBlockEntity::water);
    private final ResourceHandler<FluidResource> fluids =
            new ResourcePort<>(waterTank, slot -> true, slot -> false);
    private int heatInput, outputAmount;
    private SteamBoiler.Output output = SteamBoiler.Output.NONE;

    public SteamGeneratorBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModMachines.entityType(MachineKind.STEAM_GENERATOR), pos, blockState, 0);
    }

    public MachineFluidTank waterTank() {
        return waterTank;
    }

    public SteamBoiler.State boilerState() {
        return state;
    }

    public SteamBoiler.Settings settings() {
        return settings;
    }

    private static FluidResource fluid(FluidDefinition definition) {
        return FluidResource.of(ModFluids.FAMILIES.get(definition).source().get());
    }

    private static boolean water(FluidResource resource) {
        return resource.getFluid() == Fluids.WATER
                || resource.getFluid()
                        == ModFluids.FAMILIES.get(FluidDefinition.DISTILLED_WATER).source().get();
    }

    @Override
    public ResourceHandler<FluidResource> fluidAutomation(Direction side) {
        return fluids;
    }

    @Override
    public ResourceHandler<ItemResource> automation(Direction side) {
        return new ResourcePort<>(inventory, slot -> false, slot -> false);
    }

    @Override
    public void serverTick(ServerLevel level) {
        long tick = level.getGameTime();
        if (state.tick() == tick) return;
        var biome = level.getBiome(worldPosition);
        int ambient = biome.is(Tags.Biomes.IS_HOT) ? 45 : biome.is(Tags.Biomes.IS_COLD) ? 0 : 25;
        int drawn = 0, delivered = 0;
        boolean pressureBurst = false;
        SteamBoiler.Step step;
        try (var transaction = Transaction.openRoot()) {
            journal.updateSnapshots(transaction);
            if (!state.calcified()) drawn = drawHeat(level, transaction);
            var input = waterTank.getResource(0);
            boolean distilled =
                    input.getFluid()
                            == ModFluids.FAMILIES
                                    .get(FluidDefinition.DISTILLED_WATER)
                                    .source()
                                    .get();
            step =
                    SteamBoiler.step(
                            state,
                            settings,
                            tick,
                            drawn,
                            water(input) ? waterTank.getAmountAsInt(0) : 0,
                            distilled,
                            ambient);
            var result =
                    switch (step.output()) {
                        case NONE -> FluidResource.EMPTY;
                        case WATER, DISTILLED_WATER -> input;
                        case STEAM -> fluid(FluidDefinition.STEAM);
                        case SUPERHEATED_STEAM -> fluid(FluidDefinition.SUPERHEATED_STEAM);
                    };
            if (step.outputAmount() > 0)
                delivered = distribute(level, result, step.outputAmount(), transaction);
            int consumed = step.water();
            if (step.output().liquidWater()) consumed = delivered;
            else if (step.outputAmount() > delivered) {
                int rejected = step.outputAmount() - delivered;
                // Stable for this position and tick, including save/reload within that tick.
                pressureBurst =
                        RandomSource.create(level.getSeed() ^ worldPosition.asLong() ^ tick)
                                        .nextInt(10)
                                == 0;
                if (!pressureBurst) consumed -= rejected / 100;
            }
            if (consumed > 0 && waterTank.extract(0, input, consumed, transaction) != consumed)
                return;
            state = step.next();
            transaction.commit();
        }
        heatInput = drawn;
        outputAmount = delivered;
        output = delivered == 0 ? SteamBoiler.Output.NONE : step.output();
        setActive(drawn > 0 && !step.overheated());
        if (step.overheated()) HeatExplosion.trigger(level, worldPosition, 10, .01f, true);
        else if (pressureBurst) HeatExplosion.trigger(level, worldPosition, 1, 1, false);
    }

    private int drawHeat(ServerLevel level, Transaction transaction) {
        int heat = 0;
        for (var direction : Direction.values()) {
            var pos = worldPosition.relative(direction);
            if (!level.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) continue;
            var source = level.getCapability(WorkCapabilities.HEAT, pos, direction.getOpposite());
            if (source == null) continue;
            int remaining = SteamBoiler.MAX_HEAT_INPUT - heat;
            int extracted = source.extract(remaining, transaction);
            if (extracted < 0 || extracted > remaining)
                throw new IllegalStateException("Heat source exceeded request");
            heat += extracted;
            if (heat == SteamBoiler.MAX_HEAT_INPUT) break;
        }
        return heat;
    }

    private int distribute(
            ServerLevel level, FluidResource fluid, int amount, Transaction transaction) {
        int delivered = 0;
        for (var direction : Direction.values()) {
            var pos = worldPosition.relative(direction);
            if (!level.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) continue;
            var target =
                    level.getCapability(Capabilities.Fluid.BLOCK, pos, direction.getOpposite());
            if (target != null)
                delivered +=
                        ResourceHandlerUtil.insertStacking(
                                target, fluid, amount - delivered, transaction);
            if (delivered == amount) break;
        }
        return delivered;
    }

    @Override
    public boolean menuAction(int id) {
        if (id < 0 || id > 13) return false;
        settings = settings.configure(id);
        setChanged();
        return true;
    }

    @Override
    public int progress() {
        return state.scale();
    }

    @Override
    public int progressMaximum() {
        return SteamBoiler.MAX_SCALE;
    }

    @Override
    public int fuelRemaining() {
        return heatInput;
    }

    @Override
    public int fuelMaximum() {
        return SteamBoiler.MAX_HEAT_INPUT;
    }

    @Override
    public int menuValue(int index) {
        return switch (index) {
            case 0 -> settings.waterPerTick();
            case 1 -> waterTank.getAmountAsInt(0);
            case 2 -> settings.pressure();
            case 3 -> BuiltInRegistries.FLUID.getId(waterTank.getResource(0).getFluid());
            case 4 -> Float.floatToIntBits((float) state.temperature());
            case 5 -> outputAmount;
            case 6 -> output.ordinal();
            default -> 0;
        };
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        waterTank.deserialize(input.childOrEmpty("waterTank"));
        double temperature = input.getDoubleOr("systemheat", 0);
        state =
                new SteamBoiler.State(
                        Double.isFinite(temperature) ? Math.clamp(temperature, 0, 500) : 0,
                        Math.clamp(input.getIntOr("calcification", 0), 0, SteamBoiler.MAX_SCALE),
                        input.getLongOr("boilerTick", Long.MIN_VALUE));
        settings =
                new SteamBoiler.Settings(
                        Math.clamp(input.getIntOr("inputmb", 0), 0, 1000),
                        Math.clamp(input.getIntOr("pressurevalve", 0), 0, 300));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        waterTank.serialize(output.child("waterTank"));
        output.putDouble("systemheat", state.temperature());
        output.putInt("calcification", state.scale());
        output.putLong("boilerTick", state.tick());
        output.putInt("inputmb", settings.waterPerTick());
        output.putInt("pressurevalve", settings.pressure());
    }
}
