package ic2.core.machine;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class RadioisotopeOutputTest {
    @Test
    void pelletsDoubleTheWholeMachine() {
        assertEquals(0, RadioisotopeOutput.output(0, 2.0), "No pellets means no output");
        assertEquals(2, RadioisotopeOutput.output(1, 2.0));
        assertEquals(4, RadioisotopeOutput.output(2, 2.0));
        assertEquals(8, RadioisotopeOutput.output(3, 2.0));
        assertEquals(16, RadioisotopeOutput.output(4, 2.0));
        assertEquals(32, RadioisotopeOutput.output(5, 2.0));
        assertEquals(64, RadioisotopeOutput.output(6, 2.0));
        assertEquals(1, RadioisotopeOutput.output(1, 1.0));
        assertEquals(32, RadioisotopeOutput.output(6, 1.0));
    }

    @Test
    void invalidRequestsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> RadioisotopeOutput.output(-1, 1.0));
        assertThrows(IllegalArgumentException.class, () -> RadioisotopeOutput.output(1, -0.5));
        assertThrows(
                IllegalArgumentException.class, () -> RadioisotopeOutput.output(1, Double.NaN));
        assertThrows(
                IllegalArgumentException.class,
                () -> RadioisotopeOutput.output(1, Double.POSITIVE_INFINITY));
        assertEquals(0, RadioisotopeOutput.output(3, 0.0), "A zero multiplier stops output");
    }

    @Test
    void extremeMultipliersScaleWithoutWrapping() {
        assertEquals(
                32000000,
                RadioisotopeOutput.output(6, 1000000.0),
                "The configured ceiling must stay a positive int");
    }
}
