package ic2.core.reactor;

/** Pure reactor pulse maths shared by every fuel rod flavour. */
public final class ReactorMath {
    public static int triangularNumber(int x) {
        return (x * x + x) / 2;
    }

    /** Legacy uranium heat per component pass: triangular pulse count, four heat per step. */
    public static int uraniumHeat(int pulses) {
        return triangularNumber(pulses) * 4;
    }

    private ReactorMath() {}
}
