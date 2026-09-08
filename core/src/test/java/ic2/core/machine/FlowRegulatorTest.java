package ic2.core.machine;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class FlowRegulatorTest {
    @Test
    void settingsClampAndRejectUnknownActions() {
        var regulator = new FlowRegulator();
        assertFalse(regulator.ready(0, 0));
        assertTrue(regulator.configure(3));
        regulator.configure(0);
        assertEquals(1000, regulator.state().amount());
        regulator.configure(7);
        regulator.configure(4);
        assertEquals(0, regulator.state().amount());
        assertFalse(regulator.configure(Integer.MAX_VALUE));
        assertFalse(regulator.configure(-1));
    }

    @Test
    void fixedCadenceAndReloadGuardSurviveModeChanges() {
        var regulator = new FlowRegulator();
        regulator.configure(2);
        int count = 0;
        for (int tick = 0; tick < 60; tick++)
            if (regulator.ready(tick, 7)) {
                regulator.delivered(tick);
                count++;
            }
        assertEquals(3, count);
        var restored = new FlowRegulator();
        restored.restore(regulator.state());
        restored.configure(8);
        assertFalse(restored.ready(47, 7));
        assertTrue(restored.ready(48, 7));
        restored.configure(9);
        assertFalse(restored.ready(48, 7));
        assertTrue(restored.ready(67, 7));
    }
}
