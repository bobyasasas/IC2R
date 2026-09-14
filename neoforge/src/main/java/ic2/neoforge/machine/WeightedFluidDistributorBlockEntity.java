package ic2.neoforge.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.ArrayList;
import java.util.List;

/**
 * Always drinks from the front, but pours the whole tank along the priority list — each neighbour
 * takes as much as it can before the next one is tried.
 */
public final class WeightedFluidDistributorBlockEntity extends FluidDistributorBlockEntity {
    public static final int MAX_PRIORITY = 5;

    private final List<Direction> priority = new ArrayList<>();

    public WeightedFluidDistributorBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state);
    }

    public List<Direction> priority() {
        return priority;
    }

    @Override
    protected boolean acceptsFrom(Direction side) {
        return side.equals(facing());
    }

    @Override
    protected void moveFluid(ServerLevel level) {
        for (var side : priority) {
            if (tank.getAmountAsInt(0) <= 0) return;
            push(level, side, tank.getAmountAsInt(0));
        }
    }

    /** Values 0..4 mirror the priority list as 3D data, -1 padding; 5/6 keep the tank readout. */
    @Override
    public int menuValue(int index) {
        if (index >= 0 && index < MAX_PRIORITY)
            return index < priority.size() ? priority.get(index).get3DDataValue() : -1;
        return super.menuValue(index - MAX_PRIORITY + 1);
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
