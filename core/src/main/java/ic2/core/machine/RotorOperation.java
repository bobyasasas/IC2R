package ic2.core.machine;

/** One environment sample; output is a per-tick rate, and wear applies at the sampling interval. */
public record RotorOperation(
        int output,
        int diameter,
        float degreesPerTick,
        Status status,
        float environment,
        int obstructions,
        int wear) {
    public enum Status {
        NO_ROTOR,
        INVALID_FACING,
        NO_SPACE,
        INTERFERENCE,
        LOW_WIND,
        RUNNING,
        OVERLOADED,
        DISABLED,
        INVALID_BIOME,
        NO_FLOW
    }

    public RotorOperation {
        if (output < 0
                || diameter < 0
                || diameter > 11
                || !Float.isFinite(degreesPerTick)
                || !Float.isFinite(environment)
                || wear < 0
                || wear > 4) throw new IllegalArgumentException("Invalid rotor operation");
    }

    public static RotorOperation stopped(int diameter, Status status) {
        return new RotorOperation(0, diameter, 0, status, 0, 0, 0);
    }

    public static RotorOperation wind(
            RotorMaterial rotor, double wind, int obstructions, double multiplier) {
        if (!Double.isFinite(wind) || !Double.isFinite(multiplier) || multiplier < 0)
            throw new IllegalArgumentException("Invalid wind conditions");
        if (obstructions < 0) return stopped(rotor.diameter(), Status.INTERFERENCE);
        if (multiplier == 0) return stopped(rotor.diameter(), Status.DISABLED);
        int width = rotor.diameter() / 2 * 4 + 1;
        if (obstructions > width * width)
            throw new IllegalArgumentException("Obstructions exceed cross section");
        int effectiveObstructions = obstructions <= (rotor.diameter() + 1) / 2 ? 0 : obstructions;
        double fraction = effectiveObstructions / (double) (width * width);
        double strength = Math.max(0, wind * (1 - fraction * fraction));
        boolean strong = strength >= rotor.minimumWind(),
                overloaded = strength > rotor.maximumWind();
        int output =
                strong
                        ? (int)
                                Math.min(
                                        Integer.MAX_VALUE,
                                        strength * 10 * rotor.efficiency() * multiplier)
                        : 0;
        float degrees =
                (float)
                        (Math.clamp((strength - rotor.minimumWind()) / rotor.maximumWind(), 0, 2)
                                * 50);
        return new RotorOperation(
                output,
                rotor.diameter(),
                degrees,
                !strong ? Status.LOW_WIND : overloaded ? Status.OVERLOADED : Status.RUNNING,
                (float) strength,
                effectiveObstructions,
                strong ? overloaded ? 4 : 1 : 0);
    }
}
