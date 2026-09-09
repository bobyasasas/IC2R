package ic2.neoforge.test;

import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.machine.StorageBoxBlockEntity;
import ic2.neoforge.machine.TradeOMatBlockEntity;
import ic2.neoforge.registration.ModMachines;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

final class TradeOMatTests {
    private static final BlockPos POSITION = new BlockPos(2, 1, 2);
    private static final ItemResource STICK = ItemResource.of(Items.STICK);
    private static final ItemResource DIAMOND = ItemResource.of(Items.DIAMOND);

    static void infiniteTrade(GameTestHelper helper) {
        var machine = machine(helper);
        set(machine, TradeOMatBlockEntity.DEMAND, STICK, 1);
        set(machine, TradeOMatBlockEntity.OFFER, DIAMOND, 1);
        machine.toggleInfinite();
        insertInput(machine);
        machine.serverTick(helper.getLevel());
        helper.assertTrue(
                machine.inventory().stack(TradeOMatBlockEntity.INPUT).isEmpty(),
                "The demanded stack is consumed");
        helper.assertValueEqual(
                machine.inventory().getAmountAsInt(TradeOMatBlockEntity.OUTPUT),
                1,
                "The infinite offer is conjured into the output");
        helper.assertValueEqual(machine.totalTradeCount(), 1, "The trade is counted");
        helper.succeed();
    }

    static void suppliedTrade(GameTestHelper helper) {
        var machine = machine(helper);
        var supply = box(helper, POSITION.east());
        try (var transaction = Transaction.openRoot()) {
            supply.inventory().insert(0, DIAMOND, 3, transaction);
            transaction.commit();
        }
        set(machine, TradeOMatBlockEntity.DEMAND, STICK, 1);
        set(machine, TradeOMatBlockEntity.OFFER, DIAMOND, 1);
        insertInput(machine);
        machine.serverTick(helper.getLevel());
        helper.assertValueEqual(
                machine.inventory().getAmountAsInt(TradeOMatBlockEntity.OUTPUT),
                1,
                "The offer comes out of the adjacent supply");
        helper.assertValueEqual(
                supply.inventory().getAmountAsInt(0), 2, "The supply lost exactly one offer");
        helper.assertValueEqual(
                supply.inventory().getAmountAsInt(1),
                1,
                "The traded-in stick returns to the supply");
        helper.succeed();
    }

    private static TradeOMatBlockEntity machine(GameTestHelper helper) {
        helper.setBlock(POSITION, ModMachines.block(MachineKind.TRADE_O_MAT));
        return helper.getBlockEntity(POSITION, TradeOMatBlockEntity.class);
    }

    private static StorageBoxBlockEntity box(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, ModMachines.block(MachineKind.WOODEN_STORAGE_BOX));
        return helper.getBlockEntity(pos, StorageBoxBlockEntity.class);
    }

    private static void set(
            TradeOMatBlockEntity machine, int slot, ItemResource resource, int count) {
        try (var transaction = Transaction.openRoot()) {
            machine.inventory().insert(slot, resource, count, transaction);
            transaction.commit();
        }
    }

    private static void insertInput(TradeOMatBlockEntity machine) {
        try (var transaction = Transaction.openRoot()) {
            machine.inventory().insert(TradeOMatBlockEntity.INPUT, STICK, 1, transaction);
            transaction.commit();
        }
    }

    private TradeOMatTests() {}
}
