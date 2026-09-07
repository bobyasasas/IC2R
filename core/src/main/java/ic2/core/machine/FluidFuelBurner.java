package ic2.core.machine;

import ic2.core.energy.EnergyStore;

/** One metered fluid batch; a full EU buffer pauses the batch without losing fuel. */
public final class FluidFuelBurner {
    public record Fuel(int millibuckets, int ticks, double euPerTick) {
        public Fuel {
            if (millibuckets <= 0 || ticks <= 0 || !Double.isFinite(euPerTick) || euPerTick <= 0)
                throw new IllegalArgumentException("Invalid fluid fuel");
        }
    }

    public record State(int remaining, int total, double euPerTick) {
        public State {
            if (remaining < 0
                    || total < remaining
                    || !Double.isFinite(euPerTick)
                    || euPerTick < 0
                    || remaining > 0 && euPerTick == 0)
                throw new IllegalArgumentException("Invalid fuel state");
        }
    }

    private State state = new State(0, 0, 0);

    public State state() {
        return state;
    }

    public void restore(State state) {
        this.state = java.util.Objects.requireNonNull(state);
    }

    public boolean needsFuel(EnergyStore energy, Fuel fuel) {
        return state.remaining() == 0 && energy.free() >= fuel.euPerTick();
    }

    public void accept(Fuel fuel) {
        if (state.remaining() != 0) throw new IllegalStateException("Fuel batch is still burning");
        state = new State(fuel.ticks(), fuel.ticks(), fuel.euPerTick());
    }

    public boolean tick(EnergyStore energy) {
        if (state.remaining() == 0 || energy.free() < state.euPerTick()) return false;
        energy.insert(state.euPerTick());
        state = new State(state.remaining() - 1, state.total(), state.euPerTick());
        return true;
    }
}
