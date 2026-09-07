package ic2.core.energy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Random;

class ElectricalProfileTest {
    @ParameterizedTest
    @EnumSource(VoltageTier.class)
    void integerCalculationsMatchLegacyRulesAcrossTheInputRange(VoltageTier tier) {
        ElectricalProfile profile = new ElectricalProfile(tier);
        int voltage = tier.getVoltage();
        for (int power :
                new int[] {-1, 0, 1, voltage - 1, voltage, voltage + 1, Integer.MAX_VALUE}) {
            assertLegacyCurrents(profile, power);
        }
        Random random = new Random(2612);
        for (int sample = 0; sample < 1000; sample++) {
            assertLegacyCurrents(profile, random.nextInt(Integer.MAX_VALUE));
        }
    }

    private static void assertLegacyCurrents(ElectricalProfile profile, int input) {
        profile.setRecipePower(input);
        int power = Math.max(0, input);
        int voltage = profile.getWorkingVoltage().getVoltage();
        assertEquals(power, profile.getRecipePower());
        assertEquals((double) power / voltage, profile.getDisplayCurrent());
        assertEquals(
                power == 0 ? 0 : (int) Math.ceil((double) power / voltage),
                profile.getWorkingCurrent());
        assertEquals(
                power == 0 ? 1 : (int) Math.floor(2.0 * power / voltage) + 1,
                profile.getMaxSinkAmperage());
    }

    @Test
    void voltageAndSinkOverridesFollowMachineStateChanges() {
        ElectricalProfile profile = new ElectricalProfile(VoltageTier.LV);
        profile.setRecipePower(33);
        assertEquals(2, profile.getWorkingCurrent());
        profile.setSinkWorkingVoltage(VoltageTier.HV);
        profile.setWorkingVoltage(VoltageTier.MV);
        assertEquals(VoltageTier.HV, profile.getSinkWorkingVoltage());
        assertEquals(1, profile.getWorkingCurrent());
        profile.clearSinkWorkingVoltage();
        assertEquals(VoltageTier.MV, profile.getSinkWorkingVoltage());
        profile.setSinkWorkingVoltage(null);
        assertEquals(VoltageTier.MV, profile.getSinkWorkingVoltage());
    }

    @Test
    void zeroIsARealAmperageOverrideAndNegativeRestoresCalculation() {
        ElectricalProfile profile = new ElectricalProfile(VoltageTier.LV);
        profile.setRecipePower(32);
        assertEquals(3, profile.getMaxSinkAmperage());
        profile.setMaxSinkAmperageOverride(0);
        assertEquals(0, profile.getMaxSinkAmperage());
        profile.setMaxSinkAmperageOverride(7);
        assertEquals(7, profile.getMaxSinkAmperage());
        profile.clearMaxSinkAmperageOverride();
        assertEquals(3, profile.getMaxSinkAmperage());
        profile.setMaxSinkAmperageOverride(-2);
        assertEquals(3, profile.getMaxSinkAmperage());
    }

    @Test
    void invalidWorkingVoltageFailsAtTheBoundary() {
        assertThrows(NullPointerException.class, () -> new ElectricalProfile(null));
        ElectricalProfile profile = new ElectricalProfile(VoltageTier.LV);
        assertThrows(NullPointerException.class, () -> profile.setWorkingVoltage(null));
        assertEquals(VoltageTier.LV, profile.getWorkingVoltage());
    }
}
