package ic2.neoforge.test;

import ic2.neoforge.item.UpgradeItem.Kind;
import ic2.neoforge.machine.MachineKind;

import net.minecraft.gametest.framework.GameTestHelper;

/**
 * The 2026-09-13 parity pass over the legacy per-machine UpgradableProperty sets: every case here
 * pins a combination the suitability matrix used to reject despite legacy accepting it.
 */
final class UpgradeSuitabilityTests {
    static void suitabilityMatrix(GameTestHelper helper) {
        helper.assertTrue(
                Kind.REDSTONE_INVERTER.suitable(MachineKind.MAGNETIZER),
                "The magnetizer accepts the redstone inverter (legacy RedstoneSensitive)");
        helper.assertTrue(
                Kind.EJECTOR.suitable(MachineKind.CHUNK_LOADER)
                        && Kind.PULLING.suitable(MachineKind.CHUNK_LOADER),
                "The chunk loader accepts item ejector and pulling (legacy item consuming/producing)");
        helper.assertTrue(
                Kind.FLUID_PULLING.suitable(MachineKind.BLAST_FURNACE),
                "The blast furnace accepts fluid pulling (legacy fluid consuming)");
        helper.assertTrue(
                Kind.FLUID_EJECTOR.suitable(MachineKind.MATTER_GENERATOR)
                        && Kind.TRANSFORMER.suitable(MachineKind.MATTER_GENERATOR),
                "The matter generator accepts fluid ejector and transformer");
        helper.assertTrue(
                Kind.FLUID_EJECTOR.suitable(MachineKind.PUMP)
                        && Kind.FLUID_EJECTOR.suitable(MachineKind.SOLAR_DISTILLER),
                "Pump and solar distiller accept the fluid ejector (legacy fluid producing)");
        // The port has no separate bottler kind (the canner covers it, pending the split
        // decision); the canner carries the full legacy property set and takes both.
        helper.assertTrue(
                Kind.FLUID_EJECTOR.suitable(MachineKind.CANNER)
                        && Kind.FLUID_PULLING.suitable(MachineKind.CANNER),
                "The canner accepts both fluid directional upgrades");
        helper.assertFalse(
                Kind.FLUID_PULLING.suitable(MachineKind.MATTER_GENERATOR),
                "The matter generator has no legacy fluid consuming property");
        helper.assertFalse(
                Kind.FLUID_PULLING.suitable(MachineKind.SOLAR_DISTILLER),
                "The solar distiller has no legacy fluid consuming property");
        helper.assertFalse(
                Kind.REDSTONE_INVERTER.suitable(MachineKind.MINER),
                "The miner has no legacy redstone sensitive property");
        helper.succeed();
    }

    private UpgradeSuitabilityTests() {}
}
