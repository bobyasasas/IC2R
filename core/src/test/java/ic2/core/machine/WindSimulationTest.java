package ic2.core.machine;

import static org.junit.jupiter.api.Assertions.*;

import ic2.core.energy.EnergyStore;

import org.junit.jupiter.api.Test;

import java.util.Random;

class WindSimulationTest {
    @Test
    void heightCurveAndWeatherMatchTheLegacyConstraints() {
        double peak = 63 + (384 - 63) / 2.0;
        assertEquals(1, WindSimulation.heightFactor(peak, 384, 63), 1e-12);
        assertEquals(0, WindSimulation.heightFactor(432, 384, 63), 1e-12);
        assertEquals(0, WindSimulation.heightFactor(0, 384, 63));
        assertEquals(0, WindSimulation.heightFactor(-60, 384, 63));
        assertEquals(
                WindSimulation.heightFactor(peak - .001, 384, 63),
                WindSimulation.heightFactor(peak + .001, 384, 63),
                1e-12);
        var wind = new WindSimulation(new WindSimulation.State(30, 0, 0));
        assertEquals(72, wind.windAt(224, 384, 64, false, false), 1e-12);
        assertEquals(90, wind.windAt(224, 384, 64, true, false), 1e-12);
        assertEquals(108, wind.windAt(224, 384, 64, true, true), 1e-12);
    }

    @Test
    void randomWalkIsBoundedAndDirectionWrapsAt360() {
        var wind = new WindSimulation(new WindSimulation.State(30, 0, 127));
        wind.tick(bound -> 0);
        assertEquals(new WindSimulation.State(29, 342, 0), wind.state());
        wind = new WindSimulation(new WindSimulation.State(0, 350, 127));
        wind.tick(bound -> bound == 3 ? 2 : 0);
        assertEquals(new WindSimulation.State(1, 8, 0), wind.state());
        var random = new Random(2701);
        for (int i = 0; i < 128000; i++) wind.tick(random::nextInt);
        assertTrue(wind.state().strength() >= 0 && wind.state().strength() <= 30);
    }

    @Test
    void fullObstructionAndPacketHeadroomDoNotCreateOrStrandEnergy() {
        assertEquals(10.8 / 567, WindSimulation.production(108, 566, 1), 1e-12);
        assertEquals(0, WindSimulation.production(108, 0, 0));
        assertEquals(32, WindSimulation.capacity(false, 1));
        assertEquals(43, WindSimulation.capacity(true, 1));
        for (double production : new double[] {.013, 1.23, 10.8}) {
            var energy = new EnergyStore(WindSimulation.capacity(true, 1));
            double delivered = 0, generated = 0;
            for (int i = 0; i < 10000; i++) {
                if (energy.capacity() - energy.stored() >= production)
                    generated += energy.insert(production);
                if (energy.stored() >= 32) delivered += energy.extract(32);
            }
            assertTrue(delivered >= 32);
            assertEquals(generated, delivered + energy.stored(), 1e-7);
        }
    }
}
