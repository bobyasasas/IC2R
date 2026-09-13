package ic2.neoforge.test;

import ic2.neoforge.machine.MachineBlock;
import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.machine.StorageBoxBlockEntity;
import ic2.neoforge.registration.ModMachines;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Blocks;
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

    static void contentsRideTheDrop(GameTestHelper helper) {
        var box = box(helper, MachineKind.WOODEN_STORAGE_BOX);
        try (var transaction = Transaction.openRoot()) {
            box.inventory().insert(2, ItemResource.of(Items.STICK), 4, transaction);
            box.inventory().insert(5, ItemResource.of(Items.BRICK), 1, transaction);
            transaction.commit();
        }
        var state = helper.getBlockState(POSITION);
        var block = ModMachines.block(MachineKind.WOODEN_STORAGE_BOX);
        var dropped =
                block
                        .getDrops(
                                state,
                                (ServerLevel) helper.getLevel(),
                                helper.absolutePos(POSITION),
                                box)
                        .stream()
                        .filter(stack -> stack.is(block.asItem()))
                        .findFirst()
                        .orElse(null);
        if (dropped == null) {
            helper.assertTrue(false, "Breaking the box must drop the block item");
            return;
        }
        helper.setBlock(POSITION, Blocks.AIR.defaultBlockState());
        helper.assertTrue(
                helper.getEntities(EntityType.ITEM).isEmpty(),
                "Breaking a box must not scatter its contents on the ground");
        var restored = NonNullList.withSize(27, ItemStack.EMPTY);
        dropped.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
                .copyInto(restored);
        helper.assertValueEqual(
                restored.get(2).getCount(), 4, "Slot 2 rides the drop");
        helper.assertValueEqual(
                restored.get(5).getCount(), 1, "Slot 5 rides the drop");
        var reloaded = box(helper, MachineKind.WOODEN_STORAGE_BOX);
        block.setPlacedBy(
                helper.getLevel(),
                helper.absolutePos(POSITION),
                helper.getBlockState(POSITION),
                null,
                dropped);
        helper.assertTrue(
                reloaded.inventory().stack(2).is(Items.STICK)
                        && reloaded.inventory().stack(2).getCount() == 4,
                "Placement restores the carried contents to their slots");
        helper.assertTrue(
                reloaded.inventory().stack(5).is(Items.BRICK)
                        && reloaded.inventory().stack(5).getCount() == 1,
                "Slot 5 survives the round trip");
        helper.succeed();
    }

    static void tierCapacitiesMatchLegacy(GameTestHelper helper) {
        record Tier(MachineKind kind, int size) {}
        for (var tier : new Tier[] {
            new Tier(MachineKind.WOODEN_STORAGE_BOX, 27),
            new Tier(MachineKind.BRONZE_STORAGE_BOX, 45),
            new Tier(MachineKind.IRON_STORAGE_BOX, 45),
            new Tier(MachineKind.STEEL_STORAGE_BOX, 63),
            new Tier(MachineKind.IRIDIUM_STORAGE_BOX, 126)
        }) {
            var box = box(helper, tier.kind());
            helper.assertValueEqual(
                    box.inventory().size(), tier.size(), tier.kind() + " capacity");
            helper.setBlock(POSITION, Blocks.AIR.defaultBlockState());
        }
        helper.succeed();
    }

    private static StorageBoxBlockEntity box(GameTestHelper helper, MachineKind kind) {
        helper.setBlock(POSITION, ModMachines.block(kind));
        return helper.getBlockEntity(POSITION, StorageBoxBlockEntity.class);
    }

    private StorageBoxTests() {}
}
