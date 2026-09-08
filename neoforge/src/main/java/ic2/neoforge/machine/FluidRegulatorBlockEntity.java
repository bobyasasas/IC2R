package ic2.neoforge.machine;

import ic2.core.energy.grid.EnergyNode;
import ic2.core.machine.FlowRegulator;
import ic2.neoforge.item.ElectricItemEnergy;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.transfer.FluidContainerPort;
import ic2.neoforge.transfer.MachineFluidTank;
import ic2.neoforge.transfer.ResourcePort;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/** A delivery consumes only the fluid accepted by the target and a flat ten EU. */
public final class FluidRegulatorBlockEntity extends PoweredBlockEntity implements FluidMachine {
    private final FlowRegulator regulator = new FlowRegulator();
    private final MachineJournal<FlowRegulator.State> journal =
            new MachineJournal<>(energy, regulator::state, regulator::restore, this::setChanged);
    private final MachineFluidTank tank =
            new MachineFluidTank(10000, this::setChanged, fluid -> true);
    private int lastMoved;

    public FluidRegulatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModMachines.entityType(MachineKind.FLUID_REGULATOR), pos, state, 10000, 3);
    }

    public MachineFluidTank tank() {
        return tank;
    }

    @Override
    public EnergyNode.Terminal energyNode() {
        return EnergyNode.Terminal.sink(energy, 2048, 1);
    }

    @Override
    public ResourceHandler<FluidResource> fluidAutomation(Direction side) {
        return new ResourcePort<>(
                tank, slot -> side != getBlockState().getValue(MachineBlock.FACING), slot -> false);
    }

    @Override
    public ResourceHandler<ItemResource> automation(Direction side) {
        return new ResourcePort<>(
                inventory,
                slot -> slot == 0 && side == Direction.UP,
                slot -> slot == 1,
                (slot, item) -> FluidContainerPort.accepts(item));
    }

    @Override
    public void serverTick(ServerLevel level) {
        var battery = inventory.stack(2);
        double charge = ElectricItemEnergy.discharge(battery, energy.free(), 4, false, true, false);
        if (charge > 0) {
            inventory.set(2, ItemResource.of(battery), battery.getCount());
            energy.insert(charge);
            setChanged();
        }
        if (!inventory.stack(0).isEmpty())
            ResourceHandlerUtil.move(
                    FluidContainerPort.of(inventory, 0, 1), tank, fluid -> true, 10000, null);
        long tick = level.getGameTime();
        // Every-tick mode updates the indicator every tick; per-second mode retains it until the
        // next scheduled delivery.
        boolean scheduled =
                regulator.state().perTick()
                        || Math.floorMod(tick, 20) == Math.floorMod(worldPosition.hashCode(), 20);
        if (!scheduled) return;
        if (!regulator.ready(tick, worldPosition.hashCode())) {
            if (regulator.state().deliveredTick() != tick) setActive(false);
            return;
        }
        lastMoved = 0;
        if (energy.stored() >= 10 && !tank.getResource(0).isEmpty()) {
            var facing = getBlockState().getValue(MachineBlock.FACING);
            var target = worldPosition.relative(facing);
            if (level.getChunkSource().hasChunk(target.getX() >> 4, target.getZ() >> 4))
                try (var transaction = Transaction.openRoot()) {
                    journal.updateSnapshots(transaction);
                    int moved =
                            ResourceHandlerUtil.move(
                                    tank,
                                    level.getCapability(
                                            Capabilities.Fluid.BLOCK, target, facing.getOpposite()),
                                    fluid -> true,
                                    regulator.state().amount(),
                                    transaction);
                    if (moved > 0 && energy.consume(10)) {
                        regulator.delivered(tick);
                        transaction.commit();
                        lastMoved = moved;
                    }
                }
        }
        setActive(lastMoved > 0);
    }

    @Override
    public boolean menuAction(int id) {
        if (!regulator.configure(id)) return false;
        setChanged();
        return true;
    }

    @Override
    public int progress() {
        return 0;
    }

    @Override
    public int progressMaximum() {
        return 0;
    }

    @Override
    public int menuValue(int index) {
        return switch (index) {
            case 0 -> regulator.state().amount();
            case 1 -> tank.getAmountAsInt(0);
            case 2 -> regulator.state().perTick() ? 1 : 0;
            case 3 -> BuiltInRegistries.FLUID.getId(tank.getResource(0).getFluid());
            case 4 -> lastMoved;
            default -> 0;
        };
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        tank.deserialize(input.childOrEmpty("tank"));
        regulator.restore(
                new FlowRegulator.State(
                        Math.clamp(input.getIntOr("amount", 0), 0, 1000),
                        input.getBooleanOr("perTick", false),
                        input.getLongOr("deliveredTick", Long.MIN_VALUE)));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        tank.serialize(output.child("tank"));
        output.putInt("amount", regulator.state().amount());
        output.putBoolean("perTick", regulator.state().perTick());
        output.putLong("deliveredTick", regulator.state().deliveredTick());
    }
}
