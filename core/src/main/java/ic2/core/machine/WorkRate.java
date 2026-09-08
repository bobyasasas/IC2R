package ic2.core.machine;

/** Unused turbine output expires each tick; multiple requests share one production budget. */
public final class WorkRate implements WorkSupply {
    public record State(long tick, int extracted) {
        public State {
            if (extracted < 0) throw new IllegalArgumentException("Negative extracted work");
        }
    }

    private State state = new State(Long.MIN_VALUE, 0);

    public State state() {
        return state;
    }

    public void restore(State state) {
        this.state = java.util.Objects.requireNonNull(state);
    }

    @Override
    public int available(long tick, int bandwidth) {
        if (bandwidth < 0) throw new IllegalArgumentException("Negative bandwidth");
        return Math.max(0, bandwidth - (state.tick() == tick ? state.extracted() : 0));
    }

    @Override
    public int extract(long tick, int bandwidth, int maximum) {
        if (maximum < 0) throw new IllegalArgumentException("Negative request");
        int amount = Math.min(maximum, available(tick, bandwidth));
        state = new State(tick, (state.tick() == tick ? state.extracted() : 0) + amount);
        return amount;
    }
}
