package ic2.core.machine;

/** The platform supplies light and celestial attributes; brightness math has no world access. */
public final class SolarGeneration {
    public static double brightness(
            boolean skylight,
            int light,
            double sunAngleDegrees,
            boolean sandy,
            double rain,
            double thunder) {
        if (!skylight) return 0;
        double sunlight = Math.clamp(Math.cos(Math.toRadians(sunAngleDegrees)) * 2 + .2, 0, 1);
        if (!sandy)
            sunlight *=
                    (1 - Math.clamp(rain, 0, 1) * 5 / 16)
                            * (1 - Math.clamp(thunder, 0, 1) * 5 / 16);
        return Math.clamp(light, 0, 15) / 15.0 * sunlight;
    }

    private SolarGeneration() {}
}
