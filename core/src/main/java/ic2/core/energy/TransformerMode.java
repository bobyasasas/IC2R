package ic2.core.energy;

import ic2.core.energy.grid.EnergyMode;

public enum TransformerMode {
    REDSTONE,
    STEP_DOWN,
    STEP_UP;

    public boolean stepUp(boolean powered) {
        return this == STEP_UP || this == REDSTONE && powered;
    }

    public static boolean unsafeSwitch(
            EnergyMode energyMode, boolean previousStepUp, boolean nextStepUp, double stored) {
        return energyMode == EnergyMode.GT && previousStepUp != nextStepUp && stored > 0;
    }

    public static TransformerMode fromSavedId(int id) {
        return id >= 0 && id < values().length ? values()[id] : REDSTONE;
    }
}
