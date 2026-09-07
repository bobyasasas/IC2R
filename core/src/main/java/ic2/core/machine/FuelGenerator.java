package ic2.core.machine;

import ic2.core.energy.EnergyStore;

/** Fuel accounting for the basic generator: vanilla burn time / 4, 10 EU per active tick. */
public final class FuelGenerator {
    public record State(int remaining, int total) {
        public State {
            if (remaining < 0 || total < remaining)
                throw new IllegalArgumentException("Invalid fuel state");
        }
    }

    private int remaining, total;

    public State state() {
        return new State(remaining, total);
    }

    public void restore(State state) {
        remaining = state.remaining();
        total = state.total();
    }

    public boolean needsFuel(EnergyStore energy, double production) {
        return remaining == 0 && energy.free() >= production;
    }

    public boolean acceptFuel(int vanillaBurnTicks) {
        if (remaining > 0 || vanillaBurnTicks < 4) return false;
        total = remaining = vanillaBurnTicks / 4;
        return true;
    }

    public boolean tick(EnergyStore energy, double production) {
        if (remaining == 0) return false;
        energy.insert(production);
        remaining--;
        return true;
    }
}
