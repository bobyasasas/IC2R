package ic2.core.energy;

/** IC2 voltage levels, ordered by the maximum supported EU per tick. */
public enum VoltageTier {
    ULV(8, 0),
    LV(32, 1),
    MV(128, 2),
    HV(512, 3),
    EV(2048, 4),
    IV(8192, 5);

    private static final VoltageTier[] BY_IC_TIER = values();
    private final int voltage;
    private final int icTier;

    VoltageTier(int voltage, int icTier) {
        this.voltage = voltage;
        this.icTier = icTier;
    }

    public int getVoltage() {
        return voltage;
    }

    public int getIcTier() {
        return icTier;
    }

    public String getTranslationKey() {
        return switch (this) {
            case ULV -> "ic2.voltage.ulv";
            case LV -> "ic2.voltage.lv";
            case MV -> "ic2.voltage.mv";
            case HV -> "ic2.voltage.hv";
            case EV -> "ic2.voltage.ev";
            case IV -> "ic2.voltage.iv";
        };
    }

    public static VoltageTier fromIcTier(int tier) {
        return BY_IC_TIER[Math.clamp(tier, ULV.icTier, IV.icTier)];
    }

    /**
     * Smallest tier covering the power, saturated to the supported range. Non-positive values and
     * NaN retain the legacy ULV fallback. Direct comparisons avoid logarithm rounding at the
     * voltage boundaries.
     */
    public static VoltageTier fromPower(double power) {
        if (Double.isNaN(power)) {
            return ULV;
        }
        for (VoltageTier tier : BY_IC_TIER) {
            if (power <= tier.voltage) {
                return tier;
            }
        }
        return IV;
    }
}
