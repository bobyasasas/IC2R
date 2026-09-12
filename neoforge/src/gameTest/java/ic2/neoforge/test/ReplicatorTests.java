package ic2.neoforge.test;

import ic2.neoforge.item.UpgradeItem;
import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.machine.PatternStorageBlockEntity;
import ic2.neoforge.machine.ReplicatorBlockEntity;
import ic2.neoforge.registration.ModCells;
import ic2.neoforge.registration.ModFluids;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.registration.ModUpgrades;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

final class ReplicatorTests {
    private static final BlockPos POSITION = new BlockPos(8, 8, 8);

    private static ReplicatorBlockEntity replicator(GameTestHelper helper) {
        helper.setBlock(POSITION, ModMachines.block(MachineKind.REPLICATOR).defaultBlockState());
        return helper.getBlockEntity(POSITION, ReplicatorBlockEntity.class);
    }

    private static PatternStorageBlockEntity storage(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, ModMachines.block(MachineKind.PATTERN_STORAGE).defaultBlockState());
        return helper.getBlockEntity(pos, PatternStorageBlockEntity.class);
    }

    private static void pourCell(ReplicatorBlockEntity replicator) {
        replicator
                .inventory()
                .set(
                        0,
                        ItemResource.of(
                                ModCells.CELLS.get("uu_matter_cell").get().getDefaultInstance()),
                        1);
    }

    private static void pourTank(ReplicatorBlockEntity replicator, int mB) {
        try (var transaction = Transaction.openRoot()) {
            replicator
                    .tankView()
                    .insert(
                            0,
                            net.neoforged.neoforge.transfer.fluid.FluidResource.of(
                                    ModFluids.FAMILIES
                                            .get(ic2.neoforge.fluid.FluidDefinition.UU_MATTER)
                                            .source()
                                            .get()),
                            mB,
                            transaction);
            transaction.commit();
        }
    }

    static void replicatesPatternFromStorage(GameTestHelper helper) {
        var replicator = replicator(helper);
        var storage = storage(helper, POSITION.north());
        // Cobblestone has the world-scan base value 1.0: 0.01 mB of UU-matter, one tick.
        storage.addPattern(new ItemStack(Items.COBBLESTONE));
        // Feed one UU-matter cell through the fluid slot: it fills 1000 mB.
        pourCell(replicator);
        replicator.serverTick(helper.getLevel());
        helper.assertTrue(
                replicator.tankAmount() == 1000, "The UU cell pours 1000 mB into the tank");
        helper.assertTrue(
                !replicator.inventory().stack(1).isEmpty(),
                "The drained cell collects in the cell slot");
        replicator.energy().insert(512 * 4);
        // Legacy onNetworkEvent ids: browse (0/1) is legal while stopped, 2/3 unused here,
        // 4 starts single mode.
        helper.assertTrue(replicator.menuAction(1), "Browse is legal while stopped");
        helper.assertTrue(!replicator.menuAction(2), "Legacy ids leave 2 unused");
        helper.assertTrue(replicator.menuAction(4), "Single mode starts (legacy button 4)");
        replicator.serverTick(helper.getLevel());
        var output = replicator.inventory().stack(2);
        helper.assertTrue(
                output.is(Items.COBBLESTONE) && output.getCount() == 1,
                "The replicator materialises the pattern item");
        helper.assertTrue(
                replicator.tankAmount() == 999,
                "One mB covers the sub-mB work, saw " + replicator.tankAmount());
        helper.assertTrue(replicator.mode() == 0, "Single mode stops after one replication");
        helper.succeed();
    }

    static void replicatorChargesValueDerivedUu(GameTestHelper helper) {
        var replicator = replicator(helper);
        var storage = storage(helper, POSITION.north());
        storage.addPattern(new ItemStack(Items.IRON_INGOT));
        double requiredMb =
                ic2.neoforge.uu.UuValues.graph(helper.getLevel()).get("minecraft:iron_ingot")
                        / 100.0;
        helper.assertTrue(
                requiredMb > 13_000 && requiredMb < 13_500,
                "The iron ingot costs its world-scan value / 100 mB, saw " + requiredMb);
        int wanted = (int) Math.ceil(requiredMb);
        pourTank(replicator, wanted);
        replicator.energy().insert(2_000_000);
        helper.assertTrue(replicator.menuAction(4), "Single mode starts (legacy button 4)");
        int ticks = (int) Math.ceil(requiredMb / 0.1);
        for (int tick = 0; tick <= ticks; tick++) {
            if (tick % 2000 == 0) replicator.energy().insert(2_000_000);
            replicator.serverTick(helper.getLevel());
        }
        var output = replicator.inventory().stack(2);
        helper.assertTrue(
                output.is(Items.IRON_INGOT) && output.getCount() == 1,
                "The value-derived run materialises the ingot");
        helper.assertTrue(
                replicator.tankAmount() == 0,
                "Exactly ceil(value / 100) mB were drained, saw drained "
                        + (wanted - replicator.tankAmount())
                        + " wanted "
                        + wanted);
        helper.assertTrue(replicator.mode() == 0, "Single mode stops after the run");
        helper.succeed();
    }

    static void modeStopsWithoutUu(GameTestHelper helper) {
        var replicator = replicator(helper);
        var storage = storage(helper, POSITION.north());
        storage.addPattern(new ItemStack(Items.GOLD_INGOT));
        replicator.energy().insert(512 * 8);
        helper.assertTrue(replicator.menuAction(5), "Continuous mode starts (legacy button 5)");
        for (int tick = 0; tick < 20; tick++) replicator.serverTick(helper.getLevel());
        helper.assertTrue(
                replicator.mode() == 2, "The continuous mode remains selected without UU supply");
        helper.assertTrue(
                replicator.inventory().stack(2).isEmpty(),
                "Nothing replicates without UU-matter in the tank");
        helper.assertTrue(
                replicator.menuAction(3) && replicator.mode() == 0,
                "The stop button (legacy button 3) clears the mode");
        helper.succeed();
    }

    static void overclockScalesRates(GameTestHelper helper) {
        var replicator = replicator(helper);
        var storage = storage(helper, POSITION.north());
        storage.addPattern(new ItemStack(Items.COBBLESTONE));
        var overclocker = ModUpgrades.ALL.get(UpgradeItem.Kind.OVERCLOCKER).toStack();
        replicator.inventory().set(3, ItemResource.of(overclocker), 1);
        var storageUpgrade = ModUpgrades.ALL.get(UpgradeItem.Kind.ENERGY_STORAGE).toStack();
        replicator.inventory().set(4, ItemResource.of(storageUpgrade), 1);
        replicator.serverTick(helper.getLevel());
        helper.assertTrue(
                replicator.energy().capacity() == 2_010_000,
                "The energy storage upgrade adds 10000 EU of capacity, saw "
                        + replicator.energy().capacity());
        // One overclocker: 512 * 1.6 = 819.2 EU per tick; the cobble run still ends this tick.
        pourTank(replicator, 1000);
        replicator.energy().insert(819.2);
        helper.assertTrue(replicator.menuAction(4), "Single mode starts");
        replicator.serverTick(helper.getLevel());
        helper.assertTrue(
                replicator.energy().stored() == 0,
                "The overclocked tick draws exactly 819.2 EU, saw "
                        + replicator.energy().stored());
        helper.assertTrue(
                replicator.inventory().stack(2).is(Items.COBBLESTONE),
                "The overclocked run still completes on the first tick");
        helper.assertTrue(replicator.tankAmount() == 999, "The overclocked run drains 0.01 mB");
        helper.succeed();
    }

    static void persistenceCarriesRunState(GameTestHelper helper) {
        var replicator = replicator(helper);
        var storage = storage(helper, POSITION.north());
        storage.addPattern(new ItemStack(Items.IRON_INGOT));
        pourTank(replicator, 1000);
        replicator.energy().insert(2_000_000);
        helper.assertTrue(replicator.menuAction(4), "Single mode starts");
        for (int tick = 0; tick < 10; tick++) replicator.serverTick(helper.getLevel());
        helper.assertTrue(
                replicator.mode() == 1 && Math.abs(replicator.uuProcessed() - 1.0) < 1.0E-9,
                "Ten ticks process one mB of work, saw " + replicator.uuProcessed());
        helper.assertTrue(replicator.tankAmount() == 999, "One whole mB left the tank");
        helper.assertTrue(
                Math.abs(replicator.uuBank()) < 1.0E-9,
                "The bank has served its fractional cycle, saw " + replicator.uuBank());
        var restored =
                (ReplicatorBlockEntity)
                        BlockEntity.loadStatic(
                                replicator.getBlockPos(),
                                replicator.getBlockState(),
                                replicator.saveWithFullMetadata(helper.getLevel().registryAccess()),
                                helper.getLevel().registryAccess());
        restored.setLevel(helper.getLevel());
        restored.serverTick(helper.getLevel());
        helper.assertTrue(
                restored.mode() == 1 && Math.abs(restored.uuProcessed() - 1.1) < 1.0E-9,
                "The reload resumes the run at its saved progress, saw " + restored.uuProcessed());
        helper.assertTrue(
                restored.tankAmount() == 998,
                "The tank charge survives the reload and the resumed tick drains, saw "
                        + restored.tankAmount());
        helper.succeed();
    }

    static void browseStopAndPatternReset(GameTestHelper helper) {
        var replicator = replicator(helper);
        var storage = storage(helper, POSITION.north());
        storage.addPattern(new ItemStack(Items.IRON_INGOT));
        storage.addPattern(new ItemStack(Items.COBBLESTONE));
        pourTank(replicator, 1000);
        replicator.energy().insert(512 * 64);
        helper.assertTrue(replicator.menuAction(4), "Single mode starts on the first pattern");
        helper.assertTrue(
                !replicator.menuAction(1),
                "Browsing is refused while the machine runs (legacy STOPPED gate)");
        for (int tick = 0; tick < 5; tick++) replicator.serverTick(helper.getLevel());
        helper.assertTrue(Math.abs(replicator.uuProcessed() - 0.5) < 1.0E-9, "Iron progresses");
        helper.assertTrue(replicator.menuAction(3), "The stop button answers");
        helper.assertTrue(
                replicator.mode() == 0 && replicator.uuProcessed() == 0,
                "Stopping clears the mode and the progress (legacy button 3)");
        helper.assertTrue(replicator.menuAction(1), "Browsing is legal again while stopped");
        helper.assertTrue(
                replicator.menuValue(2) == 2,
                "The browse moved to the second pattern, saw " + replicator.menuValue(2));
        replicator.serverTick(helper.getLevel());
        helper.assertTrue(
                replicator.mode() == 0 && replicator.uuProcessed() == 0,
                "Switching patterns stops and resets (legacy refreshInfo)");
        helper.assertTrue(replicator.menuAction(4), "Single mode restarts on the cobble pattern");
        replicator.serverTick(helper.getLevel());
        helper.assertTrue(
                replicator.inventory().stack(2).is(Items.COBBLESTONE),
                "The browsed-to pattern is the one that materialises");
        helper.assertTrue(replicator.tankAmount() == 999, "The cheap pattern still banks 1 mB");
        helper.succeed();
    }

    static void valuelessPatternDrainsForever(GameTestHelper helper) {
        var replicator = replicator(helper);
        var storage = storage(helper, POSITION.north());
        // Nether star: known to no recipe resolver, valued at infinity (legacy quirk: the
        // replicator keeps burning UU-matter for it forever instead of finishing).
        storage.addPattern(new ItemStack(Items.NETHER_STAR));
        pourTank(replicator, 1000);
        replicator.energy().insert(512 * 64);
        helper.assertTrue(replicator.menuAction(5), "Continuous mode starts");
        for (int tick = 0; tick < 20; tick++) replicator.serverTick(helper.getLevel());
        helper.assertTrue(replicator.mode() == 2, "The endless run never completes");
        helper.assertTrue(
                replicator.inventory().stack(2).isEmpty(),
                "No nether star materialises from the valueless pattern");
        helper.assertTrue(
                replicator.tankAmount() == 998,
                "Twenty ticks drain exactly two whole mB, saw " + replicator.tankAmount());
        helper.succeed();
    }

    private ReplicatorTests() {}
}
