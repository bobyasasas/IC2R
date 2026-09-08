package ic2.neoforge.machine;

import ic2.core.machine.SteamTurbineCycle;
import ic2.core.machine.WorkBuffer;
import ic2.neoforge.api.WorkSource;
import ic2.neoforge.explosion.HeatExplosion;
import ic2.neoforge.fluid.FluidDefinition;
import ic2.neoforge.registration.MaterialDefinition;
import ic2.neoforge.registration.ModFluids;
import ic2.neoforge.registration.ModItems;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.transfer.MachineFluidTank;
import ic2.neoforge.transfer.ResourcePort;
import ic2.neoforge.transfer.UpgradeTransfers;

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
import net.neoforged.neoforge.transfer.CombinedResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import org.jspecify.annotations.Nullable;

/** Steam pays for a finite KU batch. Tick order cannot recreate an already consumed batch. */
public final class SteamTurbineBlockEntity extends MachineBlockEntity implements FluidMachine {
    public static final int VENTING = 1,
            THROTTLED = 2,
            WATER_BLOCKED = 4,
            NO_TURBINE = 8,
            DISABLED = 16;
    private SteamTurbineCycle.State cycle = new SteamTurbineCycle.State(0, Long.MIN_VALUE);
    private final WorkBuffer work = new WorkBuffer(Integer.MAX_VALUE);
    private int rate, flags;

    private record Snapshot(
            SteamTurbineCycle.State cycle, WorkBuffer.State work, int rate, int flags) {}

    private final StateJournal<Snapshot> journal =
            new StateJournal<>(
                    () -> new Snapshot(cycle, work.state(), rate, flags),
                    this::restore,
                    this::setChanged);
    private final MachineFluidTank steamTank =
            new MachineFluidTank(
                    SteamTurbineCycle.STEAM_CAPACITY,
                    this::setChanged,
                    SteamTurbineBlockEntity::steam);
    private final MachineFluidTank waterTank =
            new MachineFluidTank(
                    1000,
                    this::setChanged,
                    resource ->
                            resource.getFluid() == Fluids.WATER
                                    || resource.getFluid()
                                            == ModFluids.FAMILIES
                                                    .get(FluidDefinition.DISTILLED_WATER)
                                                    .source()
                                                    .get());
    private final ResourceHandler<FluidResource> fluids =
            new ResourcePort<>(
                    new CombinedResourceHandler<>(steamTank, waterTank),
                    index -> true,
                    index -> index == 1);

    public SteamTurbineBlockEntity(BlockPos pos, BlockState state) {
        super(
                ModMachines.entityType(MachineKind.STEAM_KINETIC_GENERATOR),
                pos,
                state,
                MachineKind.STEAM_KINETIC_GENERATOR.slots());
    }

    private void restore(Snapshot snapshot) {
        cycle = snapshot.cycle();
        work.restore(snapshot.work());
        rate = snapshot.rate();
        flags = snapshot.flags();
    }

    public MachineFluidTank steamTank() {
        return steamTank;
    }

    public MachineFluidTank waterTank() {
        return waterTank;
    }

    public long condensedSteam() {
        return cycle.condensedSteam();
    }

    public static boolean rotor(ItemResource resource) {
        return resource.getItem() == ModItems.MATERIALS.get(MaterialDefinition.STEAM_TURBINE).get();
    }

    public boolean hasRotor() {
        return rotor(inventory.getResource(0)) && inventory.getAmountAsInt(0) == 1;
    }

    private static FluidResource fluid(FluidDefinition definition) {
        return FluidResource.of(ModFluids.FAMILIES.get(definition).source().get());
    }

    private static boolean steam(FluidResource resource) {
        return resource.getFluid() == ModFluids.FAMILIES.get(FluidDefinition.STEAM).source().get()
                || resource.getFluid()
                        == ModFluids.FAMILIES.get(FluidDefinition.SUPERHEATED_STEAM).source().get();
    }

    @Override
    protected boolean acceptsInventorySlot(int slot, ItemResource resource) {
        return slot == 0 ? rotor(resource) : super.acceptsInventorySlot(slot, resource);
    }

    @Override
    protected int inventorySlotLimit(int slot) {
        return slot == 0 ? 1 : super.inventorySlotLimit(slot);
    }

    @Override
    public ResourceHandler<ItemResource> automation(Direction side) {
        return new ResourcePort<>(
                inventory,
                slot -> slot == 0 && side == Direction.UP,
                slot -> slot == 0 && !rotor(inventory.getResource(slot)));
    }

    @Override
    public ResourceHandler<FluidResource> fluidAutomation(Direction side) {
        return fluids;
    }

    public WorkSource output(@Nullable Direction side) {
        return new WorkOutput(
                this, side, work, () -> hasRotor() ? rate : 0, journal::updateSnapshots);
    }

