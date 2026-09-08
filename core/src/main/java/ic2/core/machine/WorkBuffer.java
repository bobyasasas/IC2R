package ic2.core.machine;

import ic2.core.energy.EnergyStore;

/** Heat/kinetic reservoir with an aggregate per-world-tick extraction budget. */
public final class WorkBuffer implements WorkSupply {
    public record State(double stored, long tick, int extracted) {
        public State {
            if (!Double.isFinite(stored) || stored < 0 || extracted < 0)
                throw new IllegalArgumentException("Invalid work buffer state");
        }
    }

    private final int capacity;
    private State state = new State(0, Long.MIN_VALUE, 0);

    public WorkBuffer(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException("Invalid capacity");
        this.capacity = capacity;
    }

    public State state() {
        return state;
    }

    public void restore(State state) {
        if (state.stored() > capacity) throw new IllegalArgumentException("Work exceeds capacity");
        this.state = state;
    }

    /**
     * Replace expired flow with a newly paid production batch, retaining this tick's draw count.
     */
    public void publish(int amount) {
        if (amount < 0 || amount > capacity)
            throw new IllegalArgumentException("Invalid work batch");
        state = new State(amount, state.tick(), state.extracted());
    }

    /** Add already paid units, retaining any excess in the caller's reserve. */
    public int insert(int maximum, int target) {
        if (maximum < 0 || target < 0 || target > capacity)
            throw new IllegalArgumentException("Invalid work insertion");
        int accepted = (int) Math.min(maximum, Math.max(0, target - state.stored()));
        state = new State(state.stored() + accepted, state.tick(), state.extracted());
        return accepted;
    }

    public int available(long tick, int bandwidth) {
        if (bandwidth < 0) throw new IllegalArgumentException("Negative bandwidth");
        int used = tick == state.tick() ? state.extracted() : 0;
        return (int) Math.min(Math.floor(state.stored()), Math.max(0, bandwidth - used));
    }

    public int extract(long tick, int bandwidth, int requested) {
        if (requested < 0) throw new IllegalArgumentException("Negative request");
        int amount = Math.min(requested, available(tick, bandwidth));
        int used = tick == state.tick() ? state.extracted() : 0;
        state = new State(state.stored() - amount, tick, used + amount);
        return amount;
    }

    public double generate(EnergyStore energy, double unitsPerEu, int target, boolean wholeUnits) {
        if (!Double.isFinite(unitsPerEu) || unitsPerEu < 0 || target < 0 || target > capacity)
            throw new IllegalArgumentException("Invalid work conversion");
        if (unitsPerEu == 0) return 0;
        double units = Math.min(Math.max(0, target - state.stored()), energy.stored() * unitsPerEu);
        if (wholeUnits) units = Math.floor(units);
        if (units <= 0 || !energy.consume(units / unitsPerEu)) return 0;
        state = new State(state.stored() + units, state.tick(), state.extracted());
        return units;
    }
}
