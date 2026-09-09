package ic2.neoforge.test;

import static net.minecraft.world.inventory.ContainerInput.PICKUP;

import ic2.neoforge.component.ModDataComponents;
import ic2.neoforge.item.ElectricItemEnergy;
import ic2.neoforge.machine.AdvMinerBlockEntity;
import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.menu.MiningFilterMenu;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.registration.ModTools;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.List;

final class MiningFilterCardTests {
    private static final BlockPos POSITION = new BlockPos(8, 8, 8);

    private static AdvMinerBlockEntity miner(
            GameTestHelper helper, ItemStack card, ItemStack machineFilter) {
        helper.setBlock(POSITION, ModMachines.block(MachineKind.ADV_MINER).defaultBlockState());
        var miner = helper.getBlockEntity(POSITION, AdvMinerBlockEntity.class);
        miner.energy().restore(100000);
        helper.assertTrue(miner.menuAction(1), "The machine runs in whitelist mode");
        var chargedScanner = ModTools.SCANNER.toStack();
        ElectricItemEnergy.charge(chargedScanner, 100000, 1, true, false);
        try (var transaction = Transaction.openRoot()) {
            var inventory = miner.inventory();
            inventory.insert(
                    AdvMinerBlockEntity.SCANNER_SLOT,
                    ItemResource.of(chargedScanner),
                    1,
                    transaction);
            if (machineFilter != null)
                inventory.insert(
                        AdvMinerBlockEntity.FILTER_START,
                        ItemResource.of(machineFilter),
                        1,
                        transaction);
            if (card != null)
                inventory.insert(
                        AdvMinerBlockEntity.CARD_SLOT, ItemResource.of(card), 1, transaction);
            transaction.commit();
        }
        return miner;
    }

    private static void run(AdvMinerBlockEntity miner, int ticks) {
        var level = (ServerLevel) miner.getLevel();
        for (int tick = 0; tick < ticks; tick++) miner.serverTick(level);
    }

    private static BlockState blockAt(GameTestHelper helper, int x, int y, int z) {
        return helper.getLevel().getBlockState(helper.absolutePos(new BlockPos(x, y, z)));
    }

    static void uneditedCardDefersToMachineFilter(GameTestHelper helper) {
        var level = helper.getLevel();
        level.setBlockAndUpdate(
                helper.absolutePos(new BlockPos(9, 7, 9)), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(
                helper.absolutePos(new BlockPos(10, 7, 9)), Blocks.COAL_ORE.defaultBlockState());
        var miner = miner(helper, ModTools.MINING_FILTER_CARD.toStack(), new ItemStack(Items.COAL));
        run(miner, 9000);
        helper.assertTrue(
                blockAt(helper, 10, 7, 9).isAir(), "The machine whitelist mined the coal ore");
        helper.assertTrue(
                !blockAt(helper, 9, 7, 9).isAir(),
                "The unedited card deferred to the machine whitelist");
        helper.succeed();
    }

    static void editedCardOverridesMachineFilter(GameTestHelper helper) {
        var level = helper.getLevel();
        level.setBlockAndUpdate(
                helper.absolutePos(new BlockPos(9, 7, 9)), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(
                helper.absolutePos(new BlockPos(10, 7, 9)), Blocks.COAL_ORE.defaultBlockState());
        var card = ModTools.MINING_FILTER_CARD.toStack();
        card.set(ModDataComponents.MINING_FILTER_BLACKLIST, false);
        card.set(
                ModDataComponents.MINING_FILTER_ITEMS,
                ItemContainerContents.fromItems(List.of(new ItemStack(Items.COBBLESTONE))));
        var miner = miner(helper, card, new ItemStack(Items.COAL));
        run(miner, 9000);
        helper.assertTrue(
                blockAt(helper, 9, 7, 9).isAir(),
                "The card whitelist mined the stone despite the machine entry");
        helper.assertTrue(
                !blockAt(helper, 10, 7, 9).isAir(),
                "The card whitelist kept the coal ore the machine listed");
        boolean coalDropped =
                helper.getEntities(EntityType.ITEM).stream()
                        .noneMatch(entity -> entity.getItem().is(Items.COAL));
        helper.assertTrue(coalDropped, "No coal dropped while the card overrode the machine");
        helper.succeed();
    }

    static void handheldMenuEditsCard(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var card = ModTools.MINING_FILTER_CARD.toStack();
        player.getInventory().setItem(0, card);
        var menu =
                new MiningFilterMenu(
                        1, player.getInventory(), player.getInventory().getSelectedSlot());
        player.containerMenu = menu;
        helper.assertTrue(menu.stillValid(player), "The editor stays open while the card is held");
        menu.setCarried(new ItemStack(Items.COAL));
        menu.clicked(5, 0, PICKUP, player);
        var entries = card.get(ModDataComponents.MINING_FILTER_ITEMS);
        helper.assertTrue(
                entries.getSlots() == 6 && entries.getStackInSlot(5).is(Items.COAL),
                "Left clicking a hologram slot copies one of the carried item");
        helper.assertTrue(
                card.get(ModDataComponents.MINING_FILTER_BLACKLIST),
                "Opening the editor marks the card as edited");
        helper.assertTrue(
                menu.clickMenuButton(player, 0) && !menu.blacklist(),
                "The mode button switches to whitelist");
        helper.assertTrue(
                !card.get(ModDataComponents.MINING_FILTER_BLACKLIST),
                "The mode is stored on the card");
        menu.setCarried(ItemStack.EMPTY);
        menu.clicked(5, 0, PICKUP, player);
        helper.assertTrue(
                card.get(ModDataComponents.MINING_FILTER_ITEMS).nonEmptyItemCopyStream().count()
                        == 0,
                "Left clicking with an empty hand clears the entry");
        helper.succeed();
    }

    private MiningFilterCardTests() {}
}
