package ic2.core.machine;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class RotorOperationTest {
    @Test
    void windRangeControlsPowerSpeedAndWear() {
        var running = RotorOperation.wind(RotorMaterial.WOODEN, 40, 0, 1);
        assertEquals(100, running.output());
        assertEquals(25, running.degreesPerTick());
        assertEquals(1, running.wear());
        var overloaded = RotorOperation.wind(RotorMaterial.WOODEN, 70, 0, 1);
        assertEquals(175, overloaded.output());
        assertEquals(4, overloaded.wear());
        assertEquals(RotorOperation.Status.OVERLOADED, overloaded.status());
        var low = RotorOperation.wind(RotorMaterial.WOODEN, 9, 0, 1);
        assertEquals(0, low.output());
        assertEquals(0, low.wear());
        assertEquals(0, low.degreesPerTick());
    }

    @Test
    void interferenceToleranceAndSquaredOcclusionMatchTheRecoveredRule() {
        assertEquals(100, RotorOperation.wind(RotorMaterial.WOODEN, 40, 3, 1).output());
        assertTrue(RotorOperation.wind(RotorMaterial.WOODEN, 40, 4, 1).output() < 100);
        assertEquals(0, RotorOperation.wind(RotorMaterial.WOODEN, 40, 81, 1).output());
        assertEquals(
                RotorOperation.Status.INTERFERENCE,
                RotorOperation.wind(RotorMaterial.WOODEN, 40, -1, 1).status());
    }

    @Test
    void disabledRotorsDoNotWearAndMultipliersDoNotChangeWind() {
        var base = RotorOperation.wind(RotorMaterial.CARBON, 50, 0, 1);
        var scaled = RotorOperation.wind(RotorMaterial.CARBON, 50, 0, 2);
        assertEquals(base.output() * 2, scaled.output());
        assertEquals(base.environment(), scaled.environment());
        assertEquals(0, RotorOperation.wind(RotorMaterial.CARBON, 100, 0, 0).wear());
    }
}
