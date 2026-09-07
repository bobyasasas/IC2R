package ic2.core.machine;

import ic2.core.recipe.ProcessingMethod;

public enum MetalFormerMode {
    EXTRUDING(0, "extruding", ProcessingMethod.METAL_FORMER_EXTRUDING),
    ROLLING(1, "rolling", ProcessingMethod.METAL_FORMER_ROLLING),
    CUTTING(2, "cutting", ProcessingMethod.METAL_FORMER_CUTTING);
    private final int id;
    private final String name;
    private final ProcessingMethod method;

    MetalFormerMode(int id, String name, ProcessingMethod method) {
        this.id = id;
        this.name = name;
        this.method = method;
    }

    public int id() {
        return id;
    }

    public String serializedName() {
        return name;
    }

    public ProcessingMethod method() {
        return method;
    }

    public MetalFormerMode next() {
        return switch (this) {
            case EXTRUDING -> ROLLING;
            case ROLLING -> CUTTING;
            case CUTTING -> EXTRUDING;
        };
    }

    public static MetalFormerMode byId(int id) {
        return switch (id) {
            case 1 -> ROLLING;
            case 2 -> CUTTING;
            default -> EXTRUDING;
        };
    }
}
