package ic2.core.machine;

import ic2.core.energy.EnergyStore;

/** A shared two-row batch; heat advances work and completion is consumed at the next tick. */
public final class InductionCycle {
    public static final int MAX_HEAT = 10000, WORK = 4000;

    public record State(int heat, int progress) {
        public State {
            if (heat < 0 || heat > MAX_HEAT || progress < 0 || progress >= WORK + MAX_HEAT / 30)
                throw new IllegalArgumentException("Invalid induction state");
        }
    }

    private int heat, progress;

    public State state() {
        return new State(heat, progress);
    }

    public void restore(State state) {
        heat = state.heat();
        progress = state.progress();
    }

    public boolean completionReady() {
        return progress >= WORK;
    }

    public void completed() {
        progress = 0;
    }

    public boolean tick(boolean canProcess, boolean redstone, EnergyStore energy) {
        if (completionReady())
            throw new IllegalStateException("Consume the completed batch before ticking");
        if ((canProcess || redstone) && energy.consume(1)) heat = Math.min(MAX_HEAT, heat + 1);
        else heat = Math.max(0, heat - 4);
        if (!canProcess) {
            progress = 0;
            return false;
        }
        // Work cannot advance unless the full processing cost was paid.
        if (!energy.consume(15)) return false;
        progress += heat / 30;
        return true;
    }
}
