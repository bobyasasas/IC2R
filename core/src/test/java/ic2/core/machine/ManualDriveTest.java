package ic2.core.machine;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ManualDriveTest {
    @Test
    void humanAndSimulatedPlayersHaveDifferentOutputAndShareACappedReservoir() {
        var drive = new ManualDrive();
        var work = new WorkBuffer(1000);
        assertEquals(new ManualDrive.Result(true, 400), drive.click(1, 20, false, 1, work));
        assertEquals(new ManualDrive.Result(true, 20), drive.click(1, 20, true, 1, work));
        assertEquals(400, drive.click(1, 20, false, 1, work).added());
        assertEquals(180, drive.click(1, 20, false, 1, work).added());
        assertEquals(1000, work.state().stored());
        assertEquals(new ManualDrive.Result(true, 0), drive.click(1, 20, false, 1, work));
    }

    @Test
    void reloadDoesNotResetThePerTickClickBudget() {
        var drive = new ManualDrive();
        var work = new WorkBuffer(1000);
        for (int i = 0; i < 10; i++) assertTrue(drive.click(5, 20, true, 1, work).accepted());
        var restored = new ManualDrive();
        restored.restore(drive.state());
        assertFalse(restored.click(5, 20, true, 1, work).accepted());
        assertTrue(restored.click(6, 20, true, 1, work).accepted());
        assertEquals(220, work.state().stored());
    }

    @Test
    void hungerAndDisabledOrFractionalMultipliersAreAppliedBeforeAcceptingClicks() {
        var drive = new ManualDrive();
        var work = new WorkBuffer(1000);
        assertFalse(drive.click(1, 6, false, 1, work).accepted());
        assertFalse(drive.click(1, 20, false, 0, work).accepted());
        assertEquals(200, drive.click(1, 7, false, .5, work).added());
        assertEquals(10, drive.click(1, 7, true, .5, work).added());
        assertEquals(2, drive.state().clicks());
    }
}
