package ic2.neoforge.test;

import ic2.neoforge.item.UpgradeItem;
import ic2.neoforge.machine.ItemBufferBlockEntity;
import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.machine.StorageBoxBlockEntity;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.registration.ModUpgrades;
import ic2.neoforge.transfer.MachineInventory;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

final class ItemBufferTests {
    private static final BlockPos POSITION = new BlockPos(8, 8, 8);

    private static ItemBufferBlockEntity buffer(GameTestHelper helper) {
        helper.setBlock(POSITION, ModMachines.block(MachineKind.ITEM_BUFFER).defaultBlockState());
        return helper.getBlockEntity(POSITION, ItemBufferBlockEntity.class);
    }

    private static StorageBoxBlockEntity box(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, ModMachines.block(MachineKind.WOODEN_STORAGE_BOX).defaultBlockState());
        return helper.getBlockEntity(pos, StorageBoxBlockEntity.class);
    }

    private static long stored(StorageBoxBlockEntity box, ItemStack stack) {
        long total = 0;
        for (int slot = 0; slot < box.inventory().size(); slot++) {
            var found = box.inventory().getResource(slot);
            if (found.getItem() == stack.getItem()) total += box.inventory().getAmountAsLong(slot);
        }
        return total;
    }

    private static void insert(MachineInventory inventory, int slot, ItemStack stack) {
        try (var transaction = Transaction.openRoot()) {
            inventory.insert(slot, ItemResource.of(stack), stack.getCount(), transaction);
            transaction.commit();
        }
    }

    private static void insertUpgrade(
            ItemBufferBlockEntity buffer, UpgradeItem.Kind kind, int count) {
        var stack = ModUpgrades.ALL.get(kind).get().getDefaultInstance();
        stack.setCount(count);
        insert(buffer.inventory(), buffer.kind().upgradeStart(), stack);
    }

    static void ejectorSendsSidesOut(GameTestHelper helper) {
        var level = helper.getLevel();
        var buffer = buffer(helper);
        var neighbor = box(helper, new BlockPos(9, 8, 8));
        insert(
                buffer.inventory(),
                ItemBufferBlockEntity.SIDE_START,
                new ItemStack(Items.COBBLESTONE, 8));
        insert(buffer.inventory(), 0, new ItemStack(Items.DIRT, 4));
        insertUpgrade(buffer, UpgradeItem.Kind.EJECTOR, 1);
        for (int tick = 0; tick < 40; tick++) buffer.serverTick(level);
        helper.assertTrue(
                buffer.inventory().getResource(ItemBufferBlockEntity.SIDE_START).getItem()
                        != Items.COBBLESTONE,
                "The ejector emptied the side group");
        helper.assertTrue(
                stored(neighbor, new ItemStack(Items.COBBLESTONE)) == 8,
                "The eastern neighbor received the ejected stack");
        helper.assertTrue(
                buffer.inventory().getAmountAsLong(0) == 4,
                "The vertical group is not exposed to horizontal ejectors");
        helper.succeed();
    }

    static void pullingTakesFromAbove(GameTestHelper helper) {
        var level = helper.getLevel();
        var buffer = buffer(helper);
        var above = box(helper, new BlockPos(8, 9, 8));
        insert(above.inventory(), 0, new ItemStack(Items.RAW_IRON, 5));
        insertUpgrade(buffer, UpgradeItem.Kind.PULLING, 1);
        for (int tick = 0; tick < 40; tick++) buffer.serverTick(level);
        helper.assertTrue(
                buffer.inventory().getResource(0).getItem() == Items.RAW_IRON
                        && buffer.inventory().getAmountAsLong(0) == 5,
                "The pulling upgrade filled the vertical group from above");
        helper.assertTrue(
                buffer.inventory().getAmountAsLong(ItemBufferBlockEntity.SIDE_START) == 0,
                "The pulled stack stayed in the vertical group");
        helper.assertTrue(
                stored(above, new ItemStack(Items.RAW_IRON)) == 0, "The chest above was drained");
        helper.succeed();
    }

    static void portsAndUpgradeSlots(GameTestHelper helper) {
        var buffer = buffer(helper);
        var horizontal = buffer.automation(Direction.NORTH);
        var vertical = buffer.automation(Direction.UP);
        try (var transaction = Transaction.openRoot()) {
            helper.assertTrue(
                    horizontal.insert(0, ItemResource.of(new ItemStack(Items.DIRT)), 1, transaction)
                            == 0,
                    "The northern port rejects the vertical group");
            helper.assertTrue(
                    horizontal.insert(
                                    ItemBufferBlockEntity.SIDE_START,
                                    ItemResource.of(new ItemStack(Items.DIRT)),
                                    1,
                                    transaction)
                            == 1,
                    "The northern port accepts the side group");
            helper.assertTrue(
                    vertical.insert(
                                    ItemBufferBlockEntity.SIDE_START,
                                    ItemResource.of(new ItemStack(Items.DIRT)),
                                    1,
                                    transaction)
                            == 0,
                    "The top port rejects the side group");
            helper.assertTrue(
                    vertical.insert(0, ItemResource.of(new ItemStack(Items.DIRT)), 1, transaction)
                            == 1,
                    "The top port accepts the vertical group");
            transaction.commit();
        }
        try (var transaction = Transaction.openRoot()) {
            helper.assertTrue(
                    buffer.inventory()
                                    .insert(
                                            buffer.kind().upgradeStart(),
                                            ItemResource.of(
                                                    ModUpgrades.ALL
                                                            .get(UpgradeItem.Kind.OVERCLOCKER)
                                                            .get()
                                                            .getDefaultInstance()),
                                            1,
                                            transaction)
                            == 0,
                    "The buffer rejects overclockers in its upgrade slots");
            transaction.commit();
        }
        helper.succeed();
    }

    private ItemBufferTests() {}
}
