package ic2.core.explosion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** Bounded ray tracing for the boiler's 1- and 10-power thermal bursts. No world mutation. */
public final class HeatBlast {
    public record Position(int x, int y, int z) {}

    /** A negative absorption is a barrier, including unloaded terrain. */
    public record Cell(double absorption, boolean solid) {
        public static final Cell AIR = new Cell(0.5, false);
        public static final Cell WATER = new Cell(1.5, true);
        public static final Cell BARRIER = new Cell(-1, false);

        public Cell {
            if (!Double.isFinite(absorption) || absorption >= 0 && absorption < 0.5)
                throw new IllegalArgumentException("Invalid heat blast absorption");
        }

        public static Cell solid(double resistance) {
            return !Double.isFinite(resistance) || resistance < 0
                    ? BARRIER
                    : new Cell(0.5 + (resistance + 4) * 1.8, true);
        }
    }

    public record Sample(double x, double y, double z, double power, int step) {}

    /** The block flag means its drops are destroyed by a ray with more than eight power. */
    public record Result(Map<Position, Boolean> blocks, List<Sample> samples) {
        public Result {
            blocks = Map.copyOf(blocks);
            samples = List.copyOf(samples);
        }
    }

    public static Result trace(
            int power, double x, double y, double z, Function<Position, Cell> terrain) {
        if (power < 1
                || power > 10
                || !Double.isFinite(x)
                || !Double.isFinite(y)
                || !Double.isFinite(z))
            throw new IllegalArgumentException("Unsupported heat blast");
        var cache = new HashMap<Position, Cell>();
        var blocks = new LinkedHashMap<Position, Boolean>();
        var samples = new ArrayList<Sample>();
        int angles = (int) Math.ceil(Math.PI / Math.atan(0.4 / power));
        // Preserve the recovered angular sampling, including its two complete azimuth passes.
        for (int azimuth = 0; azimuth < 2 * angles; azimuth++)
            for (int polar = 0; polar < angles; polar++) {
                double phi = 2 * Math.PI * azimuth / angles, theta = Math.PI * polar / angles;
                double dx = Math.sin(theta) * Math.cos(phi),
                        dy = Math.cos(theta),
                        dz = Math.sin(theta) * Math.sin(phi);
                double px = x, py = y, pz = z, remaining = power;
                for (int step = 0; remaining >= 0.5; step++) {
                    var pos =
                            new Position(
                                    (int) Math.floor(px),
                                    (int) Math.floor(py),
                                    (int) Math.floor(pz));
                    var cell = cache.computeIfAbsent(pos, terrain);
                    if (cell.absorption() < 0 || cell.absorption() > remaining) break;
                    if (cell.solid()) blocks.merge(pos, remaining > 8, (a, b) -> a || b);
                    if (azimuth % 8 == 0 && polar % 8 == 0 && (step + 4) % 8 == 0)
                        samples.add(new Sample(px, py, pz, remaining, step));
                    remaining -= cell.absorption();
                    px += dx;
                    py += dy;
                    pz += dz;
                }
            }
        return new Result(blocks, samples);
    }

    private HeatBlast() {}
}
