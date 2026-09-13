package ic2.neoforge.test;

import ic2.neoforge.item.ElectricItemEnergy;
import ic2.neoforge.menu.ScannerMenu;
import ic2.neoforge.registration.ModTools;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

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

    static void menuScan(GameTestHelper helper) {
        Level level = helper.getLevel();
        BlockPos center = new BlockPos(8, 8, 8);
        helper.setBlock(new BlockPos(8, 8, 7), Blocks.IRON_ORE);
        helper.setBlock(new BlockPos(8, 8, 9), Blocks.IRON_ORE);
        helper.setBlock(new BlockPos(7, 8, 8), Blocks.DEEPSLATE_IRON_ORE);
        helper.setBlock(new BlockPos(9, 8, 8), Blocks.COPPER_ORE);
        helper.setBlock(new BlockPos(8, 9, 8), Blocks.STONE);
        var found = ScannerMenu.scanAround(level, helper.absolutePos(center), 1);
        helper.assertTrue(
                found.size() == 3
                        && found.get(0).count() == 2
                        && found.get(1).count() == 1
                        && found.get(2).count() == 1,
                "The cube scan sums one pile per ore item and sorts by descending count");
        helper.assertTrue(
                found.get(0).stack().getItem() == Blocks.IRON_ORE.asItem()
                        && found.stream().allMatch(entry -> entry.stack().is(ItemTags.IRON_ORES)
                                || entry.stack().getItem() == Blocks.COPPER_ORE.asItem()),
                "Every reported pile passes the legacy ore tags");
        helper.assertTrue(
                found.get(1).stack().getItem() == Blocks.DEEPSLATE_IRON_ORE.asItem()
                                && found.get(2).stack().getItem() == Blocks.COPPER_ORE.asItem()
                        || found.get(2).stack().getItem() == Blocks.DEEPSLATE_IRON_ORE.asItem()
                                && found.get(1).stack().getItem() == Blocks.COPPER_ORE.asItem(),
                "Each ore variant keeps its own pile like the legacy item key");
        helper.assertTrue(
                found.stream()
                        .noneMatch(entry -> entry.stack().getItem() == Blocks.STONE.asItem()),
                "Plain stone is not reported");
        helper.succeed();
    }

    static void menuScanAndUse(GameTestHelper helper) {
        helper.setBlock(new BlockPos(8, 8, 7), Blocks.IRON_ORE);
        helper.setBlock(new BlockPos(8, 8, 9), Blocks.IRON_ORE);
        helper.setBlock(new BlockPos(7, 8, 8), Blocks.DEEPSLATE_IRON_ORE);
        helper.setBlock(new BlockPos(9, 8, 8), Blocks.COPPER_ORE);
        helper.setBlock(new BlockPos(8, 9, 8), Blocks.STONE);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var scanner = ModTools.SCANNER.toStack();
        ElectricItemEnergy.charge(scanner, 1000, 1, true, false);
        player.getInventory().setItem(0, scanner);
        var menu =
                new ScannerMenu(
                        1, player.getInventory(), player.getInventory().getSelectedSlot());
        player.containerMenu = menu;
        helper.assertTrue(
                menu.stillValid(player), "The GUI stays open while the scanner is held");
        menu.scan(helper.getLevel(), helper.absolutePos(new BlockPos(8, 8, 8)));
        menu.broadcastChanges();
        helper.assertTrue(
                menu.resultCount() == 3 && menu.resultTotal(0) == 2,
                "The menu reports the scanned piles through its data slots");
        helper.assertTrue(
                menu.resultStack(0).is(ItemTags.IRON_ORES),
                "The first row carries the biggest pile's item");
        helper.assertTrue(
                ModTools.SCANNER.get().handScanRange() == 6
                        && ModTools.ADVANCED_SCANNER.get().handScanRange() == 12,
                "The handheld sweep keeps the legacy 6 and 12 block radii");
        helper.assertTrue(
                ModTools.SCANNER.get()
                                .use(helper.getLevel(), player, InteractionHand.MAIN_HAND)
                        == InteractionResult.SUCCESS,
                "A charged scanner answers a successful use");
        helper.assertTrue(
                ElectricItemEnergy.charge(scanner) == 1000,
                "Only the server side pays the scan fee");
        var empty = ModTools.SCANNER.toStack();
        ElectricItemEnergy.charge(empty, 10, 1, true, false);
        player.getInventory().setItem(0, empty);
        helper.assertTrue(
                ModTools.SCANNER.get()
                                .use(helper.getLevel(), player, InteractionHand.MAIN_HAND)
                        == InteractionResult.FAIL,
                "An uncharged scanner refuses to open the GUI");
        helper.succeed();
    }

    static void menuDropCloses(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var scanner = ModTools.SCANNER.toStack();
        player.getInventory().setItem(0, scanner);
        var menu = new ScannerMenu(1, player.getInventory(), 0);
        player.containerMenu = menu;
        helper.assertTrue(
                ModTools.SCANNER.get().onDroppedByPlayer(scanner, player)
                        && player.containerMenu != menu,
                "Dropping the open scanner closes its GUI like the legacy drop hook");
        helper.succeed();
    }

    private ScannerItemTests() {}
}
