package ic2.core.machine;

/** Fixed world-time cadence and a persisted same-tick guard for paid fluid deliveries. */
public final class FlowRegulator {
    public record State(int amount, boolean perTick, long deliveredTick) {
        public State {
            if (amount < 0 || amount > 1000)
                throw new IllegalArgumentException("Invalid regulator amount");
        }
    }

    private State state = new State(0, false, Long.MIN_VALUE);

    public State state() {
        return state;
    }

    public void restore(State state) {
        this.state = state;
    }

    public boolean configure(int button) {
        int adjustment =
                switch (button) {
                    case 0 -> 1;
                    case 1 -> 10;
                    case 2 -> 100;
                    case 3 -> 1000;
                    case 4 -> -1;
                    case 5 -> -10;
                    case 6 -> -100;
                    case 7 -> -1000;
                    default -> 0;
                };
        if (button < 0 || button > 9) return false;
        boolean perTick =
                switch (button) {
                    case 8 -> true;
                    case 9 -> false;
                    default -> state.perTick();
                };
        state =
                new State(
                        Math.clamp(state.amount() + adjustment, 0, 1000),
                        perTick,
                        state.deliveredTick());
        return true;
    }

    public boolean ready(long tick, int phase) {
        return state.amount() > 0
                && state.deliveredTick() != tick
                && (state.perTick() || Math.floorMod(tick, 20) == Math.floorMod(phase, 20));
    }

    public void delivered(long tick) {
        state = new State(state.amount(), state.perTick(), tick);
    }
}
