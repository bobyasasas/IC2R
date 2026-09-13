package ic2.neoforge.test;

import ic2.neoforge.machine.BatchCrafterBlockEntity;
import ic2.neoforge.machine.IndustrialWorkbenchBlockEntity;
import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.menu.MachineMenu;
import ic2.neoforge.registration.ModMachines;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.transfer.item.ItemResource;

/** Legacy batch crafter and industrial workbench crafting semantics. */
final class CrafterTests {
    private CrafterTests() {}

    static void workbench(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModMachines.block(MachineKind.INDUSTRIAL_WORKBENCH));
        var workbench = helper.getBlockEntity(pos, IndustrialWorkbenchBlockEntity.class);
        workbench.inventory()
                .set(IndustrialWorkbenchBlockEntity.GRID + 4, ItemResource.of(Items.OAK_LOG), 1);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var menu = new MachineMenu(1, player.getInventory(), workbench);

        var resultSlot = menu.slots.get(31);
        helper.assertTrue(
                resultSlot instanceof ic2.neoforge.menu.CraftResultSlot,
                "Menu slot 31 must be the computed grid preview, got " + resultSlot.getClass());
        var preview = resultSlot.getItem();
        helper.assertTrue(
                preview.is(Items.OAK_PLANKS) && preview.getCount() == 4,
                "The grid preview slot must show four planks from one log");

        resultSlot.onTake(player, preview);
        var after = workbench.inventory().stack(IndustrialWorkbenchBlockEntity.GRID + 4);
        helper.assertTrue(
                after.isEmpty(),
                "Taking the preview must consume exactly one log from the grid, still "
                        + after);

        workbench.inventory().set(IndustrialWorkbenchBlockEntity.GRID, ItemResource.of(Items.COBBLESTONE), 5);
        helper.assertTrue(
                workbench.clearGrid(player)
                        && workbench.inventory().stack(IndustrialWorkbenchBlockEntity.GRID).isEmpty()
                        && workbench.inventory().stack(IndustrialWorkbenchBlockEntity.BUFFER).is(Items.COBBLESTONE)
                        && workbench.inventory().getAmountAsInt(IndustrialWorkbenchBlockEntity.BUFFER) == 5,
                "Clear must move grid contents into the buffer");
        helper.succeed();
    }

    static void batch(GameTestHelper helper) {
        var machine =
                new BatchCrafterBlockEntity(
                        helper.absolutePos(new BlockPos(1, 1, 1)),
                        ModMachines.block(MachineKind.BATCH_CRAFTER).defaultBlockState());
        machine.hologram().set(4, ItemResource.of(Items.OAK_LOG), 1);
        machine.inventory().set(4, ItemResource.of(Items.OAK_LOG), 3);
        machine.energy().insert(200);
        for (int tick = 0; tick < 40; tick++) machine.serverTick(helper.getLevel());

        var output = machine.inventory().stack(BatchCrafterBlockEntity.OUTPUT);
        helper.assertTrue(
                output.is(Items.OAK_PLANKS) && output.getCount() == 4,
                "One 40 tick operation must craft the templated recipe once, got output="
                        + output
                        + " energy="
                        + machine.energy().stored()
                        + " ingredient4="
                        + machine.inventory().stack(4)
                        + " holo4="
                        + machine.hologram().stack(4));
        helper.assertTrue(
                machine.inventory().getAmountAsInt(4) == 2,
                "One operation must consume exactly one ingredient stack item");
        helper.assertTrue(
                machine.energy().stored() == 120,
                "One operation must consume exactly 80 EU at 2 EU/t");
        helper.succeed();
    }
}
