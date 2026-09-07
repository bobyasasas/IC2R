package ic2.core.machine;

import ic2.core.energy.EnergyStore;

import java.util.OptionalInt;

/** Recovered one-EU heating and one-unit cooling, including the legacy target + 1 boundary. */
public final class CentrifugeHeat {
    public static final int MAX_TARGET = 5000;
    private int heat;
    private int target = MAX_TARGET;

    public int heat() {
        return heat;
    }

    public int target() {
        return target;
    }

    public void restore(int heat) {
        if (heat < 0 || heat > MAX_TARGET + 1)
            throw new IllegalArgumentException("Invalid centrifuge heat");
        this.heat = heat;
    }

    public void tick(OptionalInt recipeTarget, boolean redstone, EnergyStore energy) {
        if (recipeTarget.isPresent()
                && (recipeTarget.getAsInt() < 0 || recipeTarget.getAsInt() > MAX_TARGET))
            throw new IllegalArgumentException("Invalid recipe heat");
        boolean heating = false;
        if (energy.stored() >= 1) {
            int requested = -1;
            if (recipeTarget.isPresent() && !redstone) {
                target = requested = recipeTarget.getAsInt();
                heat = Math.min(heat, target);
            } else if (redstone) target = requested = MAX_TARGET;
            if (heat - 1 < requested) heating = energy.consume(1);
        }
        heat = Math.clamp(heat + (heating ? 1 : -1), 0, MAX_TARGET + 1);
    }
}
