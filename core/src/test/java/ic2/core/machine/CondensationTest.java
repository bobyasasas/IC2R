package ic2.core.machine;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CondensationTest {
    @Test
    void batchesCarryRemainderAndConserveSteam() {
        var state = new Condensation(0);
        long steam = 0, water = 0;
        for (int i = 0; i < 1000; i++) {
            var step = state.plan(i % 503, 1000, i % 5, 8);
            steam += step.steam();
            water += step.water();
            state = step.next();
            assertEquals(steam, water * 100 + state.steamCredit());
        }
        var edge = new Condensation(9999).plan(500, 100, 4, 8);
        assertEquals(10499, edge.next().steamCredit());
        assertEquals(499, edge.next().plan(0, 100, 4, 0).next().steamCredit());
    }

    @Test
    void blockedOutputAndPoweredVentsPreservePaidCredit() {
        var state = new Condensation(10000);
        assertEquals(new Condensation.Step(state, 0, 0, 0), state.plan(500, 99, 4, 8));
        var unpaid = state.plan(500, 100, 4, 7);
        assertEquals(100, unpaid.water());
        assertEquals(0, unpaid.steam());
        assertEquals(0, unpaid.energy());
        assertEquals(100, new Condensation(0).plan(500, 100, 0, 0).steam());
        assertEquals(8, new Condensation(0).plan(1, 100, 4, 8).energy());
    }
}
