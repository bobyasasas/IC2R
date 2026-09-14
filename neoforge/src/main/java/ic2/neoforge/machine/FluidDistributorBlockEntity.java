package ic2.neoforge.machine;

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

import java.util.ArrayList;

/**
 * The active flag is a persistent mode, not a working indicator: active machines push the whole
 * tank out of the front and take fluid everywhere else, idle ones take fluid only from the front
 * and balance the tank across every other side.
 */
public class FluidDistributorBlockEntity extends MachineBlockEntity implements FluidMachine {
    public static final int INPUT = 0, OUTPUT = 1;
    public static final int CAPACITY = 1000;

    protected final MachineFluidTank tank =
            new MachineFluidTank(CAPACITY, this::setChanged, fluid -> true);

    public FluidDistributorBlockEntity(BlockPos pos, BlockState state) {
        super(
                ModMachines.entityType(((MachineBlock) state.getBlock()).kind()),
                pos,
                state,
                ((MachineBlock) state.getBlock()).kind().slots());
    }

    public MachineFluidTank tank() {
        return tank;
    }

    protected Direction facing() {
        return getBlockState().getValue(MachineBlock.FACING);
    }

    /** Inverse sides feed the tank while active; the front alone feeds it while idle. */
    protected boolean acceptsFrom(Direction side) {
        return getBlockState().getValue(MachineBlock.ACTIVE) != side.equals(facing());
    }

    @Override
    public ResourceHandler<FluidResource> fluidAutomation(Direction side) {
        return new ResourcePort<>(tank, slot -> acceptsFrom(side), slot -> false);
    }

    @Override
    public ResourceHandler<ItemResource> automation(Direction side) {
        return new ResourcePort<>(
                inventory,
                slot -> slot == INPUT,
                slot -> slot == OUTPUT,
                (slot, resource) -> FluidContainerPort.accepts(resource));
    }

    @Override
    public void serverTick(ServerLevel level) {
        // Filled containers leave through the output slot, empties line up in the input.
        ResourceHandlerUtil.move(
                tank, FluidContainerPort.of(inventory, INPUT, OUTPUT), fluid -> true, CAPACITY, null);
        if (tank.getAmountAsInt(0) > 0) moveFluid(level);
    }

    protected void moveFluid(ServerLevel level) {
        if (getBlockState().getValue(MachineBlock.ACTIVE)) {
            push(level, facing(), tank.getAmountAsInt(0));
        } else {
            balance(level);
        }
    }

    /** Offers up to the accepted volume per round; neighbours that choke fall out of the split. */
    private void balance(ServerLevel level) {
        record Target(ResourceHandler<FluidResource> handler, int accepted) {}
        var front = facing();
        var targets = new ArrayList<Target>();
        int acceptedTotal = 0;
        for (var side : Direction.values()) {
            if (side == front) continue;
            var handler = handlerAt(level, side);
            if (handler == null) continue;
            int accepted;
            try (var transaction = Transaction.openRoot()) {
                accepted =
                        ResourceHandlerUtil.move(
                                tank,
                                handler,
                                fluid -> true,
                                tank.getAmountAsInt(0),
                                transaction);
            }
            if (accepted > 0) {
                targets.add(new Target(handler, accepted));
                acceptedTotal += accepted;
            }
        }
        while (!targets.isEmpty() && tank.getAmountAsInt(0) > 0 && acceptedTotal > 0) {
            int share = Math.min(acceptedTotal, tank.getAmountAsInt(0)) / targets.size();
            if (share == 0) {
                for (var target : targets)
                    acceptedTotal -= pushInto(target.handler(), Math.min(acceptedTotal, CAPACITY));
                return;
            }
            var iterator = targets.iterator();
            while (iterator.hasNext()) {
                int moved = pushInto(iterator.next().handler(), share);
                acceptedTotal -= moved;
                if (moved < share) iterator.remove();
            }
        }
    }

    protected final boolean push(ServerLevel level, Direction side, int amount) {
        var handler = handlerAt(level, side);
        return handler != null && pushInto(handler, amount) > 0;
    }

    private int pushInto(ResourceHandler<FluidResource> target, int amount) {
        try (var transaction = Transaction.openRoot()) {
            int moved = ResourceHandlerUtil.move(tank, target, fluid -> true, amount, transaction);
            if (moved > 0) transaction.commit();
            return moved;
        }
    }

    private ResourceHandler<FluidResource> handlerAt(ServerLevel level, Direction side) {
        var target = worldPosition.relative(side);
        if (!level.getChunkSource().hasChunk(target.getX() >> 4, target.getZ() >> 4)) return null;
        return level.getCapability(Capabilities.Fluid.BLOCK, target, side.getOpposite());
    }

    /** Action 0 flips the distribution mode; the tank readout mirrors the regulator screen. */
    @Override
    public boolean menuAction(int id) {
        if (id != 0) return false;
        setActive(!getBlockState().getValue(MachineBlock.ACTIVE));
        setChanged();
        return true;
    }

    @Override
    public int menuValue(int index) {
        return switch (index) {
            case 0 -> getBlockState().getValue(MachineBlock.ACTIVE) ? 1 : 0;
            case 1 -> tank.getAmountAsInt(0);
            case 2 -> BuiltInRegistries.FLUID.getId(tank.getResource(0).getFluid());
            default -> 0;
        };
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
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        tank.deserialize(input.childOrEmpty("tank"));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        tank.serialize(output.child("tank"));
    }
}
