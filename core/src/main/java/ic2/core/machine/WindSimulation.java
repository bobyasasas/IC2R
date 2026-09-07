package ic2.core.machine;

import java.util.function.IntUnaryOperator;

/** Persistent random walk and the legacy cubic height profile, without platform state. */
public final class WindSimulation {
    public static final double MAX_WIND = 108;

    public record State(int strength, int direction, int ticks) {
        public State {
            if (strength < 0
                    || strength > 30
                    || direction < 0
                    || direction >= 360
                    || ticks < 0
                    || ticks >= 128) throw new IllegalArgumentException("Invalid wind state");
        }
    }

    private State state;

    public WindSimulation(State state) {
        this.state = state;
    }

    public State state() {
        return state;
    }

    public void tick(IntUnaryOperator random) {
        int ticks = state.ticks() + 1;
        int strength = state.strength(), direction = state.direction();
        if (ticks == 128) {
            ticks = 0;
            int increase = 10 - Math.max(0, strength - 20);
            int decrease = 10 - Math.max(0, 10 - strength);
            if (random.applyAsInt(100) < increase) strength++;
            else if (random.applyAsInt(100) < decrease) strength--;
            direction = Math.floorMod(direction + (random.applyAsInt(3) - 1) * 18, 360);
        }
        state = new State(strength, direction, ticks);
    }

    public double windAt(
            int y, int worldHeight, int seaLevel, boolean raining, boolean thundering) {
        return state.strength()
                * heightFactor(y, worldHeight, seaLevel)
                * 2.4
                * (thundering ? 1.5 : raining ? 1.25 : 1);
    }

    /** f(peak)=1, f'(peak)=0 and f(falloff)=0; worldHeight is the dimension's span. */
    public static double heightFactor(double y, int worldHeight, int seaLevel) {
        double height = Math.max(1, worldHeight);
        double sea = Math.max(0, seaLevel);
        double base = sea < height ? sea : height * .5;
        double peak = base + (height - base) / 2;
        double r = height * 1.125 / peak, t = y / peak;
        return Math.max(
                0,
                (t * r * (2 * r - 3) + t * t * (3 - r * r) + t * t * t * (r - 2))
                        / ((r - 1) * (r - 1)));
    }

    public static double production(double wind, int obstructions, double multiplier) {
        if (obstructions < 0
                || obstructions > 566
                || !Double.isFinite(multiplier)
                || multiplier < 0)
            throw new IllegalArgumentException("Invalid windmill environment");
        return Math.max(0, wind) * (1 - obstructions / 567.0) * .1 * multiplier;
    }

    /** A whole packet plus one generation step avoids fractional production stalling below 32. */
    public static double capacity(boolean wholePackets, double multiplier) {
        return wholePackets ? 32 + Math.ceil(MAX_WIND * .1 * multiplier) : 32;
    }
}
