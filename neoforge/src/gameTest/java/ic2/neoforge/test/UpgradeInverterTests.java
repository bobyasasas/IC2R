package ic2.neoforge.test;

import ic2.neoforge.item.UpgradeItem;
import ic2.neoforge.machine.InductionFurnaceBlockEntity;
import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.machine.MatterGeneratorBlockEntity;
import ic2.neoforge.menu.MachineMenu;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.registration.ModUpgrades;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.transfer.item.ItemResource;

final class UpgradeInverterTests {
    private static final BlockPos POSITION = new BlockPos(8, 8, 8);

    static void inverterKeepsInductionWarm(GameTestHelper helper) {
        helper.setBlock(
                POSITION, ModMachines.block(MachineKind.INDUCTION_FURNACE).defaultBlockState());
        var machine = helper.getBlockEntity(POSITION, InductionFurnaceBlockEntity.class);
        machine.energy().insert(100);
        machine.serverTick(helper.getLevel());
        helper.assertTrue(
                machine.energy().stored() == 100 && machine.heat() == 0,
                "Without an inverter an idle signalless furnace keeps no heat and spends nothing");

        machine
                .inventory()
                .set(
                        5,
                        ItemResource.of(ModUpgrades.ALL.get(UpgradeItem.Kind.REDSTONE_INVERTER).get()),
                        1);
        machine.serverTick(helper.getLevel());
        helper.assertTrue(
                machine.energy().stored() == 99 && machine.heat() == 1,
                "Inverted absence of signal must act as a keep-warm redstone input (legacy 15 - 0)");

        helper.setBlock(POSITION.above(), Blocks.REDSTONE_BLOCK);
        machine.serverTick(helper.getLevel());
        helper.assertTrue(
                machine.energy().stored() == 99 && machine.heat() == 0,
                "A full-strength signal inverted by 15 - 15 drops the keep-warm input and cools");

        helper.setBlock(POSITION.above(), Blocks.AIR);
        machine.serverTick(helper.getLevel());
        helper.assertTrue(
                machine.energy().stored() == 98 && machine.heat() == 1,
                "Removing the signal restores the inverted keep-warm input");

        helper.succeed();
    }

    static void suitabilityMatchesLegacyRedstoneSensitive(GameTestHelper helper) {
        var inverter = UpgradeItem.Kind.REDSTONE_INVERTER;
        helper.assertTrue(
                inverter.suitable(MachineKind.INDUCTION_FURNACE)
                        && inverter.suitable(MachineKind.CENTRIFUGE)
                        && inverter.suitable(MachineKind.ADV_MINER)
                        && inverter.suitable(MachineKind.BLAST_FURNACE)
                        && inverter.suitable(MachineKind.MATTER_GENERATOR)
                        && inverter.suitable(MachineKind.REPLICATOR),
                "Every legacy RedstoneSensitive machine with upgrade slots accepts the inverter");
        helper.assertTrue(
                !inverter.suitable(MachineKind.ELECTRIC_FURNACE)
                        && !inverter.suitable(MachineKind.MAGNETIZER),
                "Plain electric machines reject the inverter and the port magnetizer has no slots");
        var remote = UpgradeItem.Kind.REMOTE_INTERFACE;
        helper.assertTrue(
                !remote.suitable(MachineKind.INDUCTION_FURNACE)
                        && !remote.suitable(MachineKind.ADV_MINER)
                        && !remote.suitable(MachineKind.REPLICATOR)
                        && !remote.suitable(MachineKind.CENTRIFUGE),
                "Legacy wires no RemotelyAccessible machine, so the remote interface fits nowhere");

        helper.setBlock(
                POSITION, ModMachines.block(MachineKind.MATTER_GENERATOR).defaultBlockState());
        var matter = helper.getBlockEntity(POSITION, MatterGeneratorBlockEntity.class);
        matter
                .inventory()
                .set(
                        3,
                        ItemResource.of(ModUpgrades.ALL.get(UpgradeItem.Kind.REDSTONE_INVERTER).get()),
                        1);
        helper.assertTrue(
                UpgradeItem.invertedSignal(
                        matter.kind(), matter.inventory(), helper.getLevel(), helper.absolutePos(POSITION)),
                "Pause-gated machines also see a signalless world as powered through the inverter");
        helper.setBlock(POSITION.above(), Blocks.REDSTONE_BLOCK);
        var absolute = helper.absolutePos(POSITION);
        int probe = helper.getLevel().getBestNeighborSignal(absolute);
        helper.assertTrue(probe == 15, "Signal above the matter generator must be 15, saw " + probe);
        helper.assertTrue(
                !UpgradeItem.invertedSignal(
                        matter.kind(), matter.inventory(), helper.getLevel(), absolute),
                "A full-strength signal reads as unpowered after the inverter (15 - 15)");

        helper.succeed();
    }

    static void menuInsertionGates(GameTestHelper helper) {
        helper.setBlock(
                POSITION, ModMachines.block(MachineKind.INDUCTION_FURNACE).defaultBlockState());
        var machine = helper.getBlockEntity(POSITION, InductionFurnaceBlockEntity.class);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var menu = new MachineMenu(1, player.getInventory(), machine);
        player.containerMenu = menu;

        player.getInventory()
                .setItem(
                        0, ModUpgrades.ALL.get(UpgradeItem.Kind.REDSTONE_INVERTER).toStack());
        menu.quickMoveStack(player, 34);
        helper.assertTrue(
                machine
                        .inventory()
                        .stack(5)
                        .is(ModUpgrades.ALL.get(UpgradeItem.Kind.REDSTONE_INVERTER).get()),
                "Shift-click must route the redstone inverter into an induction upgrade slot");

        player.getInventory()
                .setItem(
                        1, ModUpgrades.ALL.get(UpgradeItem.Kind.REMOTE_INTERFACE).toStack());
        menu.quickMoveStack(player, 35);
        boolean stayedWithPlayer = false;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++)
            if (player
                    .getInventory()
                    .getItem(slot)
                    .is(ModUpgrades.ALL.get(UpgradeItem.Kind.REMOTE_INTERFACE).get()))
                stayedWithPlayer = true;
        boolean reachedMachine = false;
        for (int slot = 0; slot < machine.inventory().size(); slot++)
            if (machine
                    .inventory()
                    .stack(slot)
                    .is(ModUpgrades.ALL.get(UpgradeItem.Kind.REMOTE_INTERFACE).get()))
                reachedMachine = true;
        helper.assertTrue(
                stayedWithPlayer && !reachedMachine,
                "The inert remote interface must stay in the player inventory everywhere");

        helper.succeed();
    }

    private UpgradeInverterTests() {}
}
