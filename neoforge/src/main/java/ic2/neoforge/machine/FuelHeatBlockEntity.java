package ic2.neoforge.machine;

import ic2.core.machine.HeatFuelCycle;
import ic2.core.machine.WorkBuffer;
import ic2.neoforge.api.WorkSource;
import ic2.neoforge.fluid.FluidDefinition;
import ic2.neoforge.registration.MaterialDefinition;
import ic2.neoforge.registration.ModFluids;
import ic2.neoforge.registration.ModItems;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.transfer.MachineFluidTank;
import ic2.neoforge.transfer.ResourcePort;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/** Fuel input and heat reserve share one journal; native inventory/tank enlist alongside it. */
public final class FuelHeatBlockEntity extends MachineBlockEntity implements FluidMachine {
    private record State(HeatFuelCycle.State fuel, WorkBuffer.State work, boolean ashes) {}

    private final HeatFuelCycle fuel = new HeatFuelCycle();
    private final WorkBuffer work = new WorkBuffer(Integer.MAX_VALUE);
    private boolean ashes;
    private final StateJournal<State> journal =
            new StateJournal<>(
                    () -> new State(fuel.state(), work.state(), ashes),
                    state -> {
                        fuel.restore(state.fuel());
                        work.restore(state.work());
                        ashes = state.ashes();
                    },
                    this::setChanged);
    private final @org.jspecify.annotations.Nullable MachineFluidTank tank;

    public FuelHeatBlockEntity(BlockPos pos, BlockState state) {
        super(ModMachines.entityType(((MachineBlock) state.getBlock()).kind()), pos, state, 2);
        tank =
                fluid()
                        ? new MachineFluidTank(
                                10000, this::setChanged, FuelHeatBlockEntity::isBiogas)
                        : null;
    }

    private boolean fluid() {
        return kind() == MachineKind.FLUID_HEAT_GENERATOR;
    }

    private static boolean isBiogas(FluidResource resource) {
        return resource.getFluid() == ModFluids.FAMILIES.get(FluidDefinition.BIOGAS).source().get();
    }

    public MachineFluidTank tank() {
        return java.util.Objects.requireNonNull(tank, "Solid heaters have no fluid tank");
    }

    private int configuredEmission() {
        return (int)
                Math.round(
                        fluid()
                                ? 32 * GenerationConfig.FLUID_HEAT.get()
                                : 20 * GenerationConfig.SOLID_HEAT.get());
    }

    private int bandwidth() {
        return !fuel.idle() || work.state().stored() > 0
                ? fuel.state().emission()
                : configuredEmission();
    }

    public double storedHeat() {
        return fuel.state().reserve() + work.state().stored();
    }

    public WorkSource output(@org.jspecify.annotations.Nullable Direction side) {
        return new WorkOutput(this, side, work, this::bandwidth, journal::updateSnapshots);
    }

    public static int solidBurnTime(ItemStack stack, net.minecraft.world.level.Level level) {
        return stack.is(Items.LAVA_BUCKET)
                ? 0
                : stack.getBurnTime(RecipeType.SMELTING, level.fuelValues()) / 4;
    }

    @Override
    public ResourceHandler<ItemResource> automation(Direction side) {
        return new ResourcePort<>(
                inventory,
                slot -> slot == 0 && side != Direction.DOWN,
                slot -> slot == 1 || slot == 0 && solidRemainder(),
                (slot, resource) ->
                        fluid()
                                ? ItemAccess.forStack(resource.toStack())
                                                .getCapability(Capabilities.Fluid.ITEM)
                                        != null
                                : level != null && solidBurnTime(resource.toStack(), level) > 0);
    }

    private boolean solidRemainder() {
        return !fluid() && level != null && solidBurnTime(inventory.stack(0), level) == 0;
    }

    @Override
    public ResourceHandler<FluidResource> fluidAutomation(Direction side) {
        return new ResourcePort<>(tank(), slot -> true, slot -> false);
    }

