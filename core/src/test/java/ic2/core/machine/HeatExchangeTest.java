package ic2.core.machine;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class HeatExchangeTest {
    @Test
    void nearlyFullOutputNeverMultipliesTheAmountDrained() {
        assertEquals(new HeatExchange(1, 20), HeatExchange.plan(2000, 1, 100, 20));
        assertEquals(new HeatExchange(0, 0), HeatExchange.plan(2000, 0, 100, 20));
    }

    @Test
    void wholeMillibucketsRequireEnoughConductorsAndRespectEveryLimit() {
        assertEquals(new HeatExchange(0, 0), HeatExchange.plan(2000, 2000, 10, 20));
        assertEquals(new HeatExchange(5, 100), HeatExchange.plan(2000, 2000, 100, 20));
        assertEquals(new HeatExchange(2, 40), HeatExchange.plan(2, 2000, 100, 20));
        assertEquals(
                new HeatExchange(1, Integer.MAX_VALUE),
                HeatExchange.plan(
                        Integer.MAX_VALUE,
                        Integer.MAX_VALUE,
                        Integer.MAX_VALUE,
                        Integer.MAX_VALUE));
        assertThrows(IllegalArgumentException.class, () -> HeatExchange.plan(1, 1, 100, 0));
    }
}
