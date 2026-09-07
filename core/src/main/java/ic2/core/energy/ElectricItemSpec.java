package ic2.core.energy;

/** Stable electrical capabilities of an item, separate from the charge of an individual stack. */
public record ElectricItemSpec(
        double capacity, double transferLimit, int tier, boolean externalOutput) {
    public ElectricItemSpec {
        if (!Double.isFinite(capacity)
                || capacity <= 0
                || !Double.isFinite(transferLimit)
                || transferLimit < 0
                || tier < 0) {
            throw new IllegalArgumentException("Invalid electric item specification");
        }
    }
}
