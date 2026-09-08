package ic2.core.machine;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class WaterFlowTest {
    private RotorOperation ocean(WaterFlow.Body body, long time, int distance, int obstruction) {
        return new WaterFlow(body, time, distance, false, obstruction, 0)
                .operation(RotorMaterial.IRON, 1);
    }

    @Test
    void tidesUseShoreDistanceAndSquaredOcclusion() {
        var peak = ocean(WaterFlow.Body.OCEAN, 3000, 100, 0);
        assertEquals(300, peak.output());
        assertEquals(5, peak.degreesPerTick());
        assertEquals(150, ocean(WaterFlow.Body.OCEAN, 3000, 50, 0).output());
        assertTrue(ocean(WaterFlow.Body.OCEAN, 3000, 100, 85).output() < 225);
        assertEquals(0, ocean(WaterFlow.Body.OCEAN, 3000, 100, 169).output());
        assertEquals(0, ocean(WaterFlow.Body.OCEAN, 0, 100, 0).output());
        var ebb = ocean(WaterFlow.Body.OCEAN, 9000, 100, 0);
        assertEquals(peak.output(), ebb.output());
        assertEquals(-peak.degreesPerTick(), ebb.degreesPerTick());
        assertEquals(peak.output(), ocean(WaterFlow.Body.OCEAN, 243000, 100, 0).output());
    }

    @Test
    void deepOceanUsesFullDiameterAndDistinctPowerAndWear() {
        var deep = ocean(WaterFlow.Body.DEEP_OCEAN, 3000, 100, 0);
        assertEquals(400, deep.output());
        assertEquals(7, deep.diameter());
        assertEquals(3, deep.wear());
        assertEquals(2, ocean(WaterFlow.Body.OCEAN, 3000, 100, 0).wear());
    }

    @Test
    void riversReduceDiameterAndBoundTurbulenceWithoutChangingWorkSign() {
        var fast =
                new WaterFlow(WaterFlow.Body.RIVER, 0, 50, false, 0, 0)
                        .operation(RotorMaterial.IRON, 1);
        var slow =
                new WaterFlow(WaterFlow.Body.RIVER, 0, 1, true, 0, 1)
                        .operation(RotorMaterial.IRON, 1);
        assertEquals(100, fast.output());
        assertEquals(28, slow.output());
        assertEquals(5, fast.diameter());
        assertEquals(-2, slow.degreesPerTick());
        assertEquals(1, slow.wear());
    }

    @Test
    void unavailableWaterNeverProducesWork() {
        var conditions = new WaterFlow(WaterFlow.Body.RIVER, 0, 50, false, 0, 0);
        assertEquals(0, conditions.operation(RotorMaterial.IRON, 0).wear());
        assertEquals(200, conditions.operation(RotorMaterial.IRON, 2).output());
        assertThrows(
                IllegalArgumentException.class,
                () -> conditions.operation(RotorMaterial.WOODEN, 1));
        assertEquals(
                RotorOperation.Status.INVALID_BIOME,
                new WaterFlow(WaterFlow.Body.INVALID, 0, 50, false, 0, 0)
                        .operation(RotorMaterial.IRON, 1)
                        .status());
        assertEquals(
                RotorOperation.Status.INTERFERENCE,
                new WaterFlow(WaterFlow.Body.RIVER, 0, 50, false, -1, 0)
                        .operation(RotorMaterial.IRON, 1)
                        .status());
    }
}
