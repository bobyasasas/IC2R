package ic2.neoforge.test;

import ic2.neoforge.item.ElectricItemEnergy;
import ic2.neoforge.registration.ModTools;

import net.minecraft.gametest.framework.GameTestHelper;

final class ScannerItemTests {
    static void layerScan(GameTestHelper helper) {
        var scanner = ModTools.SCANNER.toStack();
        var item = ModTools.SCANNER.get();
        helper.assertTrue(
                item.startLayerScan(scanner) == 0, "An uncharged scanner answers no scan pulse");
        ElectricItemEnergy.charge(scanner, 1000, 1, true, false);
        helper.assertTrue(
                item.startLayerScan(scanner) == 3 && ElectricItemEnergy.charge(scanner) == 950,
                "One basic pulse costs the legacy 50 EU and scans half the range");
        helper.assertTrue(
                item.startLayerScan(scanner) == 3 && ElectricItemEnergy.charge(scanner) == 900,
                "Every level scan pays its own pulse");
        var empty = ModTools.SCANNER.toStack();
        ElectricItemEnergy.charge(empty, 10, 1, true, false);
        helper.assertTrue(
                item.startLayerScan(empty) == 0 && ElectricItemEnergy.charge(empty) == 10,
                "An insufficient charge buys no pulse and is not drained");
        var advanced = ModTools.ADVANCED_SCANNER.toStack();
        ElectricItemEnergy.charge(advanced, 1000, 2, true, false);
        helper.assertTrue(
                ModTools.ADVANCED_SCANNER.get().startLayerScan(advanced) == 6
                        && ElectricItemEnergy.charge(advanced) == 750,
                "The advanced scanner pays 250 EU for a range-6 pulse");
        helper.assertTrue(
                item.specification().capacity() == 100000
                        && item.specification().transferLimit() == 128
                        && item.specification().tier() == 1
                        && ModTools.ADVANCED_SCANNER.get().specification().capacity() == 1000000
                        && ModTools.ADVANCED_SCANNER.get().specification().transferLimit() == 512
                        && ModTools.ADVANCED_SCANNER.get().specification().tier() == 2,
                "Both scanners keep their legacy buffers and tiers");
        helper.succeed();
    }

    private ScannerItemTests() {}
}
