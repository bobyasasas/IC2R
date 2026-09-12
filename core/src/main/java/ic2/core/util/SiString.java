package ic2.core.util;

/**
 * Legacy {@code Util.toSiString}: human readable SI-scaled numbers ("140 µ", "1.4 k") used by
 * crystal memory tooltips and machine GUIs.
 */
public final class SiString {
    public static String format(double value, int digits) {
        if (value == 0.0) return "0 ";
        if (Double.isNaN(value)) return "NaN ";
        String ret = "";
        if (value < 0.0) {
            ret = "-";
            value = -value;
        }
        if (Double.isInfinite(value)) return ret + "∞ ";

        double log = Math.log10(value);
        double mul;
        String si;
        if (log >= 0.0) {
            int reduce = (int) Math.floor(log / 3.0);
            mul = 1.0 / Math.pow(10.0, reduce * 3);
            si = switch (reduce) {
                case 0 -> "";
                case 1 -> "k";
                case 2 -> "M";
                case 3 -> "G";
                case 4 -> "T";
                case 5 -> "P";
                case 6 -> "E";
                case 7 -> "Z";
                case 8 -> "Y";
                default -> "E" + reduce * 3;
            };
        } else {
            int expand = (int) Math.ceil(-log / 3.0);
            mul = Math.pow(10.0, expand * 3);
            si = switch (expand) {
                case 0 -> "";
                case 1 -> "m";
                case 2 -> "µ";
                case 3 -> "n";
                case 4 -> "p";
                case 5 -> "f";
                case 6 -> "a";
                case 7 -> "z";
                case 8 -> "y";
                default -> "E-" + expand * 3;
            };
        }

        value *= mul;
        int iVal = (int) Math.floor(value);
        value -= iVal;
        int iDigits = 1;
        if (iVal > 0) iDigits = 1 + (int) Math.floor(Math.log10(iVal));

        mul = Math.pow(10.0, digits - iDigits);
        int dVal = (int) Math.round(value * mul);
        if (dVal >= mul) {
            iVal++;
            dVal = (int) (dVal - mul);
            iDigits = 1;
            if (iVal > 0) iDigits = 1 + (int) Math.floor(Math.log10(iVal));
        }

        ret = ret + iVal;
        if (digits > iDigits && dVal != 0)
            ret = ret + String.format(".%0" + (digits - iDigits) + "d", dVal);
        ret = ret.replaceFirst("(\\.\\d*?)0+$", "$1");
        return ret + " " + si;
    }

    private SiString() {}
}
