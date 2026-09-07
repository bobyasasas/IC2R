package ic2.core.machine;

/** Paid heat reserve: solid fuel burns over time; fluid batches are paid before emitting heat. */
public final class HeatFuelCycle {
    public record State(int remaining, int total, int emission, long reserve) {
        public State {
            if (remaining < 0
                    || total < remaining
                    || emission < 0
                    || reserve < 0
                    || remaining > 0 && emission == 0)
                throw new IllegalArgumentException("Invalid heat fuel state");
        }
    }

    private State state = new State(0, 0, 0, 0);

    public State state() {
        return state;
    }

    public void restore(State state) {
        this.state = java.util.Objects.requireNonNull(state);
    }

    public boolean idle() {
        return state.remaining() == 0 && state.reserve() == 0;
    }

    public void acceptSolid(int ticks, int emission) {
        if (!idle() || ticks <= 0 || emission <= 0)
            throw new IllegalStateException("Cannot accept solid fuel");
        state = new State(ticks, ticks, emission, 0);
    }

    public void acceptFluid(int ticks, int emission) {
        if (!idle() || ticks <= 0 || emission <= 0)
            throw new IllegalStateException("Cannot accept fluid fuel");
        state = new State(0, ticks, emission, (long) ticks * emission);
    }

    /** Returns true exactly once at the end of a solid-fuel batch. */
    public boolean burn() {
        if (state.remaining() == 0) return false;
        state =
                new State(
                        state.remaining() - 1,
                        state.total(),
                        state.emission(),
                        Math.addExact(state.reserve(), state.emission()));
        return state.remaining() == 0;
    }

    public int transferTo(WorkBuffer output, int target) {
        int accepted = output.insert((int) Math.min(Integer.MAX_VALUE, state.reserve()), target);
        state =
                new State(
                        state.remaining(),
                        state.total(),
                        state.emission(),
                        state.reserve() - accepted);
        return accepted;
    }
}
