package ic2.core.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SiStringTest {
    @Test
    void degenerateValuesKeepLegacySentinels() {
        assertEquals("0 ", SiString.format(0.0, 4));
        assertEquals("NaN ", SiString.format(Double.NaN, 4));
        assertEquals("∞ ", SiString.format(Double.POSITIVE_INFINITY, 4));
        assertEquals("-∞ ", SiString.format(Double.NEGATIVE_INFINITY, 4));
    }

    @Test
    void integerScalesCarryNoDecimalPart() {
        assertEquals("3 ", SiString.format(3.0, 4));
        assertEquals("-3 ", SiString.format(-3.0, 4));
        assertEquals("1 ", SiString.format(1.0, 4));
    }

    @Test
    void scalesIntoStandardPrefixes() {
        assertEquals("1.4 k", SiString.format(1400, 4));
        assertEquals("-1.4 k", SiString.format(-1400, 4));
        assertEquals("2.5 M", SiString.format(2.5e6, 4));
        assertEquals("140 µ", SiString.format(0.00014, 4));
        assertEquals("1.4 p", SiString.format(1.4e-12, 4));
    }

    @Test
    void beyondThePrefixTableFallsBackToExponents() {
        assertEquals("1.4 E30", SiString.format(1.4e30, 4));
        assertEquals("1.4 E-30", SiString.format(1.4e-30, 4));
    }

    @Test
    void fractionDigitsRoundAndStripTrailingZeroes() {
        assertEquals("1.343 ", SiString.format(1.342943, 4));
        assertEquals("1.4 k", SiString.format(1400.0, 4));
        assertEquals("1 ", SiString.format(1.0, 4));
    }

    @Test
    void coarseDigitsRoundCarryIntoTheInteger() {
        assertEquals("3 ", SiString.format(2.5, 1));
        assertEquals("999 ", SiString.format(999.99, 2));
    }

    @Test
    void subUnitValuesScaleDownEvenJustBelowOne() {
        // Legacy quirk: 0.999 takes the negative-log branch and reads "999 m".
        assertEquals("999 m", SiString.format(0.999, 4));
    }
}
