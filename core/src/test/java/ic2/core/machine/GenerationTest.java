package ic2.core.machine;

import static org.junit.jupiter.api.Assertions.*;

import ic2.core.energy.EnergyStore;

import org.junit.jupiter.api.Test;

class GenerationTest {
    @Test
    void solarFollowsLightWeatherAndDimension() {
        assertEquals(1, SolarGeneration.brightness(true, 15, 0, false, 0, 0));
        assertEquals(0, SolarGeneration.brightness(true, 15, 180, false, 0, 0));
        assertEquals(0, SolarGeneration.brightness(false, 15, 0, false, 0, 0));
        assertEquals(7.0 / 15, SolarGeneration.brightness(true, 7, 0, false, 0, 0));
        assertEquals(121.0 / 256, SolarGeneration.brightness(true, 15, 0, false, 1, 1));
        assertEquals(1, SolarGeneration.brightness(true, 15, 0, true, 1, 1));
        assertEquals(0, SolarGeneration.brightness(true, 0, 0, false, 0, 0));
    }

    @Test
    void meteredBatchPausesAndResumesWithoutLoss() {
        var energy = new EnergyStore(160);
        var burner = new FluidFuelBurner();
        var biogas = new FluidFuelBurner.Fuel(10, 10, 16);
        burner.accept(biogas);
        for (int tick = 0; tick < 5; tick++) assertTrue(burner.tick(energy));
        var restored = new FluidFuelBurner();
        restored.restore(burner.state());
        energy.insert(80);
        assertFalse(restored.tick(energy));
        assertEquals(5, restored.state().remaining());
        energy.extract(160);
        for (int tick = 0; tick < 5; tick++) assertTrue(restored.tick(energy));
        assertEquals(80, energy.stored());
        assertFalse(restored.tick(energy));
        assertTrue(restored.needsFuel(energy, biogas));
    }
}