    @Override
    public void serverTick(ServerLevel level) {
        long tick = level.getGameTime();
        if (cycle.tick() == tick) return;
        boolean rotor = hasRotor();
        double multiplier = GenerationConfig.STEAM_KINETIC.get();
        var input = steamTank.getResource(0);
        boolean hot =
                input.getFluid()
                        == ModFluids.FAMILIES.get(FluidDefinition.SUPERHEATED_STEAM).source().get();
        var distilled = fluid(FluidDefinition.DISTILLED_WATER);
        int water = waterTank.getAmountAsInt(0);
        boolean acceptsWater =
                water < 1000 && (water == 0 || waterTank.getResource(0).equals(distilled));
        boolean waterBlocked =
                water == 1000
                        || !acceptsWater
                                && cycle.condensedSteam()
                                                + (hot ? 0 : steamTank.getAmountAsInt(0) / 10)
                                        >= 100;
        var step =
                SteamTurbineCycle.plan(
                        cycle,
                        tick,
                        rotor && steam(input) ? steamTank.getAmountAsInt(0) : 0,
                        hot,
                        water,
                        acceptsWater,
                        multiplier);
        int delivered;
        try (var transaction = Transaction.openRoot()) {
            journal.updateSnapshots(transaction);
            if (step.steam() > 0
                    && steamTank.extract(0, input, step.steam(), transaction) != step.steam())
                return;
            if (step.water() > 0
                    && waterTank.insert(0, distilled, step.water(), transaction) != step.water())
                return;
            delivered = exhaust(level, step.exhaust(), hot, transaction);
            cycle = step.next();
            rate = step.kinetic();
            work.publish(rate);
            flags =
                    (delivered < step.exhaust() ? VENTING : 0)
                            | (water > 0 ? THROTTLED : 0)
                            | (waterBlocked ? WATER_BLOCKED : 0)
                            | (!rotor ? NO_TURBINE : 0)
                            | (multiplier == 0 ? DISABLED : 0);
            transaction.commit();
        }
        setActive(step.steam() > 0);
        UpgradeTransfers.tick(level, this);
        if (delivered < step.exhaust()
                && RandomSource.create(level.getSeed() ^ worldPosition.asLong() ^ tick).nextInt(10)
                        == 0) HeatExplosion.trigger(level, worldPosition, 1, 1, false);
    }

    private int exhaust(ServerLevel level, int amount, boolean hot, Transaction transaction) {
        if (amount == 0) return 0;
        int delivered = 0;
        var steam = fluid(FluidDefinition.STEAM);
        for (var side : Direction.values()) {
            var pos = worldPosition.relative(side);
            if (!level.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) continue;
            var target = level.getBlockEntity(pos);
            // Ordinary exhaust must end in a condenser; only the hot stage can feed a second
            // turbine.
            if (!(target instanceof CondenserBlockEntity)
                    && !(hot && target instanceof SteamTurbineBlockEntity)) continue;
            var handler = level.getCapability(Capabilities.Fluid.BLOCK, pos, side.getOpposite());
            if (handler != null)
                delivered +=
                        ResourceHandlerUtil.insertStacking(
                                handler, steam, amount - delivered, transaction);
            if (delivered == amount) break;
        }
        return delivered;
    }

    @Override
    public int progress() {
        return (int) work.state().stored();
    }

    @Override
    public int progressMaximum() {
        return rate;
    }

    @Override
    public int menuValue(int index) {
        return switch (index) {
            case 0 -> rate;
            case 1 -> steamTank.getAmountAsInt(0);
            case 2 -> waterTank.getAmountAsInt(0);
            case 3 -> BuiltInRegistries.FLUID.getId(steamTank.getResource(0).getFluid());
            case 4 -> BuiltInRegistries.FLUID.getId(waterTank.getResource(0).getFluid());
            case 5 -> flags;
            case 6 -> Float.floatToIntBits((float) (cycle.condensedSteam() / 100.0));
            default -> 0;
        };
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        steamTank.deserialize(input.childOrEmpty("steamTank"));
        waterTank.deserialize(input.childOrEmpty("distilledWaterTank"));
        cycle =
                new SteamTurbineCycle.State(
                        Math.clamp(
                                input.getLongOr("condensationprogress", 0),
                                0,
                                SteamTurbineCycle.MAX_CREDIT),
                        input.getLongOr("turbineTick", Long.MIN_VALUE));
        rate = Math.max(0, input.getIntOr("kineticRate", 0));
        work.restore(
                new WorkBuffer.State(
                        Math.clamp(input.getIntOr("kinetic", 0), 0, rate),
                        input.getLongOr("outputTick", Long.MIN_VALUE),
                        Math.max(0, input.getIntOr("outputExtracted", 0))));
        flags = 0;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        steamTank.serialize(output.child("steamTank"));
        waterTank.serialize(output.child("distilledWaterTank"));
        output.putLong("condensationprogress", cycle.condensedSteam());
        output.putLong("turbineTick", cycle.tick());
        output.putInt("kineticRate", rate);
        output.putInt("kinetic", (int) work.state().stored());
        output.putLong("outputTick", work.state().tick());
        output.putInt("outputExtracted", work.state().extracted());
    }
}
