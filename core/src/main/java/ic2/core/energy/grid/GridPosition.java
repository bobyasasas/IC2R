package ic2.core.energy.grid;

/** Dimension-local position; the world adapter owns one graph per dimension. */
public record GridPosition(int x, int y, int z) implements Comparable<GridPosition> {
    @Override
    public int compareTo(GridPosition other) {
        int order = Integer.compare(x, other.x);
        if (order == 0) order = Integer.compare(y, other.y);
        return order == 0 ? Integer.compare(z, other.z) : order;
    }
}
