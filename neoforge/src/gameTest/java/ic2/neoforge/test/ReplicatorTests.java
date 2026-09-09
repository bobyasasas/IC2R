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
        storage.addPattern(new ItemStack(Items.IRON_INGOT));
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
        for (int tick = 0; tick < 4; tick++) replicator.serverTick(helper.getLevel());
        var output = replicator.inventory().stack(2);
        helper.assertTrue(
                output.is(Items.IRON_INGOT) && output.getCount() == 1,
                "The replicator materialises the pattern item");
        helper.assertTrue(replicator.mode() == 0, "Single mode stops after one replication");
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
                replicator.mode() == 2,
                "The continuous mode remains selected without UU supply");
        helper.assertTrue(
                replicator.inventory().stack(2).isEmpty(),
                "Nothing replicates without UU-matter in the tank");
        helper.succeed();
    }

    private ReplicatorTests() {}
}