    @Override
    public void serverTick(ServerLevel level) {
        if (fluid() && !inventory.stack(0).isEmpty()) {
            var port = new ResourcePort<>(inventory, slot -> slot == 1, slot -> slot == 0);
            var container =
                    ItemAccess.forHandlerIndex(port, 0)
                            .oneByOne()
                            .getCapability(Capabilities.Fluid.ITEM);
            ResourceHandlerUtil.move(container, tank, FuelHeatBlockEntity::isBiogas, 10000, null);
        }
        boolean active;
        try (var transaction = Transaction.openRoot()) {
            journal.updateSnapshots(transaction);
            var ash = ItemResource.of(ModItems.MATERIALS.get(MaterialDefinition.ASHES).get());
            if (ashes && inventory.insert(1, ash, 1, transaction) == 1) ashes = false;
            if (fluid()) {
                int emission = configuredEmission();
                if (fuel.idle()
                        && work.state().stored() < emission
                        && emission > 0
                        && isBiogas(tank.getResource(0))
                        && tank.getAmountAsInt(0) >= 10
                        && tank.extract(0, tank.getResource(0), 10, transaction) == 10)
                    fuel.acceptFluid(20, emission);
                active = fuel.transferTo(work, bandwidth()) > 0;
            } else {
                // Preserve solid fuel's burn-while-full behavior and its one-tick output delay.
                fuel.transferTo(work, bandwidth());
                int emission = configuredEmission();
                var input = inventory.stack(0);
                int ticks = solidBurnTime(input, level);
                boolean ashFits =
                        inventory.stack(1).isEmpty()
                                || inventory.getResource(1).equals(ash)
                                        && inventory.getAmountAsInt(1) < 64;
                if (fuel.idle()
                        && work.state().stored() == 0
                        && !ashes
                        && ashFits
                        && ticks > 0
                        && emission > 0) {
                    if (inventory.extract(0, ItemResource.of(input), 1, transaction) != 1) return;
                    var remainder = input.getItem().getCraftingRemainder(input);
                    if (remainder != null
                            && inventory.insert(
                                            0,
                                            ItemResource.of(remainder),
                                            remainder.count(),
                                            transaction)
                                    != remainder.count()) return;
                    fuel.acceptSolid(ticks, emission);
                }
                active = fuel.state().remaining() > 0;
                if (fuel.burn()) {
                    ashes = level.getRandom().nextBoolean();
                    if (ashes && inventory.insert(1, ash, 1, transaction) == 1) ashes = false;
                }
            }
            transaction.commit();
        }
        setActive(active);
    }

    @Override
    public int progress() {
        return fluid()
                ? (int) Math.min(20, Math.ceil(storedHeat() / Math.max(1, bandwidth())))
                : fuel.state().remaining();
    }

    @Override
    public int progressMaximum() {
        return fluid() ? 20 : fuel.state().total();
    }

    @Override
    public int fuelRemaining() {
        return progress();
    }

    @Override
    public int fuelMaximum() {
        return progressMaximum();
    }

    @Override
    public int menuValue(int index) {
        return switch (index) {
            // GUI compaction needs only float precision; saved reserve remains an exact long.
            case 0 -> Float.floatToIntBits((float) storedHeat());
            case 1 -> bandwidth();
            case 2 -> fluid() ? tank.getAmountAsInt(0) : 0;
            case 3 -> fluid() ? BuiltInRegistries.FLUID.getId(tank.getResource(0).getFluid()) : 0;
            case 4 -> fluid() ? 10000 : 0;
            default -> 0;
        };
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        int total = Math.max(0, input.getIntOr("fuelTotal", 0));
        int emission = Math.clamp(input.getIntOr("heatEmission", 0), 0, 32000000);
        fuel.restore(
                new HeatFuelCycle.State(
                        emission > 0 ? Math.clamp(input.getIntOr("fuel", 0), 0, total) : 0,
                        total,
                        emission,
                        Math.clamp(
                                input.getLongOr("heatReserve", 0),
                                0,
                                (long) Integer.MAX_VALUE * 32000000)));
        double stored = input.getDoubleOr("heat", 0);
        work.restore(
                new WorkBuffer.State(
                        Double.isFinite(stored) ? Math.clamp(stored, 0, 32000000) : 0,
                        input.getLongOr("heatTick", Long.MIN_VALUE),
                        Math.clamp(input.getIntOr("heatExtracted", 0), 0, 32000000)));
        ashes = input.getBooleanOr("ashesPending", false);
        if (fluid()) tank.deserialize(input.childOrEmpty("fluidTank"));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("fuel", fuel.state().remaining());
        output.putInt("fuelTotal", fuel.state().total());
        output.putInt("heatEmission", fuel.state().emission());
        output.putLong("heatReserve", fuel.state().reserve());
        output.putDouble("heat", work.state().stored());
        output.putLong("heatTick", work.state().tick());
        output.putInt("heatExtracted", work.state().extracted());
        output.putBoolean("ashesPending", ashes);
        if (fluid()) tank.serialize(output.child("fluidTank"));
    }
}
