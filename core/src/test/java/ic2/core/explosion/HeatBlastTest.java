package ic2.core.explosion;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.util.HashMap;

class HeatBlastTest {
    @Test
    void thermalResistanceStopsRaysAndTerrainIsReadOnce() {
        var calls = new HashMap<HeatBlast.Position, Integer>();
        var result =
                HeatBlast.trace(
                        10,
                        .5,
                        .5,
                        .5,
                        pos -> {
                            calls.merge(pos, 1, Integer::sum);
                            return pos.x() == 0 && pos.y() == 0 && pos.z() == 0
                                    ? HeatBlast.Cell.AIR
                                    : HeatBlast.Cell.solid(6);
                        });
        assertTrue(result.blocks().isEmpty());
        assertTrue(result.samples().isEmpty());
        assertTrue(calls.values().stream().allMatch(count -> count == 1));
        assertEquals(18.5, HeatBlast.Cell.solid(6).absorption());
    }

    @Test
    void lowPowerCannotBreakSolidsAndBarriersDoNotTransmitHeat() {
        assertTrue(HeatBlast.trace(1, .5, .5, .5, p -> HeatBlast.Cell.solid(0)).blocks().isEmpty());
        assertTrue(HeatBlast.trace(10, .5, .5, .5, p -> HeatBlast.Cell.BARRIER).blocks().isEmpty());
        var weak = HeatBlast.trace(10, .5, .5, .5, p -> HeatBlast.Cell.solid(0));
        assertEquals(1, weak.blocks().size());
        assertTrue(weak.blocks().get(new HeatBlast.Position(0, 0, 0)));
        var air = HeatBlast.trace(10, .5, .5, .5, p -> HeatBlast.Cell.AIR);
        assertTrue(
                !air.samples().isEmpty()
                        && air.samples().stream().allMatch(p -> p.step() < 20 && p.power() > 0));
        assertThrows(
                IllegalArgumentException.class,
                () -> HeatBlast.trace(11, 0, 0, 0, p -> HeatBlast.Cell.AIR));
    }
}
