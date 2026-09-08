package ic2.core.machine;

/** Water conditions sampled by the platform; signed rotation never produces negative work. */
public record WaterFlow(
        Body body,
        long clockTime,
        int shoreDistance,
        boolean reverse,
        int obstructions,
        double turbulence) {
    public enum Body {
        INVALID,
        RIVER,
        OCEAN,
        DEEP_OCEAN;

        public int diameter(RotorMaterial rotor) {
            return this == RIVER ? (rotor.diameter() + 1) * 2 / 3 : rotor.diameter();
        }
    }

    public WaterFlow {
        if (body == null
                || shoreDistance < 1
                || shoreDistance > 200
                || !Double.isFinite(turbulence)
                || turbulence < 0
                || turbulence > 1) throw new IllegalArgumentException("Invalid water conditions");
    }

    public RotorOperation operation(RotorMaterial rotor, double multiplier) {
        if (!rotor.supportsWater())
            throw new IllegalArgumentException("Rotor does not support water");
        if (!Double.isFinite(multiplier) || multiplier < 0)
            throw new IllegalArgumentException("Invalid water multiplier");
        int diameter = body.diameter(rotor);
        if (multiplier == 0)
            return RotorOperation.stopped(diameter, RotorOperation.Status.DISABLED);
        if (body == Body.INVALID)
            return RotorOperation.stopped(diameter, RotorOperation.Status.INVALID_BIOME);
        if (obstructions < 0)
            return RotorOperation.stopped(diameter, RotorOperation.Status.INTERFERENCE);
        int width = diameter / 2 * 4 + 1;
        if (obstructions > width * width)
            throw new IllegalArgumentException("Obstructions exceed cross section");
        int effectiveObstructions = obstructions <= (diameter + 1) / 2 ? 0 : obstructions;
        double fraction = effectiveObstructions / (double) (width * width);
        double speed;
        int flow, wear;
        if (body == Body.RIVER) {
            speed = Math.clamp(shoreDistance, 20, 50) / 50.0;
            flow =
                    (int)
                            ((int) (speed * 1000)
                                    * rotor.efficiency()
                                    * (1 - .3 * turbulence - .1 * fraction));
            wear = 1;
        } else {
            double tide = Math.sin(Math.floorMod(clockTime, 12000) * Math.PI / 6000);
            speed = tide * Math.abs(tide) * shoreDistance / 100.0 * (1 - fraction * fraction);
            flow =
                    (int)
                            ((int) (speed * (body == Body.DEEP_OCEAN ? 4000 : 3000))
                                    * rotor.efficiency());
            wear = body == Body.DEEP_OCEAN ? 3 : 2;
        }
        int output = (int) Math.min(Integer.MAX_VALUE, Math.abs(flow * .2 * multiplier));
        return new RotorOperation(
                output,
                diameter,
                (float) (speed * (reverse ? -5 : 5)),
                output == 0 ? RotorOperation.Status.NO_FLOW : RotorOperation.Status.RUNNING,
                shoreDistance,
                effectiveObstructions,
                wear);
    }
}
