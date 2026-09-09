package ic2.core.machine;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class RepressurizationTest {
    @Test
    void batchesConserveInputHeatAndOutput() {
        var state = new Repressurization(0);
        for (int i = 0; i < 500; i++) {
            int rate = i % 7 == 0 ? 32 : 16;
            var step = state.process(i % 9973, 10000 - (i % 801), rate);
            assertEquals(step.heat() * 10, step.input(), "Every batch trades 10 mB per HU");
            assertEquals(step.heat() * rate, step.output(), "Every batch emits exactly the rate");
            state = step.next();
        }
    }

    @Test
    void wholeTicksNeverExceedSteamHeatOrSpace() {
        var full = new Repressurization(1000).process(10000, 20000, 16);
        assertEquals(1000, full.heat());
        assertEquals(10000, full.input());
        assertEquals(16000, full.output());
        assertEquals(0, full.next().reserve());
        var crammed = new Repressurization(1000).process(10000, 10000, 16);
        assertEquals(625, crammed.heat(), "Only as many batches fit the output space");
        assertEquals(6250, crammed.input());
        assertEquals(10000, crammed.output());
        assertEquals(375, crammed.next().reserve());
        var cramped = new Repressurization(1000).process(10000, 15, 16);
        assertEquals(0, cramped.input(), "A full batch needs the whole output space");
        var partial = new Repressurization(1000).process(25, 10000, 16);
        assertEquals(20, partial.input());
        assertEquals(32, partial.output());
        assertEquals(2, partial.heat());
    }

    @Test
    void zeroRateStopsInsteadOfBurningInput() {
        var state = new Repressurization(500);
        var step = state.process(10000, 10000, 0);
        assertEquals(new Repressurization.Step(state, 0, 0, 0), step);
    }

    @Test
    void heatRequestExcludesStoredReserveAndIsBounded() {
        assertEquals(1000, new Repressurization(0).heatRequest(10000));
        assertEquals(600, new Repressurization(400).heatRequest(10000));
        assertEquals(0, new Repressurization(1000).heatRequest(10000));
        assertEquals(2, new Repressurization(0).heatRequest(25));
        assertEquals(0, new Repressurization(3).heatRequest(25));
        assertEquals(0, new Repressurization(0).heatRequest(0));
        assertEquals(0, new Repressurization(0).heatRequest(9));
        assertEquals(1000, new Repressurization(0).absorb(5000));
        assertEquals(700, new Repressurization(300).absorb(400));
        assertThrows(IllegalArgumentException.class, () -> new Repressurization(-1));
        assertThrows(IllegalArgumentException.class, () -> new Repressurization(1001));
        assertThrows(IllegalArgumentException.class, () -> new Repressurization(0).absorb(-1));
        assertThrows(
                IllegalArgumentException.class, () -> new Repressurization(0).process(-1, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> new Repressurization(0).heatRequest(-1));
    }

    @Test
    void prePaidHeatSurvivesBlockedOutput() {
        var state = new Repressurization(0);
        state = new Repressurization(state.absorb(7));
        var blocked = state.process(1000, 15, 16);
        assertEquals(7, blocked.next().reserve(), "Blocked output neither spends nor loses heat");
        var open = state.process(1000, 10000, 32);
        assertEquals(7, open.heat());
        assertEquals(0, open.next().reserve());
        assertEquals(224, open.output());
    }
}
