package ic2.core.machine;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class SteamTurbineCycleTest {
    @Test
    void normalAndHotStagesConserveSteamAndCondensate() {
        var state = new SteamTurbineCycle.State(0, Long.MIN_VALUE);
        long input = 0, exhaust = 0, water = 0;
        for (int tick = 0; tick < 100; tick++) {
            int batch = tick % 3 == 0 ? 21000 : tick;
            var step = SteamTurbineCycle.plan(state, tick, batch, false, 0, true, 1);
            input += step.steam();
            exhaust += step.exhaust();
            water += step.water();
            state = step.next();
            assertEquals(input, exhaust + water * 100 + state.condensedSteam());
            assertEquals(batch * 2, step.kinetic());
        }
        var hot =
                SteamTurbineCycle.plan(new SteamTurbineCycle.State(0, 0), 1, 100, true, 0, true, 1);
        assertEquals(100, hot.exhaust());
        assertEquals(400, hot.kinetic());
        assertEquals(0, hot.next().condensedSteam());
    }

    @Test
    void waterThrottlesAndPaidCondensateCanFinishWithoutNewSteam() {
        var state = new SteamTurbineCycle.State(2100, 0);
        var step = SteamTurbineCycle.plan(state, 1, 0, false, 0, true, 1);
        assertEquals(1, step.water());
        assertEquals(2000, step.next().condensedSteam());
        assertEquals(
                42,
                SteamTurbineCycle.plan(
                                new SteamTurbineCycle.State(0, 0), 1, 21000, false, 999, true, 1)
                        .kinetic());
        var full = SteamTurbineCycle.plan(state, 1, 21000, false, 1000, false, 1);
        assertEquals(0, full.steam());
        assertEquals(2100, full.next().condensedSteam());
        var disabled = SteamTurbineCycle.plan(state, 1, 21000, false, 0, true, 0);
        assertEquals(0, disabled.steam());
        assertEquals(1, disabled.water());
    }

    @Test
    void creditAndOutputNeverOverflowOrReuseTheSameTick() {
        var state = new SteamTurbineCycle.State(SteamTurbineCycle.MAX_CREDIT, 0);
        var step = SteamTurbineCycle.plan(state, 1, 21000, false, 0, true, 1);
        assertEquals(0, step.steam());
        assertEquals(state.condensedSteam() - 100, step.next().condensedSteam());
        assertEquals(
                Integer.MAX_VALUE,
                SteamTurbineCycle.plan(
                                new SteamTurbineCycle.State(0, 0), 1, 21000, true, 0, true, 1000000)
                        .kinetic());
        assertEquals(0, SteamTurbineCycle.plan(step.next(), 1, 21000, false, 0, true, 1).steam());
    }

    @Test
    void aPaidBatchCannotBecomeFreeWorkWhenConsumerTicksFirst() {
        var work = new WorkBuffer(1000);
        work.publish(200);
        assertEquals(200, work.extract(1, 200, 200));
        assertEquals(0, work.available(2, 200));
        work.publish(200);
        assertEquals(100, work.extract(2, 200, 100));
        work.publish(400);
        assertEquals(300, work.extract(2, 400, 1000));
        assertEquals(0, work.available(2, 400));
        assertEquals(100, work.available(3, 400));
        work.publish(0);
        assertEquals(0, work.available(3, 400));
    }
}
