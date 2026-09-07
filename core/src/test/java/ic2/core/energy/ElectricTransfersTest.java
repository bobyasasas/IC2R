package ic2.core.energy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import java.util.Random;

class ElectricTransfersTest {
    private static final ElectricItemSpec BATTERY = new ElectricItemSpec(10000, 100, 1, true);

    @Test
    void tierLimitCapacityAndExternalOutputAreIndependentConstraints() {
        assertEquals(0, ElectricTransfers.charge(0, BATTERY, 500, 0, false).transferred());
        assertEquals(100, ElectricTransfers.charge(0, BATTERY, 500, 1, false).transferred());
        assertEquals(50, ElectricTransfers.charge(9950, BATTERY, 500, 1, false).transferred());
        assertEquals(
                10000,
                ElectricTransfers.charge(0, BATTERY, Double.POSITIVE_INFINITY, 1, true).stored());
        var tool = new ElectricItemSpec(10000, 100, 1, false);
        assertEquals(0, ElectricTransfers.discharge(100, tool, 50, 1, true, true).transferred());
        assertEquals(50, ElectricTransfers.discharge(100, tool, 50, 1, true, false).transferred());
    }

    @Test
    void invalidRequestsCannotCreateOrDestroyEnergy() {
        for (double request : new double[] {-1, Double.NEGATIVE_INFINITY, Double.NaN, 0}) {
            assertEquals(
                    new ElectricTransfers.Result(500, 0),
                    ElectricTransfers.charge(500, BATTERY, request, 1, true));
            assertEquals(
                    new ElectricTransfers.Result(500, 0),
                    ElectricTransfers.discharge(500, BATTERY, request, 1, true, true));
        }
        assertThrows(
                IllegalArgumentException.class,
                () -> ElectricTransfers.charge(Double.NaN, BATTERY, 1, 1, true));
        assertThrows(
                IllegalArgumentException.class,
                () -> new ElectricItemSpec(Double.POSITIVE_INFINITY, 100, 1, true));
    }

    @Test
    void randomChargeAndDischargeSequencesConserveEnergy() {
        var random = new Random(2612);
        double stored = 0;
        double balance = 0;
        for (int i = 0; i < 10000; i++) {
            double requested = random.nextDouble() * 200;
            boolean input = random.nextBoolean();
            var result =
                    input
                            ? ElectricTransfers.charge(stored, BATTERY, requested, 1, false)
                            : ElectricTransfers.discharge(
                                    stored, BATTERY, requested, 1, false, true);
            balance += input ? result.transferred() : -result.transferred();
            stored = result.stored();
            assertEquals(balance, stored, 1e-8);
        }
    }
}
