package ic2.neoforge.test;

import ic2.neoforge.item.ElectricItemEnergy;
import ic2.neoforge.machine.EnergyOMatBlockEntity;
import ic2.neoforge.machine.MachineBlock;
import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.machine.StorageBoxBlockEntity;
import ic2.neoforge.registration.ModItems;
import ic2.neoforge.registration.ModMachines;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

final class EnergyOMatTests {
    private static final BlockPos POSITION = new BlockPos(2, 1, 2);
    private static final ItemResource IRON = ItemResource.of(Items.IRON_INGOT);
    private static final ItemResource BATTERY = ItemResource.of(ModItems.RE_BATTERY.get());

    static void tradePaysForCredit(GameTestHelper helper) {
        var machine = machine(helper);
        var supply = box(helper, POSITION.east());
        set(machine, EnergyOMatBlockEntity.DEMAND, IRON, 1);
        set(machine, EnergyOMatBlockEntity.INPUT, IRON, 1);
        machine.serverTick(helper.getLevel());
        helper.assertTrue(
                machine.inventory().stack(EnergyOMatBlockEntity.INPUT).isEmpty(),
                "The demanded stack is consumed");
        helper.assertValueEqual(
                supply.inventory().getAmountAsInt(0),
                1,
                "The traded-in ingot lands in the adjacent supply");
        helper.assertValueEqual(machine.paidFor(), 1000, "One ingot buys the 1000 EU offer");
        helper.assertTrue(machine.acceptsFrom(Direction.UP), "Paid credit opens the grid input");
        helper.assertValueEqual(
                (int) machine.energy().stored(), 0, "The trade itself must not conjure EU");
        helper.succeed();
    }

    static void unpaidGate(GameTestHelper helper) {
        var machine = machine(helper);
        helper.assertTrue(
                !machine.acceptsFrom(Direction.UP),
                "Without paid credit the machine must refuse grid input");
        Direction facing = machine.getBlockState().getValue(MachineBlock.FACING);
        helper.assertTrue(machine.emitsTo(facing), "The marked face stays the output");
        helper.assertTrue(!machine.emitsTo(Direction.UP), "No other face emits");
        // The buffer still feeds its own charge slot regardless of the grid gate; the
        // battery only accepts its 100 EU/t transfer limit, so give it a few ticks.
        set(machine, EnergyOMatBlockEntity.CHARGE, BATTERY, 1);
        machine.energy().insert(500);
        for (int tick = 0; tick < 6; tick++) machine.serverTick(helper.getLevel());
        var battery = machine.inventory().stack(EnergyOMatBlockEntity.CHARGE);
        helper.assertValueEqual(
                (int) ElectricItemEnergy.charge(battery),
                500,
                "Unpaid or not, the buffer charges a battery it holds");
        helper.assertValueEqual(machine.paidFor(), 0, "No trade, no credit");
        helper.succeed();
    }

    static void creditPaysForCharge(GameTestHelper helper) {
        var machine = machine(helper);
        box(helper, POSITION.east());
        set(machine, EnergyOMatBlockEntity.DEMAND, IRON, 1);
        set(machine, EnergyOMatBlockEntity.INPUT, IRON, 1);
        machine.serverTick(helper.getLevel());
        helper.assertValueEqual(machine.paidFor(), 1000, "Setup: the ingot bought credit");
        set(machine, EnergyOMatBlockEntity.CHARGE, BATTERY, 1);
        machine.energy().insert(600);
        // The first tick meters the arrival against the credit; the battery then drains the
        // buffer at its 100 EU/t transfer limit.
        for (int tick = 0; tick < 8; tick++) machine.serverTick(helper.getLevel());
        var battery = machine.inventory().stack(EnergyOMatBlockEntity.CHARGE);
        helper.assertValueEqual(
                (int) ElectricItemEnergy.charge(battery),
                600,
                "Arrived grid power flows into the charge slot");
        helper.assertValueEqual(
                (int) machine.energy().stored(), 0, "The buffer is left empty");
        helper.assertValueEqual(
                machine.paidFor(), 400, "Grid arrival burns exactly its paid credit");
        helper.succeed();
    }

    static void priceKeypad(GameTestHelper helper) {
        var machine = machine(helper);
        helper.assertValueEqual(machine.offer(), 1000, "The default price is 1000 EU");
        helper.assertTrue(machine.menuAction(3), "The -100 button answers");
        helper.assertValueEqual(machine.offer(), 900, "-100 lowers the price");
        helper.assertTrue(machine.menuAction(0), "The -100000 button answers");
        helper.assertValueEqual(machine.offer(), 100, "The price floors at 100 EU");
        helper.assertTrue(machine.menuAction(6), "The +1000 button answers");
        helper.assertValueEqual(machine.offer(), 1100, "+1000 raises the price");
        helper.assertValueEqual(machine.menuValue(0), 1100, "The menu exposes the offer");
        helper.succeed();
    }

    private static EnergyOMatBlockEntity machine(GameTestHelper helper) {
        helper.setBlock(POSITION, ModMachines.block(MachineKind.ENERGY_O_MAT));
        return helper.getBlockEntity(POSITION, EnergyOMatBlockEntity.class);
    }

    private static StorageBoxBlockEntity box(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, ModMachines.block(MachineKind.WOODEN_STORAGE_BOX));
        return helper.getBlockEntity(pos, StorageBoxBlockEntity.class);
    }

    private static void set(EnergyOMatBlockEntity machine, int slot, ItemResource resource,
            int count) {
        try (var transaction = Transaction.openRoot()) {
            machine.inventory().insert(slot, resource, count, transaction);
            transaction.commit();
        }
    }

    private EnergyOMatTests() {}
}
