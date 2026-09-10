package ic2.neoforge.test;

import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.machine.PatternStorageBlockEntity;
import ic2.neoforge.machine.ReplicatorBlockEntity;
import ic2.neoforge.registration.ModCells;
import ic2.neoforge.registration.ModMachines;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.transfer.item.ItemResource;

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

    static void replicatesPatternFromStorage(GameTestHelper helper) {
        var replicator = replicator(helper);
        var storage = storage(helper, POSITION.north());
        // Cobblestone has the world-scan base value 1.0: 0.01 mB of UU-matter, one tick.
        storage.addPattern(new ItemStack(Items.COBBLESTONE));
        // Feed one UU-matter cell through the fluid slot: it fills 1000 mB.
        replicator
                .inventory()
                .set(
                        0,
                        ItemResource.of(
                                ModCells.CELLS.get("uu_matter_cell").get().getDefaultInstance()),
                        1);
        replicator.serverTick(helper.getLevel());
        helper.assertTrue(
                replicator.tankAmount() == 1000, "The UU cell pours 1000 mB into the tank");
        helper.assertTrue(
                !replicator.inventory().stack(1).isEmpty(),
                "The drained cell collects in the cell slot");
        replicator.energy().insert(512 * 4);
        helper.assertTrue(replicator.menuAction(2), "Single mode starts");
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
        try (var transaction = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
            replicator
                    .tankView()
                    .insert(
                            0,
                            net.neoforged.neoforge.transfer.fluid.FluidResource.of(
                                    ic2.neoforge.registration.ModFluids.FAMILIES
                                            .get(ic2.neoforge.fluid.FluidDefinition.UU_MATTER)
                                            .source()
                                            .get()),
                            wanted,
                            transaction);
            transaction.commit();
        }
        replicator.energy().insert(2_000_000);
        helper.assertTrue(replicator.menuAction(2), "Single mode starts");
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
        helper.assertTrue(replicator.menuAction(3), "Continuous mode starts");
        for (int tick = 0; tick < 20; tick++) replicator.serverTick(helper.getLevel());
        helper.assertTrue(
                replicator.mode() == 2, "The continuous mode remains selected without UU supply");
        helper.assertTrue(
                replicator.inventory().stack(2).isEmpty(),
                "Nothing replicates without UU-matter in the tank");
        helper.succeed();
    }

    private ReplicatorTests() {}
}
