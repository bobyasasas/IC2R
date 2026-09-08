package ic2.core.machine;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class SteamBoilerTest {
    private static SteamBoiler.State state(double temperature) {
        return new SteamBoiler.State(temperature, 0, Long.MIN_VALUE);
    }

    @Test
    void warmupAndPressureUseTheirOwnHeatBudgets() {
        var zero = new SteamBoiler.Settings(1, 0);
        var warm = SteamBoiler.step(state(25), zero, 1, 1200, 10, true, 25);
        assertEquals(25.6, warm.next().temperature(), 1e-9);
        assertEquals(SteamBoiler.Output.DISTILLED_WATER, warm.output());
        assertEquals(1, warm.outputAmount());
        var pressure =
                SteamBoiler.step(
                        state(25), new SteamBoiler.Settings(1, 220), 1, 1200, 10, true, 25);
        assertEquals(0, pressure.water());
        assertEquals(SteamBoiler.Output.NONE, pressure.output());
        var steam = SteamBoiler.step(state(100), zero, 1, 100, 10, true, 25);
        assertEquals(100, steam.outputAmount());
        assertEquals(100, steam.next().temperature());
        assertEquals(SteamBoiler.Output.STEAM, steam.output());
        var hot =
                SteamBoiler.step(
                        state(374), new SteamBoiler.Settings(1, 220), 1, 200, 10, true, 25);
        assertEquals(SteamBoiler.Output.SUPERHEATED_STEAM, hot.output());
        assertEquals(374, hot.next().temperature());
    }

    @Test
    void storedHeatAndWaterCoolingAreBounded() {
        var step =
                SteamBoiler.step(
                        state(110), new SteamBoiler.Settings(1000, 0), 1, 100, 1000, false, 25);
        assertEquals(1200, step.outputAmount());
        assertEquals(20, step.water());
        assertEquals(107.45, step.next().temperature(), 1e-9);
        assertEquals(20, step.next().scale());
        assertEquals(
                0,
                SteamBoiler.step(
                                state(110),
                                new SteamBoiler.Settings(1000, 0),
                                1,
                                100,
                                1000,
                                true,
                                25)
                        .next()
                        .scale());
        assertEquals(
                step.next(),
                SteamBoiler.step(
                                step.next(),
                                new SteamBoiler.Settings(1, 0),
                                1,
                                1200,
                                1000,
                                false,
                                25)
                        .next());
    }

    @Test
    void calcificationAndOverheatingHaveExplicitOutcomes() {
        var scaled = new SteamBoiler.State(100, SteamBoiler.MAX_SCALE, 0);
        var stopped =
                SteamBoiler.step(scaled, new SteamBoiler.Settings(1, 0), 1, 1200, 100, false, 25);
        assertEquals(0, stopped.water());
        assertEquals(99.99, stopped.next().temperature(), 1e-9);
        var hot =
                SteamBoiler.step(
                        state(499.9), new SteamBoiler.Settings(0, 0), 1, 1200, 0, true, 25);
        assertTrue(hot.overheated());
        assertEquals(500, hot.next().temperature());
        assertFalse(
                SteamBoiler.step(state(500), new SteamBoiler.Settings(0, 0), 1, 0, 0, true, 25)
                        .overheated());
    }

    @Test
    void settingsRejectUnknownRequestsAndClampBothControls() {
        var settings = new SteamBoiler.Settings(999, 299).configure(3).configure(10);
        assertEquals(new SteamBoiler.Settings(1000, 300), settings);
        assertEquals(0, settings.configure(7).waterPerTick());
        assertEquals(0, settings.configure(13).configure(13).configure(13).pressure());
        assertThrows(IllegalArgumentException.class, () -> settings.configure(Integer.MAX_VALUE));
        assertThrows(IllegalArgumentException.class, () -> state(Double.NaN));
    }
}
