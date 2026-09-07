package ic2.core.machine;

import ic2.core.energy.EnergyStore;

/** Legacy container batches and hundredth-EU ambient generation, with persistent batch power. */
public final class WaterMill {
    public static final int MAX_FUEL = 2000;

    public record State(int fuel, double production) {
        public State {
            if (fuel < 0 || fuel > MAX_FUEL || !Double.isFinite(production) || production < 0)
                throw new IllegalArgumentException("Invalid water mill state");
        }
    }

    private int fuel;
    private double production;

    public State state() {
        return new State(fuel, production);
    }

    public void restore(State state) {
        fuel = state.fuel();
        production = state.production();
    }

    public boolean canAcceptContainer() {
        return fuel + 500 <= MAX_FUEL;
    }

    public void acceptContainer(boolean returnsContainer) {
        if (!canAcceptContainer())
            throw new IllegalStateException("Water mill batch buffer is full");
        fuel += 500;
        production = returnsContainer ? 1 : 2;
    }

    public void prepareAmbient(int waterBlocks, double multiplier) {
        if (waterBlocks < 0 || waterBlocks > 27 || !Double.isFinite(multiplier) || multiplier < 0)
            throw new IllegalArgumentException("Invalid ambient water sample");
        if (fuel > 0) return;
        production = waterBlocks * multiplier / 100;
        if (production > 0) fuel = 1;
    }

    public boolean tick(EnergyStore energy) {
        if (fuel <= 0) return false;
        // The recovered water mill consumes its batch even when the tiny EU buffer is full.
        energy.insert(production);
        fuel--;
        return true;
    }
}
