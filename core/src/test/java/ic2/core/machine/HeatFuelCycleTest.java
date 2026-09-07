package ic2.core.machine;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class HeatFuelCycleTest {
    @Test
    void solidFuelBurnsIntoASavedReserveWhileTheOutputIsFull() {
        var fuel = new HeatFuelCycle();
        var work = new WorkBuffer(20);
        fuel.acceptSolid(400, 20);
        int finished = 0;
        for (int i = 0; i < 400; i++) {
            fuel.transferTo(work, 20);
            if (fuel.burn()) finished++;
            if (i == 173) {
                var restored = new HeatFuelCycle();
                restored.restore(fuel.state());
                fuel = restored;
            }
        }
        assertEquals(1, finished);
        assertEquals(7980, fuel.state().reserve());
        assertEquals(20, work.state().stored());
        assertFalse(fuel.idle());
        long delivered = 0;
        for (int tick = 0; tick < 401; tick++) {
            fuel.transferTo(work, 20);
            delivered += work.extract(tick, 20, 20);
        }
        assertEquals(8000, delivered);
        assertTrue(fuel.idle());
    }

    @Test
    void prepaidFluidHeatSurvivesPartialDemandAndReload() {
        var fuel = new HeatFuelCycle();
        var work = new WorkBuffer(32);
        fuel.acceptFluid(20, 32);
        long delivered = 0;
        for (int tick = 0; tick < 200; tick++) {
            fuel.transferTo(work, 32);
            delivered += work.extract(tick, 32, 7);
            if (tick == 11) {
                var restored = new HeatFuelCycle();
                restored.restore(fuel.state());
                fuel = restored;
            }
        }
        assertEquals(640, delivered);
        assertEquals(0, fuel.state().reserve());
        assertEquals(0, work.state().stored());
    }

    @Test
    void heatReserveUsesLongArithmeticAndCannotAcceptOverlappingBatches() {
        var fuel = new HeatFuelCycle();
        fuel.acceptFluid(4000, 32000000);
        assertEquals(128000000000L, fuel.state().reserve());
        assertThrows(IllegalStateException.class, () -> fuel.acceptSolid(1, 20));
        assertThrows(IllegalArgumentException.class, () -> new HeatFuelCycle.State(1, 1, 0, 0));
    }
}
