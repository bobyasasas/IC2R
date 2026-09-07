package ic2.core.machine;

import static org.junit.jupiter.api.Assertions.*;

import ic2.core.energy.EnergyStore;

import org.junit.jupiter.api.Test;

class WorkBufferTest {
    @Test
    void conversionConservesEnergyAndStopsAtTheTarget() {
        var energy = new EnergyStore(1000);
        energy.insert(1000);
        var work = new WorkBuffer(1000);
        assertEquals(1000, work.generate(energy, 4, 1000, false));
        assertEquals(750, energy.stored());
        assertEquals(0, work.generate(energy, 4, 1000, false));
        assertEquals(100, work.extract(1, 100, 1000));
        assertEquals(100, work.generate(energy, 4, 1000, false));
        assertEquals(0, work.extract(1, 100, 1000));
        assertEquals(100, work.extract(2, 100, 1000));
        assertEquals(1000, energy.stored() + (work.state().stored() + 200) / 4);
    }

    @Test
    void heatMultiplierChargesTheActualConvertedUnits() {
        var energy = new EnergyStore(100);
        energy.insert(5);
        var work = new WorkBuffer(100);
        assertEquals(10, work.generate(energy, 2, 100, true));
        assertEquals(0, energy.stored());
        energy.insert(.25);
        assertEquals(0, work.generate(energy, 2, 100, true));
        assertEquals(.25, energy.stored());
    }

    @Test
    void reloadRetainsFractionalKineticEnergyAndTheCurrentTickBudget() {
        var energy = new EnergyStore(100);
        energy.insert(.625);
        var work = new WorkBuffer(1000);
        work.generate(energy, 4, 1000, false);
        assertEquals(2, work.extract(10, 2, 10));
        var restored = new WorkBuffer(1000);
        restored.restore(work.state());
        assertEquals(.5, restored.state().stored());
        energy.insert(10);
        restored.generate(energy, 4, 1000, false);
        assertEquals(0, restored.available(10, 2));
        assertEquals(2, restored.available(11, 2));
    }

    @Test
    void invalidRequestsAndDisabledConversionCannotMutateEnergy() {
        var energy = new EnergyStore(100);
        energy.insert(100);
        var work = new WorkBuffer(100);
        assertEquals(0, work.generate(energy, 0, 100, true));
        assertThrows(IllegalArgumentException.class, () -> work.extract(1, 100, -1));
        assertThrows(
                IllegalArgumentException.class, () -> work.generate(energy, Double.NaN, 100, true));
        assertEquals(100, energy.stored());
    }
}
