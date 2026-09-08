package ic2.core.machine;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class FermentationCycleTest {
    private static final FermentationCycle.Batch DEFAULT =
            new FermentationCycle.Batch(20, 4000, 500);

    @Test
    void partialHeatIsPaidOnceAndReloadable() {
        var cycle = new FermentationCycle();
        cycle.addHeat(2000, DEFAULT);
        assertFalse(cycle.complete(DEFAULT));
        var restored = new FermentationCycle();
        restored.restore(cycle.state());
        assertEquals(2000, restored.addHeat(5000, DEFAULT));
        assertTrue(restored.complete(DEFAULT));
        assertEquals(new FermentationCycle.State(0, 20), restored.state());
        assertFalse(restored.complete(DEFAULT));
    }

    @Test
    void fertilizerCarriesTheRemainderAcrossNonDivisibleBatches() {
        var cycle = new FermentationCycle();
        var batch = new FermentationCycle.Batch(300, 1, 500);
        cycle.addHeat(1, batch);
        cycle.complete(batch);
        assertEquals(1, cycle.fertilizer(batch));
        cycle.addHeat(1, batch);
        cycle.complete(batch);
        assertEquals(100, cycle.state().processed());
        assertEquals(0, cycle.fertilizer(batch));
    }

    @Test
    void largeRecipeValuesDoNotOverflowOrCreateFreeHeat() {
        var cycle = new FermentationCycle();
        var batch =
                new FermentationCycle.Batch(
                        Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
        cycle.restore(new FermentationCycle.State(Integer.MAX_VALUE - 1, Integer.MAX_VALUE - 1));
        assertEquals(1, cycle.addHeat(Integer.MAX_VALUE, batch));
        assertEquals(1, cycle.fertilizer(batch));
        assertTrue(cycle.complete(batch));
        assertEquals(new FermentationCycle.State(0, Integer.MAX_VALUE - 1), cycle.state());
        assertThrows(IllegalArgumentException.class, () -> new FermentationCycle.Batch(20, 0, 500));
        assertThrows(
                IllegalArgumentException.class, () -> new FermentationCycle.Batch(20, 4000, 0));
    }
}
