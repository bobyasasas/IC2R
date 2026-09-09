package ic2.neoforge.machine;

import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.transfer.FluidContainerPort;
import ic2.neoforge.transfer.MachineFluidTank;
import ic2.neoforge.transfer.ResourcePort;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.ArrayDeque;
import java.util.HashSet;

/**
 * Draws liquids from the faced fluid block or, failing that, from a source block found around and
 * below it, filling an 8,000 mB tank that fills carried buckets. One operation (twenty ticks) pumps
 * up to one bucket and costs one EU per tick.
 */
public final class PumpBlockEntity extends PoweredBlockEntity implements FluidMachine {
    public static final int TANK = 8000;
    private static final int SEARCH_LIMIT = 32;
    private final MachineFluidTank tank =
            new MachineFluidTank(TANK, this::setChanged, resource -> !resource.isEmpty());
    private final ResourceHandler<FluidResource> fluids =
            new ResourcePort<>(tank, slot -> false, slot -> true);
    private int progress;
    private BlockPos requestedDrain;

    public PumpBlockEntity(BlockPos pos, BlockState state) {
        super(ModMachines.entityType(MachineKind.PUMP), pos, state, MachineKind.PUMP.ticks(), 6);
    }

    @Override
    public ic2.core.energy.grid.EnergyNode.Terminal energyNode() {
        return ic2.core.energy.grid.EnergyNode.Terminal.sink(energy, 32, 1);
    }

    public MachineFluidTank tank() {
        return tank;
    }

    @Override
    public ResourceHandler<FluidResource> fluidAutomation(Direction side) {
        return fluids;
    }

    @Override
    public ResourceHandler<ItemResource> automation(Direction side) {
        return new ResourcePort<>(
                inventory,
                slot -> slot == 0 && side != Direction.DOWN,
                slot -> slot == 1,
                (slot, resource) -> FluidContainerPort.accepts(resource));
    }

    @Override
    public void serverTick(ServerLevel level) {
        // Buckets in the container slot are filled straight from the tank.
        ResourceHandlerUtil.move(
                tank, FluidContainerPort.of(inventory, 0, 1), resource -> true, 1000, null);
        var faced = worldPosition.relative(getBlockState().getValue(MachineBlock.FACING));
        if (progress < kind().ticks()) {
            if (energy.stored() >= 1) {
                energy.extract(1);
                progress++;
                setActive(true);
            } else {
                setActive(false);
            }
            return;
        }
        progress = 0;
        boolean pumped = pump(level, faced);
        setActive(pumped);
    }

    /**
     * Mining linkage: answers whether an attached miner may treat a liquid as pumpable, i.e. the
     * tank can take another bucket and the faced search still finds a source.
     */
    public boolean canDrain(ServerLevel level) {
        if (TANK - tank.getAmountAsInt(0) < 1000) return false;
        var faced = worldPosition.relative(getBlockState().getValue(MachineBlock.FACING));
        return searchSource(level, faced) != null;
    }

    /** The miner hands over one marked liquid position that the pump drains before searching. */
    public void requestDrain(BlockPos pos) {
        requestedDrain = pos.immutable();
    }

    private boolean pump(ServerLevel level, BlockPos faced) {
        int space = TANK - tank.getAmountAsInt(0);
        if (space <= 0) return false;
        if (requestedDrain != null) {
            var target = requestedDrain;
            requestedDrain = null;
            var state = level.getFluidState(target);
            if (!state.isEmpty()) return drainBlock(level, target, state, space);
        }
        var facedState = level.getFluidState(faced);
        if (facedState.isSource()) {
            return drainBlock(level, faced, facedState, space);
        }
        BlockPos found = searchSource(level, faced);
        if (found == null) return false;
        return drainBlock(level, found, level.getFluidState(found), space);
    }

    private boolean drainBlock(ServerLevel level, BlockPos pos, FluidState state, int space) {
        var resource = FluidResource.of(state.getType());
        int amount = Math.min(space, 1000);
        try (var transaction = Transaction.openRoot()) {
            if (tank.insert(0, resource, amount, transaction) != amount) return false;
            transaction.commit();
        }
        level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        setChanged();
        return true;
    }

    /** Breadth-first source search: faced position, then down and sideways, at most 32 nodes. */
    private static BlockPos searchSource(ServerLevel level, BlockPos start) {
        var queue = new ArrayDeque<BlockPos>();
        var visited = new HashSet<BlockPos>();
        queue.add(start);
        visited.add(start);
        while (!queue.isEmpty() && visited.size() < SEARCH_LIMIT) {
            var pos = queue.poll();
            if (level.getFluidState(pos).isSource()) return pos;
            for (var direction :
                    new Direction[] {
                        Direction.DOWN,
                        Direction.NORTH,
                        Direction.SOUTH,
                        Direction.WEST,
                        Direction.EAST
                    }) {
                var next = pos.relative(direction);
                if (visited.contains(next)) continue;
                visited.add(next);
                var fluid = level.getFluidState(next);
                if (!fluid.isEmpty()) queue.add(next);
            }
        }
        return null;
    }

    @Override
    public int progress() {
        return progress;
    }

    @Override
    public int progressMaximum() {
        return kind().ticks();
    }

    @Override
    public int fuelRemaining() {
        return tank.getAmountAsInt(0);
    }

    @Override
    public int fuelMaximum() {
        return TANK;
    }

    @Override
    public int menuValue(int index) {
        return switch (index) {
            case 0 -> tank.getAmountAsInt(0);
            case 1 ->
                    net.minecraft.core.registries.BuiltInRegistries.FLUID.getId(
                            tank.getResource(0).getFluid());
            default -> 0;
        };
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        tank.deserialize(input.childOrEmpty("tank"));
        progress = Math.clamp(input.getIntOr("progress", 0), 0, kind().ticks());
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        tank.serialize(output.child("tank"));
        output.putInt("progress", progress);
    }
}
