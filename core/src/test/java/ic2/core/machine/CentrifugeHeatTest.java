package ic2.core.machine;

import static org.junit.jupiter.api.Assertions.*;

import ic2.core.energy.EnergyStore;

import org.junit.jupiter.api.Test;

import java.util.OptionalInt;

class CentrifugeHeatTest {
    @Test
    void recipeHeatingAndUnpoweredCooling() {
        var heat = new CentrifugeHeat();
        var energy = new EnergyStore(5000);
        energy.insert(2000);
        for (int i = 0; i < 1500; i++) heat.tick(OptionalInt.of(1500), false, energy);
        assertEquals(1500, heat.heat());
        assertEquals(500, energy.stored());
        heat.tick(OptionalInt.of(1500), false, energy);
        assertEquals(1501, heat.heat());
        heat.tick(OptionalInt.of(1000), false, energy);
        assertEquals(1001, heat.heat());
        energy.extract(energy.stored());
        heat.tick(OptionalInt.of(1000), false, energy);
        assertEquals(1000, heat.heat());
    }

    @Test
    void redstonePreheatAndBoundedLegacyOscillation() {
        var heat = new CentrifugeHeat();
        var energy = new EnergyStore(6000);
        energy.insert(6000);
        for (int i = 0; i < 5001; i++) heat.tick(OptionalInt.empty(), true, energy);
        assertEquals(5001, heat.heat());
        heat.tick(OptionalInt.empty(), true, energy);
        assertEquals(5000, heat.heat());
        assertEquals(999, energy.stored());
        heat.tick(OptionalInt.empty(), false, energy);
        assertEquals(4999, heat.heat());
        assertEquals(999, energy.stored());
        assertThrows(IllegalArgumentException.class, () -> heat.restore(5002));
    }

    @Test
    void mediumVoltageBaseAndAuxiliaryLoad() {
        var base = new UpgradeProfile.Base(500, 48, 24000, 2, 1);
        var standard = UpgradeProfile.calculate(base, 0, 0, 0);
        assertEquals(128, standard.classicVoltage());
        assertEquals(128, standard.workingVoltage());
        assertEquals(2, standard.itemTier());
        assertEquals(48000, standard.capacity());
        assertEquals(512, UpgradeProfile.calculate(base, 0, 1, 0).classicVoltage());
        // The extra EU crosses an exact voltage boundary and must affect packet selection.
        var boundary =
                UpgradeProfile.calculate(new UpgradeProfile.Base(100, 32, 3200, 1, 1), 0, 0, 0);
        assertEquals(128, boundary.workingVoltage());
    }
}
