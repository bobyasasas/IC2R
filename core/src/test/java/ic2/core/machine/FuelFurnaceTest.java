package ic2.core.machine;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class FuelFurnaceTest {
    @Test
    void completesAfter160TicksAndKeepsBurningWhenBlocked() {
        var furnace = new FuelFurnace();
        assertFalse(furnace.needsFuel(false));
        assertTrue(furnace.needsFuel(true));
        furnace.acceptFuel(1600);
        for (int tick = 0; tick < 159; tick++) assertFalse(furnace.tick(true).completed());
        assertTrue(furnace.tick(true).completed());
        assertEquals(new FuelFurnace.State(1440, 1600, 0), furnace.state());
        assertTrue(furnace.tick(false).burning());
        assertEquals(1439, furnace.state().fuel());
    }

    @Test
    void InterruptionResetsProgressAndReloadPreservesFuel() {
        var furnace = new FuelFurnace();
        furnace.acceptFuel(100);
        for (int tick = 0; tick < 50; tick++) furnace.tick(true);
        var restored = new FuelFurnace();
        restored.restore(furnace.state());
        assertEquals(50, restored.state().progress());
        restored.tick(false);
        assertEquals(new FuelFurnace.State(49, 100, 0), restored.state());
    }

    @Test
    void rejectsInvalidPersistedStateAndFuelReplacement() {
        assertThrows(IllegalArgumentException.class, () -> new FuelFurnace.State(10, 9, 0));
        assertThrows(IllegalArgumentException.class, () -> new FuelFurnace.State(0, 0, 160));
        var furnace = new FuelFurnace();
        assertThrows(IllegalArgumentException.class, () -> furnace.acceptFuel(0));
        furnace.acceptFuel(1);
        assertThrows(IllegalArgumentException.class, () -> furnace.acceptFuel(2));
        assertTrue(furnace.tick(true).burning());
        assertFalse(furnace.tick(true).burning());
        assertEquals(0, furnace.state().progress());
    }
}
