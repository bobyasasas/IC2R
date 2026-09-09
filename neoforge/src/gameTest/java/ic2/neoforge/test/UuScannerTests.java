package ic2.neoforge.test;

import ic2.neoforge.item.CrystalMemoryItem;
import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.machine.UuScannerBlockEntity;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.registration.ModReactorItems;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.transfer.item.ItemResource;

final class UuScannerTests {
    private static final BlockPos POSITION = new BlockPos(8, 8, 8);

    private static UuScannerBlockEntity scanner(GameTestHelper helper) {
        helper.setBlock(POSITION, ModMachines.block(MachineKind.UU_SCANNER).defaultBlockState());
        return helper.getBlockEntity(POSITION, UuScannerBlockEntity.class);
    }

    static void scansSeededItemOntoMemory(GameTestHelper helper) {
        var scanner = scanner(helper);
        scanner.energy().insert(512000);
        scanner.inventory().set(0, ItemResource.of(new ItemStack(Items.IRON_INGOT)), 1);
        scanner.inventory()
                .set(
                        1,
                        ItemResource.of(ModReactorItems.CRYSTAL_MEMORY.get().getDefaultInstance()),
                        1);
        for (int tick = 0; tick < 3400; tick++) {
            if (tick % 1000 == 0) scanner.energy().insert(512000);
            scanner.serverTick(helper.getLevel());
        }
        var memory = scanner.inventory().stack(1);
        var recorded = ((CrystalMemoryItem) memory.getItem()).readPattern(memory);
        helper.assertTrue(
                recorded.is(Items.IRON_INGOT),
                "The scanner records the scanned iron ingot, input="
                        + scanner.inventory().stack(0)
                        + " memory="
                        + memory
                        + " state="
                        + scanner.state()
                        + " progress="
                        + scanner.progress()
                        + " energy="
                        + scanner.energy().stored());
        helper.assertTrue(scanner.inventory().stack(0).isEmpty(), "The scanned item is consumed");
        helper.succeed();
    }

    static void unknownItemFails(GameTestHelper helper) {
        var scanner = scanner(helper);
        scanner.energy().insert(512000);
        scanner.inventory()
                .set(
                        1,
                        ItemResource.of(ModReactorItems.CRYSTAL_MEMORY.get().getDefaultInstance()),
                        1);
        scanner.inventory().set(0, ItemResource.of(new ItemStack(Items.DIAMOND)), 1);
        for (int tick = 0; tick < 3400; tick++) scanner.serverTick(helper.getLevel());
        helper.assertTrue(
                !scanner.inventory().stack(0).isEmpty(),
                "An item without a UU value stays in the input slot");
        helper.assertTrue(
                scanner.inventory().stack(1).is(ModReactorItems.CRYSTAL_MEMORY.get()),
                "The crystal memory stays blank for unknown items");
        helper.succeed();
    }

    static void expandedSeedCoverage(GameTestHelper helper) {
        var scanner = scanner(helper);
        scanner.energy().insert(512000);
        // Wheat: new seed; its value flows through the macerator/crop transformations.
        scanner.inventory().set(0, ItemResource.of(new ItemStack(Items.WHEAT)), 1);
        scanner.inventory()
                .set(
                        1,
                        ItemResource.of(ModReactorItems.CRYSTAL_MEMORY.get().getDefaultInstance()),
                        1);
        for (int tick = 0; tick < 3400; tick++) {
            if (tick % 1000 == 0) scanner.energy().insert(512000);
            scanner.serverTick(helper.getLevel());
        }
        var memory = scanner.inventory().stack(1);
        var recorded = ((CrystalMemoryItem) memory.getItem()).readPattern(memory);
        helper.assertTrue(
                recorded.is(Items.WHEAT), "The expanded seed table makes crops scannable");
        helper.succeed();
    }

    private UuScannerTests() {}
}
