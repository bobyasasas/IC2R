package ic2.core.machine;

/** Stable IDs retain the four legacy modes; inventory rules derive from the same value. */
public enum CannerMode {
    BOTTLE_SOLID(0, "bottle_solid"),
    EMPTY_LIQUID(1, "empty_liquid"),
    BOTTLE_LIQUID(2, "bottle_liquid"),
    ENRICH_LIQUID(3, "enrich_liquid");
    private final int id;
    private final String name;

    CannerMode(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int id() {
        return id;
    }

    public String serializedName() {
        return name;
    }

    public boolean acceptsAdditive() {
        return this == BOTTLE_SOLID || this == ENRICH_LIQUID;
    }

    public static CannerMode byId(int id) {
        if (id < 0 || id >= values().length)
            throw new IllegalArgumentException("Unknown canner mode " + id);
        return values()[id];
    }
}
