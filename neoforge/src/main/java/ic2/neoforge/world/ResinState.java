package ic2.neoforge.world;

import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

/** Stable saved names with explicit dry/wet relationships, independent of enum arithmetic. */
public enum ResinState implements StringRepresentable {
    PLAIN(null, false),
    DRY_NORTH(Direction.NORTH, false),
    DRY_SOUTH(Direction.SOUTH, false),
    DRY_WEST(Direction.WEST, false),
    DRY_EAST(Direction.EAST, false),
    WET_NORTH(Direction.NORTH, true),
    WET_SOUTH(Direction.SOUTH, true),
    WET_WEST(Direction.WEST, true),
    WET_EAST(Direction.EAST, true);
    private final Direction facing;
    private final boolean wet;

    ResinState(Direction facing, boolean wet) {
        this.facing = facing;
        this.wet = wet;
    }

    public Direction facing() {
        return facing;
    }

    public boolean wet() {
        return wet;
    }

    public boolean plain() {
        return this == PLAIN;
    }

    public ResinState withWet(boolean wet) {
        if (plain()) return PLAIN;
        return switch (facing) {
            case NORTH -> wet ? WET_NORTH : DRY_NORTH;
            case SOUTH -> wet ? WET_SOUTH : DRY_SOUTH;
            case WEST -> wet ? WET_WEST : DRY_WEST;
            case EAST -> wet ? WET_EAST : DRY_EAST;
            default -> throw new IllegalStateException("Vertical resin hole");
        };
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
