package ic2.neoforge.machine;

import ic2.core.machine.SolarGeneration;
import ic2.neoforge.fluid.FluidDefinition;
import ic2.neoforge.registration.ModFluids;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.transfer.FluidContainerPort;
import ic2.neoforge.transfer.MachineFluidTank;
import ic2.neoforge.transfer.ResourcePort;
import ic2.neoforge.transfer.UpgradeTransfers;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.CombinedResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * Passive solar still: no energy, one water millibucket becomes distilled water every 72 ticks
 * while brightness above half shines on the block. The legacy HOT=36/COLD=144 biome rates are
 * unreachable through the EnvProxy stub, so the ordinary rate is a constant here too.
 */
public final class SolarDistillerBlockEntity extends MachineBlockEntity implements FluidMachine {
    public static final int WATER_INPUT = 0,
            WATER_OUTPUT = 1,
            DISTILLED_INPUT = 2,
            DISTILLED_OUTPUT = 3;
    public static final int TICK_RATE = 72;
    public static final int CAPACITY_MB = 10000;

    private final MachineFluidTank inputTank =
            new MachineFluidTank(CAPACITY_MB, this::setChanged, SolarDistillerBlockEntity::water);
    private final MachineFluidTank outputTank =
            new MachineFluidTank(CAPACITY_MB, this::setChanged, fluid -> true);
    private final ResourceHandler<FluidResource> fluids =
            new ResourcePort<>(
                    new CombinedResourceHandler<>(inputTank, outputTank),
                    index -> index == 0,
                    index -> index == 1);
    private int updateTicker;
    private double skyLight;

    public SolarDistillerBlockEntity(BlockPos pos, BlockState state) {
        super(
                ModMachines.entityType(MachineKind.SOLAR_DISTILLER),
                pos,
                state,
                MachineKind.SOLAR_DISTILLER.slots());
        // Legacy seeds its ticker with random.nextInt(tickRate) on load; position hashing
        // reproduces the same stagger deterministically.
        updateTicker = Math.floorMod(pos.hashCode(), TICK_RATE);
    }

    public MachineFluidTank inputTank() {
        return inputTank;
    }

    public MachineFluidTank outputTank() {
        return outputTank;
    }

    public double skyLight() {
        return skyLight;
    }

    private static boolean water(FluidResource resource) {
        return resource.getFluid() == Fluids.WATER;
    }

    private static FluidResource distilledWater() {
        return FluidResource.of(
                ModFluids.FAMILIES.get(FluidDefinition.DISTILLED_WATER).source().get());
    }

    @Override
    protected boolean acceptsInventorySlot(int slot, ItemResource resource) {
        if (slot == WATER_INPUT || slot == DISTILLED_INPUT)
            return FluidContainerPort.accepts(resource);
        return super.acceptsInventorySlot(slot, resource);
    }

    @Override
    public ResourceHandler<ItemResource> automation(Direction side) {
        return new ResourcePort<>(
                inventory,
                slot ->
                        slot == WATER_INPUT && side == Direction.UP
                                || slot == DISTILLED_INPUT && side == Direction.DOWN,
                slot -> slot == WATER_OUTPUT || slot == DISTILLED_OUTPUT,
                (slot, resource) -> FluidContainerPort.accepts(resource));
    }

    @Override
    public ResourceHandler<FluidResource> fluidAutomation(Direction side) {
        return fluids;
    }

    @Override
    public void serverTick(ServerLevel level) {
        if (!inventory.stack(WATER_INPUT).isEmpty())
            ResourceHandlerUtil.move(
                    FluidContainerPort.of(inventory, WATER_INPUT, WATER_OUTPUT),
                    inputTank,
                    SolarDistillerBlockEntity::water,
                    CAPACITY_MB,
                    null);
        if (!inventory.stack(DISTILLED_INPUT).isEmpty())
            ResourceHandlerUtil.move(
                    outputTank,
                    FluidContainerPort.of(inventory, DISTILLED_INPUT, DISTILLED_OUTPUT),
                    fluid -> true,
                    CAPACITY_MB,
                    null);
        if (++updateTicker >= TICK_RATE) {
            updateTicker = 0;
            updateSunVisibility(level);
            setActive(canWork() && distill());
        }
        UpgradeTransfers.tick(level, this);
    }

    /** Legacy TileEntitySolarGenerator.getSkyLight sampled one block above the still. */
    public void updateSunVisibility(ServerLevel level) {
        var sample = worldPosition.above();
        skyLight =
                SolarGeneration.brightness(
                        level.dimensionType().hasSkyLight(),
                        level.getBrightness(LightLayer.SKY, sample),
                        level.environmentAttributes()
                                .getValue(EnvironmentAttributes.SUN_ANGLE, sample),
                        false,
                        level.getRainLevel(1),
                        level.getThunderLevel(1));
    }

    public boolean canWork() {
        return inputTank.getAmountAsInt(0) > 0
                && outputTank.getAmountAsInt(0) < CAPACITY_MB
                && skyLight > 0.5;
    }

    private boolean distill() {
        try (var transaction = Transaction.openRoot()) {
            if (inputTank.extract(0, inputTank.getResource(0), 1, transaction) != 1) return false;
            if (outputTank.insert(0, distilledWater(), 1, transaction) != 1) return false;
            transaction.commit();
        }
        return true;
    }

    @Override
    public int progress() {
        return (int) Math.round(skyLight * 1000);
    }

    @Override
    public int progressMaximum() {
        return 1000;
    }

    @Override
    public int menuValue(int index) {
        return switch (index) {
            case 1 -> inputTank.getAmountAsInt(0);
            case 2 -> outputTank.getAmountAsInt(0);
            case 3 -> BuiltInRegistries.FLUID.getId(inputTank.getResource(0).getFluid());
            case 4 -> BuiltInRegistries.FLUID.getId(outputTank.getResource(0).getFluid());
            default -> 0;
        };
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        inputTank.deserialize(input.childOrEmpty("inputTank"));
        outputTank.deserialize(input.childOrEmpty("outputTank"));
        skyLight = input.getDoubleOr("skyLight", 0.0);
        updateTicker = Math.floorMod(input.getIntOr("updateTicker", 0), TICK_RATE);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        inputTank.serialize(output.child("inputTank"));
        outputTank.serialize(output.child("outputTank"));
        output.putDouble("skyLight", skyLight);
        output.putInt("updateTicker", updateTicker);
    }
}
