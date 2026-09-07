package ic2.core.machine;

import static org.junit.jupiter.api.Assertions.*;

import ic2.core.energy.EnergyStore;

import org.junit.jupiter.api.Test;

class WaterMillTest {
    @Test
    void bucketAndConsumableBatchPower() {
        for (boolean returned : new boolean[] {false, true}) {
            var mill = new WaterMill();
            var energy = new EnergyStore(2000);
            mill.acceptContainer(returned);
            for (int i = 0; i < 500; i++) assertTrue(mill.tick(energy));
            assertFalse(mill.tick(energy));
            assertEquals(returned ? 500 : 1000, energy.stored());
        }
    }

    @Test
    void ambientMultiplierDoesNotMutateTheWaterCount() {
        var mill = new WaterMill();
        var energy = new EnergyStore(1000);
        for (int i = 0; i < 100; i++) {
            mill.prepareAmbient(25, 2);
            mill.tick(energy);
        }
        assertEquals(50, energy.stored(), .000001);
    }

    @Test
    void fullBuffersPreserveLegacyFuelConsumptionAndBatchPowerReloads() {
        var mill = new WaterMill();
        var energy = new EnergyStore(4);
        energy.insert(4);
        mill.acceptContainer(true);
        mill.tick(energy);
        assertEquals(new WaterMill.State(499, 1), mill.state());
        var restored = new WaterMill();
        restored.restore(mill.state());
        energy.extract(4);
        restored.tick(energy);
        assertEquals(1, energy.stored());
    }
}
