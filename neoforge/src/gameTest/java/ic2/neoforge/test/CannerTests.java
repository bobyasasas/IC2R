package ic2.neoforge.test;

import ic2.core.machine.CannerMode;
import ic2.neoforge.fluid.FluidDefinition;
import ic2.neoforge.machine.CannerBlockEntity;
import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.menu.MachineMenu;
import ic2.neoforge.registration.*;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;

final class CannerTests {
    private static CannerBlockEntity machine(GameTestHelper helper) {
        return new CannerBlockEntity(
                helper.absolutePos(new BlockPos(1, 1, 1)),
                ModMachines.block(MachineKind.CANNER).defaultBlockState());
    }

    private static void ticks(CannerBlockEntity machine, GameTestHelper helper, int count) {
        for (int tick = 0; tick < count; tick++) machine.serverTick(helper.getLevel());
    }

    static void solid(GameTestHelper helper) {
        var machine = machine(helper);
        machine.energy().insert(800);
        machine.inventory().set(0, ItemResource.of(Items.MUTTON), 1);
        machine.inventory()
                .set(
                        3,
                        ItemResource.of(ModItems.MATERIALS.get(MaterialDefinition.TIN_CAN).get()),
                        2);
        ticks(machine, helper, 200);
        helper.assertTrue(
                machine.inventory().stack(1).is(ModItems.FILLED_TIN_CAN.get())
                        && machine.inventory().stack(1).getCount() == 2,
                "Solid canning must preserve the recipe's container count and output");
        helper.assertTrue(
                machine.energy().stored() == 0
                        && machine.inventory().stack(0).isEmpty()
                        && machine.inventory().stack(3).isEmpty(),
                "One operation must consume exactly 800 EU, two cans and one additive");
        helper.succeed();
    }

    static void fillAndEmpty(GameTestHelper helper) {
        var machine = machine(helper);
        machine.setMode(CannerMode.BOTTLE_LIQUID);
        machine.energy().insert(800);
        machine.inputTank().set(0, FluidResource.of(Fluids.WATER), 1000);
        machine.inventory().set(3, ItemResource.of(ModCells.EMPTY.get()), 64);
        ticks(machine, helper, 200);
        helper.assertTrue(
                machine.inventory().stack(1).is(ModCells.WATER.get())
                        && machine.inventory().stack(3).getCount() == 63,
                "Filling must consume exactly one stacked empty cell");
        helper.assertTrue(
                machine.inputTank().getAmountAsInt(0) == 0 && machine.energy().stored() == 0,
                "Filling must consume exactly one bucket and 800 EU");
        machine.setMode(CannerMode.EMPTY_LIQUID);
        machine.energy().insert(800);
        machine.inventory().set(1, ItemResource.EMPTY, 0);
        machine.inventory().set(3, ItemResource.of(ModCells.WATER.get()), 1);
        ticks(machine, helper, 200);
        helper.assertTrue(
                machine.inventory().stack(1).is(ModCells.EMPTY.get())
                        && machine.inventory().stack(3).isEmpty(),
                "Draining must return its empty cell");
        helper.assertTrue(
                machine.outputTank().getAmountAsInt(0) == 1000
                        && machine.outputTank()
                                .getResource(0)
                                .equals(FluidResource.of(Fluids.WATER)),
                "Draining must put all fluid in the output tank");
        helper.succeed();
    }

