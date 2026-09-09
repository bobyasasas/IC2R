package ic2.neoforge.test;

import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.machine.StorageBoxBlockEntity;
import ic2.neoforge.registration.ModMachines;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

final class StorageBoxTests {
    private static final BlockPos POSITION = new BlockPos(2, 1, 2);

    static void woodenCapacityAndAutomation(GameTestHelper helper) {
        var box = box(helper, MachineKind.WOODEN_STORAGE_BOX);
        var top =
                helper.getLevel()
                        .getCapability(
                                Capabilities.Item.BLOCK,
                                helper.absolutePos(POSITION),
                                Direction.UP);
        var bottom =
                helper.getLevel()
                        .getCapability(
                                Capabilities.Item.BLOCK,
                                helper.absolutePos(POSITION),
                                Direction.DOWN);
        helper.assertTrue(top != null && bottom != null, "Boxes expose every face");
        try (var transaction = Transaction.openRoot()) {
            for (int slot = 0; slot < 27; slot++)
                helper.assertValueEqual(
                        top.insert(slot, ItemResource.of(Items.STICK), 4, transaction),
                        4,
                        "Every slot of the wooden box accepts items");
            transaction.commit();
        }
        try (var transaction = Transaction.openRoot()) {
            helper.assertValueEqual(
                    bottom.extract(0, ItemResource.of(Items.STICK), 108, transaction),
                    4,
                    "Any face offers the slot contents, scoped to one slot per call");
            transaction.commit();
        }
        helper.assertTrue(box.inventory().stack(0).isEmpty(), "The box is empty after extraction");
        helper.succeed();
    }

    static void iridiumHolds126Slots(GameTestHelper helper) {
        var box = box(helper, MachineKind.IRIDIUM_STORAGE_BOX);
        try (var transaction = Transaction.openRoot()) {
            for (int slot = 0; slot < 126; slot++) {
                if (box.inventory().insert(slot, ItemResource.of(Items.BRICK), 1, transaction)
                        != 1) {
                    helper.assertTrue(false, "Iridium box must hold 126 slots");
                    return;
                }
            }
            transaction.commit();
        }
        helper.assertValueEqual(box.inventory().size(), 126, "The iridium box has 126 slots");
        helper.succeed();
    }

    private static StorageBoxBlockEntity box(GameTestHelper helper, MachineKind kind) {
        helper.setBlock(POSITION, ModMachines.block(kind));
        return helper.getBlockEntity(POSITION, StorageBoxBlockEntity.class);
    }

    private StorageBoxTests() {}
}
