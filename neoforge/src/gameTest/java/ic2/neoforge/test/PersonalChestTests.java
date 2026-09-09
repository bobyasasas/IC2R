package ic2.neoforge.test;

import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.machine.PersonalChestBlockEntity;
import ic2.neoforge.registration.ModMachines;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

final class PersonalChestTests {
    private static final BlockPos POSITION = new BlockPos(2, 1, 2);

    static void claimAndDeny(GameTestHelper helper) {
        var chest = chest(helper);
        var owner = helper.makeMockPlayer(GameType.SURVIVAL);
        var stranger = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.assertTrue(
                !chest.owned() && chest.permits(owner), "The first opener claims the safe");
        helper.assertTrue(chest.owned() && chest.permits(owner), "The owner keeps access");
        helper.assertTrue(
                !chest.permits(stranger), "A stranger is denied access to a claimed safe");
        try (var transaction = Transaction.openRoot()) {
            helper.assertValueEqual(
                    chest.inventory().insert(0, ItemResource.of(Items.DIAMOND), 5, transaction),
                    5,
                    "The owner stores diamonds");
            transaction.commit();
        }
        var restored =
                (PersonalChestBlockEntity)
                        BlockEntity.loadStatic(
                                chest.getBlockPos(),
                                chest.getBlockState(),
                                chest.saveWithFullMetadata(helper.getLevel().registryAccess()),
                                helper.getLevel().registryAccess());
        helper.assertTrue(
                restored.owned() && restored.permits(owner) && !restored.permits(stranger),
                "Ownership survives a block entity reload");
        helper.assertValueEqual(
                restored.inventory().getAmountAsInt(0),
                5,
                "The stored diamonds survive the reload");
        helper.succeed();
    }

    static void blocksAutomation(GameTestHelper helper) {
        var chest = chest(helper);
        var port =
                helper.getLevel()
                        .getCapability(
                                net.neoforged.neoforge.capabilities.Capabilities.Item.BLOCK,
                                helper.absolutePos(POSITION),
                                Direction.DOWN);
        helper.assertTrue(port != null, "The safe exposes an item port for viewing");
        try (var transaction = Transaction.openRoot()) {
            helper.assertValueEqual(
                    port.insert(0, ItemResource.of(Items.DIAMOND), 1, transaction),
                    0,
                    "Automation is refused by the backend");
        }
        try (var transaction = Transaction.openRoot()) {
            chest.inventory().insert(0, ItemResource.of(Items.DIAMOND), 1, transaction);
            transaction.commit();
        }
        helper.succeed();
    }

    private static PersonalChestBlockEntity chest(GameTestHelper helper) {
        helper.setBlock(POSITION, ModMachines.block(MachineKind.PERSONAL_CHEST));
        return helper.getBlockEntity(POSITION, PersonalChestBlockEntity.class);
    }

    private PersonalChestTests() {}
}
