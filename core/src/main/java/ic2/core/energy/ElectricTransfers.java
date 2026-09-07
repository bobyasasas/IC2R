package ic2.core.energy;

/** Pure charge/discharge rules. Applying a result to a stack belongs to the platform adapter. */
public final class ElectricTransfers {
    public record Result(double stored, double transferred) {}

    private ElectricTransfers() {}

    public static Result charge(
            double stored,
            ElectricItemSpec spec,
            double requested,
            int sourceTier,
            boolean ignoreLimit) {
        validateStored(stored, spec);
        double amount = allowed(requested, spec, sourceTier, ignoreLimit);
        amount = Math.min(amount, spec.capacity() - stored);
        return new Result(stored + amount, amount);
    }

    public static Result discharge(
            double stored,
            ElectricItemSpec spec,
            double requested,
            int receiverTier,
            boolean ignoreLimit,
            boolean external) {
        validateStored(stored, spec);
        double amount =
                external && !spec.externalOutput()
                        ? 0
                        : Math.min(stored, allowed(requested, spec, receiverTier, ignoreLimit));
        return new Result(stored - amount, amount);
    }

    private static double allowed(
            double requested, ElectricItemSpec spec, int tier, boolean ignoreLimit) {
        if (Double.isNaN(requested) || requested <= 0 || tier < spec.tier()) return 0;
        return ignoreLimit ? requested : Math.min(requested, spec.transferLimit());
    }

    private static void validateStored(double stored, ElectricItemSpec spec) {
        if (!Double.isFinite(stored) || stored < 0 || stored > spec.capacity()) {
            throw new IllegalArgumentException("Stored energy is outside the item capacity");
        }
    }
}
