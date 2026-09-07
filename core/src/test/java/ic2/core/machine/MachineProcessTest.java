package ic2.core.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ic2.core.energy.EnergyStore;

import org.junit.jupiter.api.Test;

class MachineProcessTest {
    private static final MachineProcess.WorkOrder IRON =
            new MachineProcess.WorkOrder("iron", 100, 3);

    @Test
    void furnaceConsumesExactly300EuAndFinishesOnTick100() {
        var energy = new EnergyStore(300);
        energy.insert(300);
        var process = new MachineProcess();
        for (int tick = 1; tick < 100; tick++) {
            assertEquals(MachineProcess.Outcome.RUNNING, process.tick(IRON, true, energy));
        }
        assertEquals(MachineProcess.Outcome.COMPLETED, process.tick(IRON, true, energy));
        assertEquals(0, process.state().progress());
        assertEquals(0, energy.stored());
    }

    @Test
    void powerLossOutputBackpressureAndPersistencePreserveProgress() {
        var energy = new EnergyStore(300);
        energy.insert(3);
        var process = new MachineProcess();
        process.tick(IRON, true, energy);
        assertEquals(MachineProcess.Outcome.PAUSED, process.tick(IRON, true, energy));
        var restored = new MachineProcess();
        restored.restore(process.state());
        energy.insert(300);
        assertEquals(MachineProcess.Outcome.PAUSED, restored.tick(IRON, false, energy));
        assertEquals(1, restored.state().progress());
        assertEquals(300, energy.stored());
        restored.tick(IRON, true, energy);
        assertEquals(2, restored.state().progress());
    }

    @Test
    void changedInputResetsProgressBeforeSpendingEnergy() {
        var energy = new EnergyStore(300);
        energy.insert(300);
        var process = new MachineProcess();
        for (int i = 0; i < 20; i++) process.tick(IRON, true, energy);
        process.tick(new MachineProcess.WorkOrder("gold", 100, 3), true, energy);
        assertEquals(1, process.state().progress());
        assertEquals(MachineProcess.Outcome.IDLE, process.tick(null, true, energy));
        assertEquals(0, process.state().progress());
    }

    @Test
    void generatorRetainsLegacyFuelConsumptionWhenItsBufferFills() {
        var energy = new EnergyStore(4000);
        var generator = new FuelGenerator();
        assertTrue(generator.acceptFuel(1600));
        for (int tick = 0; tick < 400; tick++) assertTrue(generator.tick(energy, 10));
        assertEquals(4000, energy.stored());
        assertEquals(0, generator.state().remaining());
        assertEquals(false, generator.needsFuel(energy, 10));
    }
}