    static void enrichmentRollback(GameTestHelper helper) {
        var machine = machine(helper);
        var water = FluidResource.of(Fluids.WATER);
        machine.setMode(CannerMode.ENRICH_LIQUID);
        machine.energy().insert(800);
        machine.inputTank().set(0, water, 1000);
        machine.outputTank().set(0, water, 8000);
        machine.inventory()
                .set(
                        0,
                        ItemResource.of(
                                ModItems.MATERIALS.get(MaterialDefinition.LAPIS_DUST).get()),
                        8);
        machine.inventory().set(3, ItemResource.of(ModCells.EMPTY.get()), 1);
        machine.inventory().set(1, ItemResource.of(Items.COBBLESTONE), 64);
        ticks(machine, helper, 10);
        helper.assertTrue(
                machine.progress() == 0
                        && machine.energy().stored() == 800
                        && machine.inputTank().getAmountAsInt(0) == 1000
                        && machine.inventory().stack(0).getCount() == 8,
                "Blocked enrichment must roll back additive, containers, fluid and EU");
        machine.inventory().set(1, ItemResource.EMPTY, 0);
        ticks(machine, helper, 200);
        helper.assertTrue(
                machine.inventory().stack(1).is(ModCells.CELLS.get("coolant_cell").get()),
                "Enrichment may fill a container even if the output tank holds a different fluid");
        helper.assertTrue(
                machine.outputTank().getAmountAsInt(0) == 8000
                        && machine.outputTank().getResource(0).equals(water),
                "Fully bottled output must leave the output tank untouched");
        helper.assertTrue(
                machine.inputTank().getAmountAsInt(0) == 0
                        && machine.inventory().stack(0).isEmpty()
                        && machine.energy().stored() == 0,
                "Successful enrichment consumes exact inputs once");
        helper.succeed();
    }

    static void stateAndButtons(GameTestHelper helper) {
        var machine = machine(helper);
        machine.setMode(CannerMode.BOTTLE_LIQUID);
        machine.energy().insert(800);
        machine.inputTank().set(0, FluidResource.of(Fluids.WATER), 1000);
        machine.inventory().set(3, ItemResource.of(ModCells.EMPTY.get()), 1);
        ticks(machine, helper, 100);
        helper.assertTrue(
                machine.progress() == 100 && !machine.swapTanks(),
                "Tanks cannot be swapped during an active operation");
        var saved = machine.saveWithFullMetadata(helper.getLevel().registryAccess());
        var restored =
                (CannerBlockEntity)
                        BlockEntity.loadStatic(
                                machine.getBlockPos(),
                                machine.getBlockState(),
                                saved,
                                helper.getLevel().registryAccess());
        helper.assertTrue(
                restored.mode() == CannerMode.BOTTLE_LIQUID && restored.progress() == 100,
                "Mode and progress must survive restart");
        ticks(restored, helper, 100);
        helper.assertTrue(
                restored.inventory().stack(1).is(ModCells.WATER.get())
                        && restored.energy().stored() == 0,
                "Reloaded canner must finish with its remaining 400 EU");
        restored.outputTank()
                .set(
                        0,
                        FluidResource.of(
                                ModFluids.FAMILIES.get(FluidDefinition.COOLANT).source().get()),
                        1000);
        helper.assertTrue(
                restored.swapTanks()
                        && restored.inputTank().getAmountAsInt(0) == 1000
                        && restored.outputTank().getAmountAsInt(0) == 0,
                "Idle tank swapping must exchange complete contents");
        var pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModMachines.block(MachineKind.CANNER));
        var placed = helper.getBlockEntity(pos, CannerBlockEntity.class);
        var player = helper.makeMockPlayer(GameType.CREATIVE);
        player.setPos(
                placed.getBlockPos().getX() + .5,
                placed.getBlockPos().getY(),
                placed.getBlockPos().getZ() + .5);
        var menu = new MachineMenu(1, player.getInventory(), placed);
        helper.assertTrue(
                !menu.clickMenuButton(player, 1),
                "Only the player's current menu may receive actions");
        player.containerMenu = menu;
        helper.assertTrue(!menu.clickMenuButton(player, 99), "Invalid mode IDs must be rejected");
        helper.assertTrue(
                menu.clickMenuButton(player, 1) && placed.mode() == CannerMode.EMPTY_LIQUID,
                "Valid server action must update the typed mode");
        helper.assertTrue(
                menu.slots.size() == 40,
                "Canner menu must include four machine slots plus player inventory");
        player.setPos(player.getX() + 20, player.getY(), player.getZ());
        helper.assertTrue(
                !menu.clickMenuButton(player, 2), "Distant mode changes must be rejected");
        helper.succeed();
    }

    private CannerTests() {}
}
