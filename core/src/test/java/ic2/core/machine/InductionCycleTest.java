package ic2.core.machine;

import static org.junit.jupiter.api.Assertions.*;

import ic2.core.energy.EnergyStore;

import org.junit.jupiter.api.Test;

class InductionCycleTest {
    @Test
    void coldFurnacePaysForHeatingAndProcessing() {
        var cycle = new InductionCycle();
        var energy = new EnergyStore(1000);
        energy.insert(1000);
        for (int i = 0; i < 29; i++) cycle.tick(true, false, energy);
        assertEquals(new InductionCycle.State(29, 0), cycle.state());
        cycle.tick(true, false, energy);
        assertEquals(new InductionCycle.State(30, 1), cycle.state());
        assertEquals(520, energy.stored());
    }

    @Test
    void insufficientEnergyCannotAdvanceWork() {
        var cycle = new InductionCycle();
        cycle.restore(new InductionCycle.State(9000, 0));
        var energy = new EnergyStore(100);
        energy.insert(15);
        assertFalse(cycle.tick(true, false, energy));
        assertEquals(new InductionCycle.State(9001, 0), cycle.state());
        assertEquals(14, energy.stored());
    }

    @Test
    void hotBatchFinishesAtNextTickAndIdleCools() {
        var cycle = new InductionCycle();
        cycle.restore(new InductionCycle.State(10000, 0));
        var energy = new EnergyStore(1000);
        energy.insert(1000);
        for (int i = 0; i < 12; i++) cycle.tick(true, false, energy);
        assertFalse(cycle.completionReady());
        cycle.tick(true, false, energy);
        assertTrue(cycle.completionReady());
        assertEquals(792, energy.stored());
        cycle.completed();
        cycle.tick(false, false, energy);
        assertEquals(new InductionCycle.State(9996, 0), cycle.state());
        cycle.tick(false, true, energy);
        assertEquals(new InductionCycle.State(9997, 0), cycle.state());
        assertEquals(791, energy.stored());
    }
}
