package ic2.neoforge.test;

import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.machine.SortingMachineBlockEntity;
import ic2.neoforge.machine.StorageBoxBlockEntity;
import ic2.neoforge.registration.ModMachines;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

final class SortingMachineTests {
    private static final BlockPos POSITION = new BlockPos(2, 1, 2);
    private static final ItemResource STICK = ItemResource.of(Items.STICK);
    private static final ItemResource DIAMOND = ItemResource.of(Items.DIAMOND);

    static void filterRouting(GameTestHelper helper) {
        var machine = machine(helper);
        var receiver = box(helper, POSITION.east(), MachineKind.WOODEN_STORAGE_BOX);
        // East face filters (face ordinal 5): one stick, meaning five stuck stacks route there.
        try (var transaction = Transaction.openRoot()) {
            helper.assertValueEqual(
                    machine.filters().insert(35, STICK, 1, transaction), 1, "Test sets a filter");
            transaction.commit();
        }
        try (var transaction = Transaction.openRoot()) {
            machine.inventory().insert(0, STICK, 5, transaction);
            transaction.commit();
        }
        machine.energy().insert(1000);
        for (int tick = 0; tick < 20; tick++) machine.serverTick(helper.getLevel());
        helper.assertValueEqual(
                receiver.inventory().getAmountAsInt(0),
                5,
                "The filtered neighbour receives the whole stack");
        helper.assertTrue(
                machine.inventory().stack(0).isEmpty(), "The buffer empties after routing");
        helper.assertValueEqual(machine.storedEnergy(), 900.0, "Five items cost one hundred EU");
        helper.succeed();
    }

    static void defaultRouteFallback(GameTestHelper helper) {
        var machine = machine(helper);
        helper.setBlock(POSITION.above(), ModMachines.block(MachineKind.WOODEN_STORAGE_BOX));
        var receiver = helper.getBlockEntity(POSITION.above(), StorageBoxBlockEntity.class);
        machine.defaultRoute(Direction.UP);
        try (var transaction = Transaction.openRoot()) {
            machine.inventory().insert(0, DIAMOND, 1, transaction);
            transaction.commit();
        }
        machine.energy().insert(100);
        for (int tick = 0; tick < 10; tick++) machine.serverTick(helper.getLevel());
        helper.assertValueEqual(
                receiver.inventory().getAmountAsInt(0),
                1,
                "An unfiltered item leaves through the default face");
        helper.assertValueEqual(machine.storedEnergy(), 80.0, "One item costs twenty EU");
        helper.succeed();
    }

    private static SortingMachineBlockEntity machine(GameTestHelper helper) {
        helper.setBlock(POSITION, ModMachines.block(MachineKind.SORTING_MACHINE));
        return helper.getBlockEntity(POSITION, SortingMachineBlockEntity.class);
    }

    private static StorageBoxBlockEntity box(
            GameTestHelper helper, BlockPos pos, MachineKind kind) {
        helper.setBlock(pos, ModMachines.block(kind));
        return helper.getBlockEntity(pos, StorageBoxBlockEntity.class);
    }

    private SortingMachineTests() {}
}
