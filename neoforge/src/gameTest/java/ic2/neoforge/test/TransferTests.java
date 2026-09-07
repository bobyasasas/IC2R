package ic2.neoforge.test;

import ic2.neoforge.transfer.MachineFluidTank;
import ic2.neoforge.transfer.MachineInventory;
import ic2.neoforge.transfer.ResourcePort;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.concurrent.atomic.AtomicInteger;

final class TransferTests {
    static void inventory(GameTestHelper helper) {
        var dirty = new AtomicInteger();
        var inventory = new MachineInventory(2, dirty::incrementAndGet, (slot, resource) -> true);
        var port = new ResourcePort<>(inventory, slot -> slot == 0, slot -> slot == 1);
        var iron = ItemResource.of(Items.IRON_INGOT);
        try (var transaction = Transaction.openRoot()) {
            helper.assertValueEqual(
                    port.insert(iron, 70, transaction),
                    64,
                    "Bulk insert obeys slot permissions and stack capacity");
            helper.assertValueEqual(
                    port.insert(1, iron, 1, transaction), 0, "Output insertion is forbidden");
        }
        helper.assertValueEqual(inventory.getAmountAsInt(0), 0, "Aborted insert rolls back");
        helper.assertValueEqual(dirty.get(), 0, "Simulation cannot mark the inventory dirty");
        try (var transaction = Transaction.openRoot()) {
            port.insert(iron, 4, transaction);
            transaction.commit();
        }
        helper.assertValueEqual(inventory.getAmountAsInt(0), 4, "Committed input");
        helper.assertValueEqual(dirty.get(), 1, "Dirty once on commit");
        try (var transaction = Transaction.openRoot()) {
            helper.assertValueEqual(
                    port.extract(iron, 4, transaction),
                    0,
                    "Bulk extraction cannot bypass input-only permissions");
            inventory.extract(0, iron, 4, transaction);
            try (var nested = Transaction.open(transaction)) {
                inventory.insert(1, iron, 4, nested);
                nested.commit();
            }
            // Outer abort must also undo a committed inner transaction.
        }
        helper.assertValueEqual(inventory.getAmountAsInt(0), 4, "Outer rollback restores input");
        helper.assertValueEqual(inventory.getAmountAsInt(1), 0, "Outer rollback restores output");
        helper.assertValueEqual(dirty.get(), 1, "Nested rollback does not notify");
        helper.succeed();
    }

    static void fluid(GameTestHelper helper) {
        var tank = new MachineFluidTank(1000, () -> {}, resource -> true);
        var water = FluidResource.of(Fluids.WATER);
        var lava = FluidResource.of(Fluids.LAVA);
        try (var transaction = Transaction.openRoot()) {
            helper.assertValueEqual(tank.insert(water, 1500, transaction), 1000, "Tank capacity");
        }
        helper.assertValueEqual(tank.getAmountAsInt(0), 0, "Fluid simulation rollback");
        try (var transaction = Transaction.openRoot()) {
            tank.insert(water, 700, transaction);
            transaction.commit();
        }
        try (var transaction = Transaction.openRoot()) {
            helper.assertValueEqual(
                    tank.insert(lava, 100, transaction), 0, "Different fluids cannot mix");
            helper.assertValueEqual(tank.extract(water, 200, transaction), 200, "Simulated drain");
        }
        helper.assertValueEqual(tank.getAmountAsInt(0), 700, "Drain rollback");
        var output = new ResourcePort<>(tank, slot -> false, slot -> true);
        try (var transaction = Transaction.openRoot()) {
            helper.assertValueEqual(
                    output.insert(water, 50, transaction), 0, "Output-only tank refuses insertion");
            output.extract(water, 250, transaction);
            transaction.commit();
        }
        helper.assertValueEqual(tank.getAmountAsInt(0), 450, "Committed drain");
        helper.succeed();
    }
}
