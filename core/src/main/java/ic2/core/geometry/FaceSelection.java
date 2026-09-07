package ic2.core.geometry;

/** Wrench hit zones: face center selects the face, edges adjacent faces, corners the back. */
public final class FaceSelection {
    public enum Face {
        DOWN,
        UP,
        NORTH,
        SOUTH,
        WEST,
        EAST;

        public Face opposite() {
            return values()[ordinal() ^ 1];
        }
    }

    public static Face select(Face hit, double x, double y, double z) {
        double a = hit == Face.WEST || hit == Face.EAST ? z : x;
        double b = hit == Face.UP || hit == Face.DOWN ? z : y;
        int horizontal = zone(a), vertical = zone(b);
        if (horizontal != 0 && vertical != 0) return hit.opposite();
        if (horizontal != 0)
            return hit == Face.WEST || hit == Face.EAST
                    ? (horizontal < 0 ? Face.NORTH : Face.SOUTH)
                    : (horizontal < 0 ? Face.WEST : Face.EAST);
        if (vertical != 0)
            return hit == Face.UP || hit == Face.DOWN
                    ? (vertical < 0 ? Face.NORTH : Face.SOUTH)
                    : (vertical < 0 ? Face.DOWN : Face.UP);
        return hit;
    }

    private static int zone(double value) {
        return value <= .25 ? -1 : value >= .75 ? 1 : 0;
    }

    private FaceSelection() {}
}
