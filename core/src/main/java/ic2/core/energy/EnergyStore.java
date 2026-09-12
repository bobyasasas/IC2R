package ic2.core.energy;

/** Bounded EU storage. All mutations are owned by the server thread. */
public final class EnergyStore {
    private double capacity;
    private double stored;

    public EnergyStore(double capacity) {
        if (!Double.isFinite(capacity) || capacity <= 0)
            throw new IllegalArgumentException("Invalid capacity");
        this.capacity = capacity;
    }

    /** Removing storage upgrades discards only energy above the new physical capacity. */
    public void resize(double capacity) {
        if (!Double.isFinite(capacity) || capacity <= 0)
            throw new IllegalArgumentException("Invalid capacity");
        this.capacity = capacity;
        stored = Math.min(stored, capacity);
    }

    public double capacity() {
        return capacity;
    }

    public double stored() {
        return stored;
    }

    public double free() {
        return capacity - stored;
    }

    public void restore(double value) {
        if (!Double.isFinite(value) || value < 0 || value > capacity)
            throw new IllegalArgumentException("Invalid stored energy");
        stored = value;
    }

    public double insert(double amount) {
        checkAmount(amount);
        double accepted = Math.min(amount, free());
        stored += accepted;
        return accepted;
    }

    public double extract(double amount) {
        checkAmount(amount);
        double extracted = Math.min(amount, stored);
        stored -= extracted;
        return extracted;
    }

    public boolean consume(double amount) {
        checkAmount(amount);
        if (stored < amount) return false;
        stored -= amount;
        return true;
    }

    /**
     * Legacy forceAddEnergy: deposits past the capacity cap. Only the luminator hand-discharge
     * quirk uses this — the grid can never push a sink above capacity because it goes through
     * {@link #insert}, but a discharged electric item may top the store up to 10000 EU.
     */
    public void forceAdd(double amount) {
        checkAmount(amount);
        stored += amount;
    }

    private static void checkAmount(double amount) {
        if (!Double.isFinite(amount) || amount < 0)
            throw new IllegalArgumentException("Invalid transfer amount");
    }
}
