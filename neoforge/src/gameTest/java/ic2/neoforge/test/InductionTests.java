package ic2.neoforge.test;

import ic2.neoforge.item.UpgradeItem;
import ic2.neoforge.machine.InductionFurnaceBlockEntity;
import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.menu.MachineMenu;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.registration.ModUpgrades;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.transfer.item.ItemResource;

final class InductionTests {
    static void twoRowsPersistence(GameTestHelper helper) {
        var machine = hotMachine(helper);
        machine.inventory().set(0, ItemResource.of(Items.RAW_IRON), 1);
        machine.inventory().set(3, ItemResource.of(Items.RAW_GOLD), 1);
        machine.energy().insert(1000);
        for (int tick = 0; tick < 12; tick++) machine.serverTick(helper.getLevel());
        helper.assertTrue(
                machine.progress() == 3996 && machine.comparator() == 15,
                "Hot furnace must share 333 work per tick between both rows");
        var restored =
                (InductionFurnaceBlockEntity)
                        BlockEntity.loadStatic(
                                machine.getBlockPos(),
                                machine.getBlockState(),
                                machine.saveWithFullMetadata(helper.getLevel().registryAccess()),
                                helper.getLevel().registryAccess());
        restored.serverTick(helper.getLevel());
        helper.assertTrue(
                restored.progress() == 4329 && restored.inventory().stack(1).isEmpty(),
                "Threshold is consumed at the following tick, including after reload");
        restored.serverTick(helper.getLevel());
        helper.assertTrue(
                restored.inventory().stack(1).is(Items.IRON_INGOT)
                        && restored.inventory().stack(4).is(Items.GOLD_INGOT)
                        && restored.inventory().stack(0).isEmpty()
                        && restored.inventory().stack(3).isEmpty(),
                "Both rows must complete their own smelting recipes exactly once");
        helper.assertTrue(
                restored.energy().stored() == 792
                        && restored.heat() == 9996
                        && restored.progress() == 0,
                "A hot two-row batch costs 208 EU and idle cooling starts after completion");
        helper.assertTrue(
                restored.energyNode().input().orElseThrow().voltage() == 128,
                "Induction furnace must remain MV");
        helper.succeed();
    }

    static void blockedRow(GameTestHelper helper) {
        var machine = hotMachine(helper);
        machine.inventory().set(0, ItemResource.of(Items.RAW_IRON), 1);
        machine.inventory().set(1, ItemResource.of(Items.DIRT), 64);
        machine.inventory().set(3, ItemResource.of(Items.RAW_GOLD), 1);
        machine.energy().insert(1000);
        for (int tick = 0; tick < 14; tick++) machine.serverTick(helper.getLevel());
        helper.assertTrue(
                machine.inventory().stack(0).is(Items.RAW_IRON)
                        && machine.inventory().stack(1).is(Items.DIRT)
                        && machine.inventory().stack(4).is(Items.GOLD_INGOT),
                "A blocked row must preserve its input while the other row completes");
        helper.assertTrue(
                machine.energy().stored() == 792,
                "Single-row work uses the same shared batch energy");
        helper.succeed();
    }

    static void menuAndUpgrades(GameTestHelper helper) {
        var pos = new BlockPos(3, 1, 3);
        helper.setBlock(pos, ModMachines.block(MachineKind.INDUCTION_FURNACE));
        var machine = helper.getBlockEntity(pos, InductionFurnaceBlockEntity.class);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var menu = new MachineMenu(1, player.getInventory(), machine);
        player.containerMenu = menu;
        machine.inventory().set(0, ItemResource.of(Items.RAW_IRON), 64);
        player.getInventory().setItem(0, Items.RAW_GOLD.getDefaultInstance());
        menu.quickMoveStack(player, 34);
        helper.assertTrue(
                machine.inventory().stack(3).is(Items.RAW_GOLD)
                        && player.getInventory().getItem(0).isEmpty(),
                "Shift-click must fall back to the second input when the first cannot accept it");
        player.getInventory().setItem(1, ModUpgrades.ALL.get(UpgradeItem.Kind.EJECTOR).toStack());
        menu.quickMoveStack(player, 35);
        helper.assertTrue(
                machine.inventory().stack(5).is(ModUpgrades.ALL.get(UpgradeItem.Kind.EJECTOR).get())
                        && menu.slots.size() == 43,
                "Induction furnace must expose exactly two upgrade slots and 36 player slots");
        helper.assertTrue(
                !UpgradeItem.Kind.OVERCLOCKER.suitable(machine.kind())
                        && !UpgradeItem.Kind.TRANSFORMER.suitable(machine.kind())
                        && !UpgradeItem.Kind.ENERGY_STORAGE.suitable(machine.kind()),
                "Unsupported upgrades must not change induction heat, storage or voltage");
        helper.succeed();
    }

    private static InductionFurnaceBlockEntity hotMachine(GameTestHelper helper) {
        var machine =
                new InductionFurnaceBlockEntity(
                        helper.absolutePos(new BlockPos(1, 1, 1)),
                        ModMachines.block(MachineKind.INDUCTION_FURNACE).defaultBlockState());
        var data = machine.saveWithFullMetadata(helper.getLevel().registryAccess());
        data.putInt("heat", 10000);
        return (InductionFurnaceBlockEntity)
                BlockEntity.loadStatic(
                        machine.getBlockPos(),
                        machine.getBlockState(),
                        data,
                        helper.getLevel().registryAccess());
    }

    private InductionTests() {}
}
