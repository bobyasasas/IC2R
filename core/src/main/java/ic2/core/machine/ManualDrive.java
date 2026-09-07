package ic2.core.machine;

/** One player's crank operation; click limits are shared by all players using the machine. */
public final class ManualDrive {
    public record State(long tick, int clicks) {
        public State {
            if (clicks < 0 || clicks > 10)
                throw new IllegalArgumentException("Invalid click count");
        }
    }

    public record Result(boolean accepted, int added) {}

    private State state = new State(Long.MIN_VALUE, 0);

    public State state() {
        return state;
    }

    public void restore(State state) {
        this.state = java.util.Objects.requireNonNull(state);
    }

    public Result click(
            long tick, int food, boolean simulatedPlayer, double multiplier, WorkBuffer work) {
        if (!Double.isFinite(multiplier) || multiplier < 0)
            throw new IllegalArgumentException("Invalid manual multiplier");
        int used = state.tick() == tick ? state.clicks() : 0;
        int produced = (int) Math.min(1000, Math.round((simulatedPlayer ? 20 : 400) * multiplier));
        if (food <= 6 || used >= 10 || produced == 0) return new Result(false, 0);
        state = new State(tick, used + 1);
        return new Result(true, work.insert(produced, 1000));
    }
}
