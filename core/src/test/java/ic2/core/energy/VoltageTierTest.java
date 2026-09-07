package ic2.core.energy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class VoltageTierTest {
    @ParameterizedTest
    @EnumSource(VoltageTier.class)
    void powerAtEachBoundaryUsesTheSmallestSufficientTier(VoltageTier tier) {
        assertEquals(tier, VoltageTier.fromPower(tier.getVoltage()));
        assertEquals(tier, VoltageTier.fromPower(Math.nextDown((double) tier.getVoltage())));
        assertEquals(
                VoltageTier.fromIcTier(tier.getIcTier() + 1),
                VoltageTier.fromPower(Math.nextUp((double) tier.getVoltage())));
        assertEquals(tier, VoltageTier.fromIcTier(tier.getIcTier()));
    }

    @Test
    void outOfRangeInputsKeepLegacyFallbacks() {
        assertEquals(VoltageTier.ULV, VoltageTier.fromIcTier(Integer.MIN_VALUE));
        assertEquals(VoltageTier.IV, VoltageTier.fromIcTier(Integer.MAX_VALUE));
        assertEquals(VoltageTier.ULV, VoltageTier.fromPower(0));
        assertEquals(VoltageTier.ULV, VoltageTier.fromPower(-1));
        assertEquals(VoltageTier.ULV, VoltageTier.fromPower(Double.NEGATIVE_INFINITY));
        assertEquals(VoltageTier.ULV, VoltageTier.fromPower(Double.NaN));
        assertEquals(VoltageTier.IV, VoltageTier.fromPower(Double.POSITIVE_INFINITY));
    }
}
