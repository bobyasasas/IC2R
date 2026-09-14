package ic2.neoforge.machine;

import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.transfer.ResourcePort;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.ArrayList;
import java.util.List;

/**
 * An import-only nine slot buffer that pours into the neighbours named by the priority list, in
 * order, until one of them empties it.
 */
public final class WeightedItemDistributorBlockEntity extends MachineBlockEntity {
    public static final int BUFFER_END = 9;
    public static final int MAX_PRIORITY = 5;

    private final List<Direction> priority = new ArrayList<>();

    public WeightedItemDistributorBlockEntity(BlockPos pos, BlockState state) {
        super(
                ModMachines.entityType(((MachineBlock) state.getBlock()).kind()),
                pos,
                state,
                ((MachineBlock) state.getBlock()).kind().slots());
    }

    public List<Direction> priority() {
        return priority;
    }

    private Direction facing() {
        return getBlockState().getValue(MachineBlock.FACING);
    }

    @Override
    public ResourceHandler<ItemResource> automation(Direction side) {
        return new ResourcePort<>(inventory, slot -> true, slot -> false);
    }

    @Override
    public void serverTick(ServerLevel level) {
        if (priority.isEmpty() || bufferEmpty()) return;
        for (var side : priority) {
            if (side.equals(facing())) continue;
            var target = worldPosition.relative(side);
            if (!level.getChunkSource().hasChunk(target.getX() >> 4, target.getZ() >> 4)) continue;
            var adjacent =
                    level.getCapability(Capabilities.Item.BLOCK, target, side.getOpposite());
            if (adjacent == null) continue;
            try (var transaction = Transaction.openRoot()) {
                ResourceHandlerUtil.moveStacking(
                        inventory, adjacent, resource -> true, Integer.MAX_VALUE, transaction);
                transaction.commit();
            }
            if (bufferEmpty()) break;
        }
    }

    private boolean bufferEmpty() {
        for (int slot = 0; slot < BUFFER_END; slot++)
            if (!inventory.stack(slot).isEmpty()) return false;
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

    /** Values 0..4 mirror the priority list as 3D data, -1 padding beyond the list end. */
    @Override
    public int menuValue(int index) {
        if (index < 0 || index >= MAX_PRIORITY) return 0;
        return index < priority.size() ? priority.get(index).get3DDataValue() : -1;
    }

    /** Toggling a direction removes it; adding appends it as the lowest priority. */
    @Override
    public boolean menuAction(int id) {
        if (id >= 6 && id < 36) {
            int encoded = id - 6;
            return movePriority(Direction.from3DDataValue(encoded % 6), encoded / 6);
        }
        if (id < 0 || id >= 6) return false;
        var side = Direction.from3DDataValue(id);
        if (priority.remove(side)) {
            setChanged();
            return true;
        }
        if (priority.size() >= MAX_PRIORITY || side.equals(facing())) return false;
        priority.add(side);
        setChanged();
        return true;
    }

    private boolean movePriority(Direction side, int row) {
        int current = priority.indexOf(side);
        if (current == row) {
            priority.remove(current);
            setChanged();
            return true;
        }
        if (side.equals(facing())) return false;
        priority.remove(side);
        priority.add(Math.min(row, priority.size()), side);
        while (priority.size() > MAX_PRIORITY) priority.remove(priority.size() - 1);
        setChanged();
        return true;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        priority.clear();
        for (int index : input.getIntArray("priority").orElse(new int[0])) {
            var side = Direction.from3DDataValue(index);
            if (priority.size() < MAX_PRIORITY && !priority.contains(side)) priority.add(side);
        }
        // Legacy updateConnectivity drops the front from the list whenever facing changes.
        priority.remove(facing());
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putIntArray(
                "priority", priority.stream().mapToInt(Direction::get3DDataValue).toArray());
    }
}
