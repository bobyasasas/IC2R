package ic2.core.energy;

import static org.junit.jupiter.api.Assertions.*;

import ic2.core.energy.grid.EnergyMode;

import org.junit.jupiter.api.Test;

class EnergyDevicePolicyTest {
    @Test
    void signalAndOverflowHaveDistinctThresholds() {
        double threshold = 40000 - 32 * 20;
        assertTrue(StorageRedstoneMode.EMIT_NEAR_FULL.emitsSignal(threshold, 40000, 32));
        assertFalse(
                StorageRedstoneMode.OVERFLOW_WHEN_POWERED.emitsEnergy(true, threshold, 40000, 32));
        assertTrue(
                StorageRedstoneMode.OVERFLOW_WHEN_POWERED.emitsEnergy(
                        true, threshold + 1, 40000, 32));
        assertTrue(StorageRedstoneMode.OVERFLOW_WHEN_POWERED.emitsEnergy(false, 0, 40000, 32));
        assertFalse(StorageRedstoneMode.STOP_WHEN_POWERED.emitsEnergy(true, 40000, 40000, 32));
        assertFalse(StorageRedstoneMode.EMIT_EMPTY.emitsSignal(32, 40000, 32));
        assertTrue(StorageRedstoneMode.EMIT_EMPTY.emitsSignal(31, 40000, 32));
        assertFalse(StorageRedstoneMode.EMIT_PARTIAL.emitsSignal(32, 40000, 32));
        assertFalse(StorageRedstoneMode.EMIT_NOT_FULL.emitsSignal(39968, 40000, 32));
    }

    @Test
    void transformerOnlyFaultsOnChargedGtDirectionChange() {
        assertTrue(TransformerMode.unsafeSwitch(EnergyMode.GT, false, true, .01));
        assertFalse(TransformerMode.unsafeSwitch(EnergyMode.GT, false, true, 0));
        assertFalse(TransformerMode.unsafeSwitch(EnergyMode.IC2, false, true, 256));
        assertFalse(TransformerMode.unsafeSwitch(EnergyMode.GT, true, true, 256));
        assertTrue(TransformerMode.REDSTONE.stepUp(true));
        assertFalse(TransformerMode.STEP_DOWN.stepUp(true));
        assertEquals(TransformerMode.REDSTONE, TransformerMode.fromSavedId(-1));
        assertEquals(StorageRedstoneMode.IGNORE, StorageRedstoneMode.fromSavedId(7));
    }
}
