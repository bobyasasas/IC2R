package ic2.core.crop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Deterministic checks of the legacy crop growth and terrain formulas. */
class CropMathTest {
    @Test
    void surplusQualityGrowsWithoutReset() {
        // Tier one with zero stats demands quality 0; any environment works.
        CropMath.GrowthResult result = CropMath.growth(0, 0, 0, 1, 20, rand(42));
        assertTrue(result.points() > 0);
        assertFalse(result.reset());
    }

    @Test
    void severeDeficitCanKillTheCrop() {
        // Tier five with maxed stats demands far more than the environment provides; with zero
        // resistance the kill roll succeeds whenever the roll exceeds it.
        int kills = 0;
        for (long seed = 0; seed < 40; seed++) {
            CropMath.GrowthResult result = CropMath.growth(31, 31, 0, 5, 20, rand(seed));
            if (result.reset()) kills++;
        }
        assertTrue(kills > 30, "Severe deficits kill most low-resistance crops, saw " + kills);
    }

    @Test
    void deficitSlowButSurvivesWithHighResistance() {
        // Resistance 31 beats every kill roll (nextInt(32) >= 31 survives); growth keeps running.
        for (long seed = 0; seed < 40; seed++) {
            CropMath.GrowthResult result = CropMath.growth(31, 31, 31, 5, 20, rand(seed));
            assertFalse(result.reset());
            assertTrue(result.points() >= 0);
        }
    }

    @Test
    void humidityFollowsWaterSources() {
        assertEquals(0, CropMath.humidity(0, false, 0));
        assertEquals(2, CropMath.humidity(0, true, 0));
        // Watered farmland plus five stored water: two bonus points each plus the storage term.
        assertEquals(5, CropMath.humidity(0, true, 5));
        assertEquals(1, CropMath.humidity(0, false, 1));
        assertEquals(6, CropMath.humidity(3, false, 25));
    }

    @Test
    void nutrientsFollowDirtDepthAndStorage() {
        assertEquals(0, CropMath.nutrients(0, 0, 0));
        assertEquals(3, CropMath.nutrients(1, 2, 0));
        assertEquals(1, CropMath.nutrients(0, 0, 1));
        // (20 + 19) / 20 stays 1 in the legacy integer division.
        assertEquals(1, CropMath.nutrients(0, 0, 20));
    }

    @Test
    void airQualityClampsAltitudeAndRewardsOpenSky() {
        // The fresh-air budget starts at nine; each obstructing neighbour column costs one.
        assertEquals(4, CropMath.airQuality(0, 9, false));
        assertEquals(8, CropMath.airQuality(0, 9, true));
        assertEquals(5, CropMath.airQuality(1, 9, false));
        assertEquals(6, CropMath.airQuality(9, 9, false));
        assertEquals(2, CropMath.airQuality(0, 5, false));
        assertEquals(0, CropMath.airQuality(0, 1, false));
    }

    private CropRandom rand(long seed) {
        java.util.Random random = new java.util.Random(seed);
        return random::nextInt;
    }
}
