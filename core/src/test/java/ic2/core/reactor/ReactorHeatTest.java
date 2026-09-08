package ic2.core.reactor;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ReactorHeatTest {
    @Test
    void exchangeConservesHeatAndReportsOverflowWithTheCorrectSign() {
        for (int stored : new int[] {0, 1, 999, 1000})
            for (int request :
                    new int[] {Integer.MIN_VALUE, -1001, -6, 0, 6, 1001, Integer.MAX_VALUE}) {
                var result = new ReactorHeat(1000, stored).exchange(request);
                assertEquals(
                        (long) stored + request,
                        (long) result.heat().stored() + result.remainder());
                assertTrue(result.heat().stored() >= 0 && result.heat().stored() <= 1000);
            }
        assertEquals(1, new ReactorHeat(1000, 999).exchange(2).remainder());
        assertEquals(-5, new ReactorHeat(1000, 1).exchange(-6).remainder());
    }
}
