package ic2.core.machine;

import static org.junit.jupiter.api.Assertions.*;

import ic2.core.energy.EnergyStore;

import org.junit.jupiter.api.Test;

class UpgradeProfileTest {
    @Test
    void recoveredFurnaceRatesAndCapacity() {
        var base = UpgradeProfile.calculate(100, 3, 300, 0, 0, 0);
        assertEquals(100, base.ticks());
        assertEquals(600, base.capacity());
        var one = UpgradeProfile.calculate(100, 3, 300, 1, 0, 1);
        assertEquals(70, one.ticks());
        assertEquals(5, one.euPerTick());
        assertEquals(10650, one.capacity());
        assertEquals(35, one.rescaleProgress(50, 100));
        var two = UpgradeProfile.calculate(100, 3, 300, 2, 1, 0);
        assertEquals(49, two.ticks());
        assertEquals(8, two.euPerTick());
        assertEquals(128, two.classicVoltage());
    }

    @Test
    void extremeStacksRemainBounded() {
        for (int count = 0; count <= 256; count++) {
            var result = UpgradeProfile.calculate(300, 2, 600, count, count, count);
            assertTrue(result.ticks() >= 1 && result.euPerTick() >= 2);
            assertTrue(result.capacity() >= result.euPerTick());
            assertTrue(result.operations() >= 1 && result.amperage() >= 1);
            assertTrue(result.workingVoltage() >= result.classicVoltage());
        }
        assertEquals(
                Integer.MAX_VALUE, UpgradeProfile.calculate(300, 2, 600, 256, 0, 0).capacity());
        assertThrows(
                IllegalArgumentException.class,
                () -> UpgradeProfile.calculate(100, 3, 300, -1, 0, 0));
    }

    @Test
    void capacityChangesCannotCreateEnergy() {
        var store = new EnergyStore(10000);
        store.insert(9000);
        store.resize(500);
        assertEquals(500, store.stored());
        store.resize(10000);
        assertEquals(500, store.stored());
        assertThrows(IllegalArgumentException.class, () -> store.resize(Double.NaN));
        assertEquals(10000, store.capacity());
    }
}
