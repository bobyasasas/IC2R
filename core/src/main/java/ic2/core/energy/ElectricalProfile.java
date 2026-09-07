package ic2.core.energy;

import java.util.Objects;

/** Mutable machine settings, owned by the server thread; independent of loader and world state. */
public final class ElectricalProfile {
    private VoltageTier workingVoltage;
    private VoltageTier sinkWorkingVoltage;
    private int recipePower;
    private int maxSinkAmperageOverride = -1;

    public ElectricalProfile(VoltageTier workingVoltage) {
        this.workingVoltage = Objects.requireNonNull(workingVoltage, "workingVoltage");
    }

    public VoltageTier getWorkingVoltage() {
        return this.workingVoltage;
    }

    public void setWorkingVoltage(VoltageTier workingVoltage) {
        this.workingVoltage = Objects.requireNonNull(workingVoltage, "workingVoltage");
    }

    public VoltageTier getSinkWorkingVoltage() {
        return this.sinkWorkingVoltage != null ? this.sinkWorkingVoltage : this.workingVoltage;
    }

    /** A null override restores the working-voltage fallback, as in the Forge implementation. */
    public void setSinkWorkingVoltage(VoltageTier sinkWorkingVoltage) {
        this.sinkWorkingVoltage = sinkWorkingVoltage;
    }

    public void clearSinkWorkingVoltage() {
        this.sinkWorkingVoltage = null;
    }

    public int getRecipePower() {
        return this.recipePower;
    }

    public void setRecipePower(int recipePower) {
        this.recipePower = Math.max(0, recipePower);
    }

    /** Negative values restore the calculated limit; zero explicitly disables input. */
    public void setMaxSinkAmperageOverride(int maxSinkAmperageOverride) {
        this.maxSinkAmperageOverride = maxSinkAmperageOverride;
    }

    public void clearMaxSinkAmperageOverride() {
        this.maxSinkAmperageOverride = -1;
    }

    public double getDisplayCurrent() {
        int voltage = this.workingVoltage.getVoltage();
        return (double) this.recipePower / voltage;
    }

    public int getWorkingCurrent() {
        if (this.recipePower <= 0) {
            return 0;
        }

        int voltage = this.workingVoltage.getVoltage();
        // Promote before addition so even Integer.MAX_VALUE EU/t cannot overflow.
        return (int) ((this.recipePower + (long) voltage - 1) / voltage);
    }

    public int getMaxSinkAmperage() {
        if (this.maxSinkAmperageOverride >= 0) {
            return this.maxSinkAmperageOverride;
        }

        if (this.recipePower <= 0) {
            return 1;
        }

        int voltage = this.workingVoltage.getVoltage();
        // Keep the legacy safety margin: floor(2 * power / voltage) + 1.
        return (int) (2L * this.recipePower / voltage + 1);
    }
}
