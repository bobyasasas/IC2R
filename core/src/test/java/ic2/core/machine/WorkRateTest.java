package ic2.core.machine;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class WorkRateTest {
    @Test
    void unusedFlowExpiresAndRepeatedCallsShareOneTick() {
        var flow = new WorkRate();
        assertEquals(100, flow.available(1, 100));
        assertEquals(100, flow.available(10, 100));
        assertEquals(70, flow.extract(10, 100, 70));
        assertEquals(30, flow.extract(10, 100, 100));
        assertEquals(0, flow.available(10, 100));
        assertEquals(100, flow.available(11, 100));
    }

    @Test
    void reloadAndRateChangesCannotResetAlreadyUsedProduction() {
        var flow = new WorkRate();
        flow.extract(1, 100, 70);
        var restored = new WorkRate();
        restored.restore(flow.state());
        assertEquals(30, restored.available(1, 100));
        assertEquals(0, restored.available(1, 50));
        assertEquals(130, restored.available(1, 200));
        assertThrows(IllegalArgumentException.class, () -> restored.extract(1, 100, -1));
    }
}
