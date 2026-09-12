package ic2.neoforge.test;

import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.machine.PatternStorageBlockEntity;
import ic2.neoforge.machine.UuScannerBlockEntity;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.registration.ModReactorItems;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.transfer.item.ItemResource;

final class PatternStorageTests {
    private static final BlockPos SCANNER_POS = new BlockPos(8, 8, 8);

    private static UuScannerBlockEntity scanner(GameTestHelper helper) {
        helper.setBlock(SCANNER_POS, ModMachines.block(MachineKind.UU_SCANNER).defaultBlockState());
        return helper.getBlockEntity(SCANNER_POS, UuScannerBlockEntity.class);
    }

    static void scannerTransfersToStorage(GameTestHelper helper) {
        var scanner = scanner(helper);
        helper.setBlock(
                SCANNER_POS.north(),
                ModMachines.block(MachineKind.PATTERN_STORAGE).defaultBlockState());
        var storage = helper.getBlockEntity(SCANNER_POS.north(), PatternStorageBlockEntity.class);
        scanner.energy().insert(512000);
        scanner.inventory().set(0, ItemResource.of(new ItemStack(Items.IRON_INGOT)), 1);
        for (int tick = 0; tick < 3400; tick++) {
            if (tick % 1000 == 0) scanner.energy().insert(512000);
            scanner.serverTick(helper.getLevel());
        }
        helper.assertTrue(
                "COMPLETED".equals(scanner.state()),
                "The scan finishes with a pattern storage adjacent");
        helper.assertTrue(
                scanner.menuAction(1), "The record button saves into the adjacent storage");
        helper.assertTrue(
                storage.getPatterns().size() == 1
                        && storage.getPatterns().get(0).is(Items.IRON_INGOT),
                "Without a disk the recorded pattern lands in the adjacent storage");
        helper.assertTrue(scanner.inventory().stack(0).isEmpty(), "The scanned item is consumed");
        helper.succeed();
    }

    static void deduplicatesPatterns(GameTestHelper helper) {
        var storage = storage(helper);
        helper.assertTrue(
                storage.addPattern(new ItemStack(Items.IRON_INGOT)),
                "The first pattern is accepted");
        helper.assertTrue(
                !storage.addPattern(new ItemStack(Items.IRON_INGOT)),
                "The same pattern is rejected as a duplicate");
        helper.assertTrue(
                storage.addPattern(new ItemStack(Items.GOLD_INGOT)),
                "A different pattern is accepted");
        helper.succeed();
    }

    static void writesPatternBackToDisk(GameTestHelper helper) {
        var storage = storage(helper);
        storage.addPattern(new ItemStack(Items.IRON_INGOT));
        storage.addPattern(new ItemStack(Items.GOLD_INGOT));
        storage.inventory()
                .set(
                        0,
                        ItemResource.of(ModReactorItems.CRYSTAL_MEMORY.get().getDefaultInstance()),
                        1);
        helper.assertTrue(
                storage.menuAction(2) && !storage.diskStack().isEmpty(),
                "The selected pattern writes onto the crystal memory in the disk slot");
        helper.succeed();
    }

    private static PatternStorageBlockEntity storage(GameTestHelper helper) {
        helper.setBlock(
                SCANNER_POS, ModMachines.block(MachineKind.PATTERN_STORAGE).defaultBlockState());
        return helper.getBlockEntity(SCANNER_POS, PatternStorageBlockEntity.class);
    }

    private PatternStorageTests() {}
}
