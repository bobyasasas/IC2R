package ic2.neoforge.test;

import ic2.neoforge.component.ModDataComponents;
import ic2.neoforge.item.CrystalMemoryItem;
import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.machine.PatternStorageBlockEntity;
import ic2.neoforge.machine.UuScannerBlockEntity;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.registration.ModReactorItems;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

final class UuScannerTests {
    private static final BlockPos POSITION = new BlockPos(8, 8, 8);

    private static UuScannerBlockEntity scanner(GameTestHelper helper) {
        helper.setBlock(POSITION, ModMachines.block(MachineKind.UU_SCANNER).defaultBlockState());
        return helper.getBlockEntity(POSITION, UuScannerBlockEntity.class);
    }

    private static PatternStorageBlockEntity storage(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, ModMachines.block(MachineKind.PATTERN_STORAGE).defaultBlockState());
        return helper.getBlockEntity(pos, PatternStorageBlockEntity.class);
    }

    private static ItemStack blankMemory() {
        return ModReactorItems.CRYSTAL_MEMORY.get().getDefaultInstance();
    }

    /** A full 3300-tick scan with periodic energy top-ups (legacy 256 EU/tick). */
    private static void runScan(UuScannerBlockEntity scanner, GameTestHelper helper) {
        for (int tick = 0; tick < 3400; tick++) {
            if (tick % 1000 == 0) scanner.energy().insert(512000);
            scanner.serverTick(helper.getLevel());
        }
    }

    private static String readPattern(ItemStack memory) {
        return ((CrystalMemoryItem) memory.getItem()).readPattern(memory).getItem().toString();
    }

    static void scansSeededItemOntoMemory(GameTestHelper helper) {
        var scanner = scanner(helper);
        scanner.energy().insert(512000);
        scanner.inventory().set(0, ItemResource.of(new ItemStack(Items.IRON_INGOT)), 1);
        scanner.inventory().set(1, ItemResource.of(blankMemory()), 1);
        helper.assertTrue(
                !scanner.menuAction(1), "The record button refuses an unfinished scan");
        runScan(scanner, helper);
        helper.assertTrue(
                "COMPLETED".equals(scanner.state()),
                "The scan completes after 3300 ticks, state="
                        + scanner.state()
                        + " progress="
                        + scanner.progress());
        helper.assertTrue(scanner.inventory().stack(0).isEmpty(), "The scanned item is consumed");
        helper.assertTrue(
                scanner.inventory().stack(1).is(ModReactorItems.CRYSTAL_MEMORY.get())
                        && ((CrystalMemoryItem) scanner.inventory().stack(1).getItem())
                                .readPattern(scanner.inventory().stack(1))
                                .isEmpty(),
                "Legacy two-phase completion: the finished scan waits for the record button");
        helper.assertTrue(scanner.menuAction(1), "The record button accepts the finished scan");
        var memory = scanner.inventory().stack(1);
        helper.assertTrue(
                Items.IRON_INGOT.toString().equals(readPattern(memory)),
                "The scanner records the scanned iron ingot onto the memory");
        double expected =
                ic2.neoforge.uu.UuValues.graph(helper.getLevel()).get("minecraft:iron_ingot")
                        * 1.0E-5;
        Double value = memory.get(ModDataComponents.CRYSTAL_MEMORY_VALUE);
        helper.assertTrue(
                value != null && Math.abs(value - expected) < 1.0E-9,
                "The record snapshots the bucket value, saw "
                        + value
                        + " expected "
                        + expected);
        helper.succeed();
    }

    static void unknownItemFails(GameTestHelper helper) {
        var scanner = scanner(helper);
        scanner.energy().insert(512000);
        scanner.inventory().set(1, ItemResource.of(blankMemory()), 1);
        scanner.inventory().set(0, ItemResource.of(new ItemStack(Items.NETHER_STAR)), 1);
        scanner.serverTick(helper.getLevel());
        helper.assertTrue(
                "FAILED".equals(scanner.state()),
                "An item outside the UU graph fails on the first tick");
        helper.assertTrue(
                scanner.energy().stored() == 512000, "The instant failure burns no energy");
        for (int tick = 0; tick < 3400; tick++) scanner.serverTick(helper.getLevel());
        helper.assertTrue(
                !scanner.inventory().stack(0).isEmpty(),
                "An item without a UU value stays in the input slot");
        helper.assertTrue(
                scanner.inventory().stack(1).is(ModReactorItems.CRYSTAL_MEMORY.get()),
                "The crystal memory stays blank for unknown items");
        helper.assertTrue(scanner.progress() == 0, "The failed scan keeps no progress");
        helper.succeed();
    }

    static void expandedSeedCoverage(GameTestHelper helper) {
        var scanner = scanner(helper);
        scanner.energy().insert(512000);
        // Wheat: new seed; its value flows through the macerator/crop transformations.
        scanner.inventory().set(0, ItemResource.of(new ItemStack(Items.WHEAT)), 1);
        scanner.inventory().set(1, ItemResource.of(blankMemory()), 1);
        runScan(scanner, helper);
        helper.assertTrue("COMPLETED".equals(scanner.state()), "The wheat scan completes");
        helper.assertTrue(scanner.menuAction(1), "The finished scan records");
        helper.assertTrue(
                Items.WHEAT.toString().equals(readPattern(scanner.inventory().stack(1))),
                "The expanded seed table makes crops scannable");
        helper.succeed();
    }

    static void alreadyRecordedSkipsRescan(GameTestHelper helper) {
        var scanner = scanner(helper);
        var storage = storage(helper, POSITION.north());
        storage.addPattern(new ItemStack(Items.IRON_INGOT));
        scanner.energy().insert(512000);
        scanner.inventory().set(0, ItemResource.of(new ItemStack(Items.IRON_INGOT)), 1);
        scanner.serverTick(helper.getLevel());
        helper.assertTrue(
                "ALREADY_RECORDED".equals(scanner.state()),
                "A pattern already on record blocks the rescan");
        helper.assertTrue(scanner.energy().stored() == 512000, "The skip burns no energy");
        helper.assertTrue(scanner.progress() == 0, "The skip keeps no progress");
        helper.assertTrue(!scanner.inventory().stack(0).isEmpty(), "The input survives the skip");
        helper.succeed();
    }

    static void persistenceCarriesScanProgress(GameTestHelper helper) {
        var scanner = scanner(helper);
        scanner.energy().insert(512000);
        scanner.inventory().set(0, ItemResource.of(new ItemStack(Items.IRON_INGOT)), 1);
        scanner.inventory().set(1, ItemResource.of(blankMemory()), 1);
        for (int tick = 0; tick < 100; tick++) scanner.serverTick(helper.getLevel());
        helper.assertTrue(
                scanner.progress() == 100 && "SCANNING".equals(scanner.state()),
                "The scan is mid-flight before the reload");
        var restored =
                (UuScannerBlockEntity)
                        BlockEntity.loadStatic(
                                scanner.getBlockPos(),
                                scanner.getBlockState(),
                                scanner.saveWithFullMetadata(helper.getLevel().registryAccess()),
                                helper.getLevel().registryAccess());
        restored.setLevel(helper.getLevel());
        helper.assertTrue(restored.progress() == 100, "The scan progress survives a reload");
        helper.assertTrue("SCANNING".equals(restored.state()), "The scan state survives a reload");
        helper.assertTrue(
                !restored.inventory().stack(0).isEmpty()
                        && restored.inventory().stack(0).is(Items.IRON_INGOT),
                "The snapshot of the scanned item survives a reload");
        for (int tick = 0; tick < 3300; tick++) {
            if (tick % 1000 == 0) restored.energy().insert(512000);
            restored.serverTick(helper.getLevel());
        }
        helper.assertTrue(
                "COMPLETED".equals(restored.state()), "The restored scan runs to completion");
        helper.assertTrue(restored.menuAction(1), "The restored scan records");
        helper.assertTrue(
                Items.IRON_INGOT.toString().equals(readPattern(restored.inventory().stack(1))),
                "The restored scan records the same item");
        helper.succeed();
    }

    static void inputChangeRestartsScan(GameTestHelper helper) {
        var scanner = scanner(helper);
        scanner.energy().insert(512000);
        scanner.inventory().set(0, ItemResource.of(new ItemStack(Items.IRON_INGOT)), 1);
        scanner.inventory().set(1, ItemResource.of(blankMemory()), 1);
        for (int tick = 0; tick < 50; tick++) scanner.serverTick(helper.getLevel());
        helper.assertTrue(scanner.progress() == 50, "The scan advances while the input holds");
        scanner.inventory().set(0, ItemResource.of(new ItemStack(Items.WHEAT)), 1);
        scanner.serverTick(helper.getLevel());
        helper.assertTrue(
                "IDLE".equals(scanner.state()) && scanner.progress() == 0,
                "A changed input restarts the scan (legacy currentStack guard)");
        helper.succeed();
    }

    static void recordFailureHoldsPattern(GameTestHelper helper) {
        var scanner = scanner(helper);
        scanner.energy().insert(512000);
        scanner.inventory().set(0, ItemResource.of(new ItemStack(Items.IRON_INGOT)), 1);
        scanner.inventory().set(1, ItemResource.of(blankMemory()), 1);
        runScan(scanner, helper);
        helper.assertTrue("COMPLETED".equals(scanner.state()), "The scan completes");
        // Pull the memory out through the automation port so neither target is available.
        try (var transaction = Transaction.openRoot()) {
            helper.assertTrue(
                    scanner.automation(net.minecraft.core.Direction.NORTH)
                            .extract(
                                    1,
                                    ItemResource.of(blankMemory()),
                                    1,
                                    transaction)
                            == 1,
                    "The finished scanner releases the memory through its item port");
            transaction.commit();
        }
        helper.assertTrue(scanner.menuAction(1), "The record press is still acknowledged");
        helper.assertTrue(
                "TRANSFER_ERROR".equals(scanner.state()),
                "With no memory and no storage the record fails (legacy TRANSFER_ERROR)");
        helper.assertTrue(
                scanner.progress() == UuScannerBlockEntity.SCANNER_TICKS,
                "The failed record holds the finished scan instead of wiping it");
        scanner.inventory().set(1, ItemResource.of(blankMemory()), 1);
        helper.assertTrue(scanner.menuAction(1), "A re-inserted memory records");
        helper.assertTrue(
                Items.IRON_INGOT.toString().equals(readPattern(scanner.inventory().stack(1))),
                "The held pattern records once a target is back");
        helper.succeed();
    }

    static void datapackSeedsDriveGraph(GameTestHelper helper) {
        var graph = ic2.neoforge.uu.UuValues.graph(helper.getLevel());
        // Cobblestone and dirt only exist in the shipped world-scan seed datapack; the built-in
        // fallback table does not contain them, so a finite value proves the reload listener
        // loaded data/ic2/uu_values/world_scan.json.
        helper.assertTrue(
                graph.get("minecraft:cobblestone") == 1.0,
                "The world-scan datapack seeds cobblestone at 1.0, saw "
                        + graph.get("minecraft:cobblestone"));
        helper.assertTrue(
                graph.get("minecraft:dirt") == 14.857483653272267,
                "Fractional world-scan values round-trip, saw "
                        + graph.get("minecraft:dirt"));
        // World-scan coverage: previously unscannable world blocks now carry values.
        helper.assertTrue(
                graph.knows("minecraft:sand"), "The shipped seed set expands the scan coverage");
        helper.succeed();
    }

    private UuScannerTests() {}
}
