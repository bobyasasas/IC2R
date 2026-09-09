package ic2.core.machine;

/** Radioisotope fuel scales exponentially: n pellets emit 2^(n-1) base units, zero on empty. */
public final class RadioisotopeOutput {
    public static final int SLOTS = 6;

    public static int output(int installed, double perUnit) {
        if (installed < 0 || !Double.isFinite(perUnit) || perUnit < 0)
            throw new IllegalArgumentException("Invalid radioisotope output request");
        if (installed == 0 || perUnit == 0) return 0;
        if (installed > SLOTS) installed = SLOTS;
        double value = Math.pow(2.0, installed - 1) * perUnit;
        return value >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
    }

    private RadioisotopeOutput() {}
}
