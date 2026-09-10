package ic2.neoforge.test;

import static net.minecraft.world.inventory.ContainerInput.PICKUP;

import ic2.neoforge.component.CropSeed;
import ic2.neoforge.component.ModDataComponents;
import ic2.neoforge.crop.CropBlockEntity;
import ic2.neoforge.item.CropAnalyzerItem;
import ic2.neoforge.item.ElectricItemEnergy;
import ic2.neoforge.menu.CropAnalyzerMenu;
import ic2.neoforge.registration.ModCrops;
import ic2.neoforge.registration.ModItems;
import ic2.neoforge.registration.ModTools;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;

final class CropAnalyzerTests {
    private static final BlockPos POSITION = new BlockPos(8, 8, 8);

    private static ItemStack bag(int scan) {
        var bag = ModCrops.CROP_SEED_BAG.toStack();
        bag.set(ModDataComponents.CROP_SEED.get(), new CropSeed("wheat", 5, 3, 2, scan));
        return bag;
    }

    private static int scan(ItemStack stack) {
        return stack.get(ModDataComponents.CROP_SEED.get()).scan();
    }

    private static List<ItemStack> slots(ItemStack input, ItemStack output, ItemStack battery) {
        return new ArrayList<>(List.of(input, output, battery));
    }

    /** Legacy loop: take the scanned bag out of the output and feed it back as the input. */
    private static void rotate(List<ItemStack> slots) {
        slots.set(0, slots.get(1));
        slots.set(1, ItemStack.EMPTY);
    }

    static void scanLadder(GameTestHelper helper) {
        var item = (CropAnalyzerItem) ModTools.CROP_ANALYZER.get();
        helper.assertTrue(
                item.specification().capacity() == 100000
                        && item.specification().transferLimit() == 128
                        && item.specification().tier() == 2,
                "The analyzer keeps the legacy 100k buffer at tier 2");

        var analyzer = ModTools.CROP_ANALYZER.toStack();
        var slots = slots(bag(0), ItemStack.EMPTY, ItemStack.EMPTY);
        helper.assertTrue(
                !item.tryScan(analyzer, slots) && slots.get(1).isEmpty(),
                "An uncharged analyzer scans nothing");
        ElectricItemEnergy.charge(analyzer, 10001, 2, true, false);
        helper.assertTrue(
                item.tryScan(analyzer, slots)
                        && scan(slots.get(1)) == 1
                        && ElectricItemEnergy.charge(analyzer) == 10001 - 10,
                "The first scan pays the legacy 10 EU and lifts the seed to level 1");
        rotate(slots);
        helper.assertTrue(
                item.tryScan(analyzer, slots)
                        && scan(slots.get(1)) == 2
                        && ElectricItemEnergy.charge(analyzer) == 10001 - 10 - 90,
                "The second scan pays the legacy 90 EU");
        rotate(slots);
        helper.assertTrue(
                item.tryScan(analyzer, slots)
                        && scan(slots.get(1)) == 3
                        && ElectricItemEnergy.charge(analyzer) == 10001 - 10 - 90 - 900,
                "The third scan pays the legacy 900 EU");
        rotate(slots);
        helper.assertTrue(
                item.tryScan(analyzer, slots)
                        && scan(slots.get(1)) == 4
                        && ElectricItemEnergy.charge(analyzer) == 1,
                "The fourth scan pays the legacy 9000 EU");
        rotate(slots);
        double before = ElectricItemEnergy.charge(analyzer);
        helper.assertTrue(
                item.tryScan(analyzer, slots)
                        && scan(slots.get(1)) == 4
                        && ElectricItemEnergy.charge(analyzer) == before,
                "A fully scanned bag moves to the output for free");
        helper.assertTrue(
                !item.tryScan(analyzer, slots(slots.get(1), bag(0), ItemStack.EMPTY)),
                "A full output blocks scanning");
        helper.assertTrue(
                !item.tryScan(analyzer, slots(new ItemStack(Items.DIRT), ItemStack.EMPTY, ItemStack.EMPTY)),
                "Only seed bags scan");
        helper.succeed();
    }

    static void cropReport(GameTestHelper helper) {
        helper.setBlock(POSITION.below(), Blocks.FARMLAND);
        helper.setBlock(POSITION, ModCrops.CROP_STICK.get().defaultBlockState());
        CropBlockEntity crop = helper.getBlockEntity(POSITION, CropBlockEntity.class);
        helper.assertTrue(
                crop.tryPlantIn(ModCrops.WHEAT_CARD, 0, 2, 2, 2, 4),
                "Wheat plants into the crop stick");
        // Planting replaces the stick block, so the tile must be re-fetched from the crop block.
        crop = helper.getBlockEntity(POSITION, CropBlockEntity.class);
        var item = (CropAnalyzerItem) ModTools.CROP_ANALYZER.get();
        var analyzer = ModTools.CROP_ANALYZER.toStack();
        helper.assertTrue(
                item.analyzeCrop(analyzer, crop).isEmpty(),
                "An uncharged analyzer sends no report");
        ElectricItemEnergy.charge(analyzer, 1000, 2, true, false);
        var report = item.analyzeCrop(analyzer, crop);
        helper.assertTrue(
                report.size() == 7, "The legacy report answers with seven lines, saw " + report.size());
        helper.assertTrue(
                ElectricItemEnergy.charge(analyzer) == 100,
                "The report pays the legacy 900 EU fee");
        helper.succeed();
    }

    static void menuIntegration(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var analyzer = ModTools.CROP_ANALYZER.toStack();
        ElectricItemEnergy.charge(analyzer, 10000, 2, true, false);
        player.getInventory().setItem(0, analyzer);
        var menu =
                new CropAnalyzerMenu(
                        1, player.getInventory(), player.getInventory().getSelectedSlot());
        player.containerMenu = menu;
        helper.assertTrue(
                menu.stillValid(player), "The GUI stays open while the analyzer is held");

        var battery = ModItems.RE_BATTERY.toStack();
        ElectricItemEnergy.charge(battery, 10000, 1, true, false);
        helper.assertTrue(
                menu.getSlot(0).mayPlace(bag(0))
                        && !menu.getSlot(0).mayPlace(new ItemStack(Items.DIRT)),
                "The input slot only takes seed bags");
        helper.assertTrue(
                !menu.getSlot(1).mayPlace(bag(0)), "The output slot accepts nothing");
        helper.assertTrue(
                menu.getSlot(2).mayPlace(battery) && !menu.getSlot(2).mayPlace(new ItemStack(Items.DIRT)),
                "The battery slot gates on dischargeable items");

        menu.setCarried(bag(0));
        menu.clicked(0, 0, PICKUP, player);
        menu.broadcastChanges();
        helper.assertTrue(
                menu.getSlot(0).getItem().isEmpty()
                        && scan(menu.getSlot(1).getItem()) == 1
                        && ElectricItemEnergy.charge(analyzer) == 10000 - 10,
                "One broadcast runs the 10 EU scan and moves the bag to the output");
        var saved = analyzer.get(ModDataComponents.ANALYZER_CONTENTS.get());
        // Trailing empty slots trim, so the bag in the middle leaves two stored slots.
        helper.assertTrue(
                saved.getSlots() == 2 && scan(saved.getStackInSlot(1)) == 1,
                "The slots persist on the analyzer stack");
        helper.succeed();
    }

    private CropAnalyzerTests() {}
}
