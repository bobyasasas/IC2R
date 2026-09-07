package ic2.core.machine;

/** Iron-furnace timing differs from electric processing: interruption resets progress. */
public final class FuelFurnace {
    public record State(int fuel, int totalFuel, int progress) {
        public State {
            if (fuel < 0 || totalFuel < fuel || progress < 0 || progress >= 160)
                throw new IllegalArgumentException("Invalid fuel furnace state");
        }
    }

    public record Tick(boolean burning, boolean completed) {}

    private State state = new State(0, 0, 0);

    public State state() {
        return state;
    }

    public void restore(State state) {
        this.state = java.util.Objects.requireNonNull(state);
    }

    public boolean needsFuel(boolean canOperate) {
        return state.fuel() == 0 && canOperate;
    }

    public void acceptFuel(int ticks) {
        if (ticks <= 0 || state.fuel() != 0)
            throw new IllegalArgumentException("Cannot replace burning fuel");
        state = new State(ticks, ticks, state.progress());
    }

    public Tick tick(boolean canOperate) {
        boolean burning = state.fuel() > 0;
        int progress = burning && canOperate ? state.progress() + 1 : 0;
        boolean completed = progress == 160;
        state =
                new State(
                        Math.max(0, state.fuel() - 1), state.totalFuel(), completed ? 0 : progress);
        return new Tick(burning, completed);
    }
}
