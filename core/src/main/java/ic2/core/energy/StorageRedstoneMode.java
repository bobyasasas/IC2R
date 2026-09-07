package ic2.core.energy;

/** Legacy storage thresholds, independent of world power and neighbor updates. */
public enum StorageRedstoneMode {
    IGNORE,
    EMIT_NEAR_FULL,
    EMIT_PARTIAL,
    EMIT_NOT_FULL,
    EMIT_EMPTY,
    STOP_WHEN_POWERED,
    OVERFLOW_WHEN_POWERED;

    public boolean emitsSignal(double stored, double capacity, int voltage) {
        return switch (this) {
            case EMIT_NEAR_FULL -> stored >= capacity - voltage * 20.0;
            case EMIT_PARTIAL -> stored > voltage && stored < capacity - voltage;
            case EMIT_NOT_FULL -> stored < capacity - voltage;
            case EMIT_EMPTY -> stored < voltage;
            default -> false;
        };
    }

    public boolean emitsEnergy(boolean powered, double stored, double capacity, int voltage) {
        return switch (this) {
            case STOP_WHEN_POWERED -> !powered;
            case OVERFLOW_WHEN_POWERED -> !powered || stored > capacity - voltage * 20.0;
            default -> true;
        };
    }

    public static StorageRedstoneMode fromSavedId(int id) {
        return id >= 0 && id < values().length ? values()[id] : IGNORE;
    }
}
