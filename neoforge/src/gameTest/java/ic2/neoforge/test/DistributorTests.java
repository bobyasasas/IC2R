package ic2.neoforge.test;

import ic2.neoforge.machine.FluidDistributorBlockEntity;
import ic2.neoforge.machine.MachineBlock;
import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.machine.StorageBoxBlockEntity;
import ic2.neoforge.machine.TankBlockEntity;
import ic2.neoforge.machine.WeightedFluidDistributorBlockEntity;
import ic2.neoforge.machine.WeightedItemDistributorBlockEntity;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.transfer.MachineInventory;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/** Legacy distributor semantics: mode-gated sides, front push, balanced or prioritised output. */
final class DistributorTests {
    private static final BlockPos POSITION = new BlockPos(2, 1, 2);
    private static final FluidResource WATER = FluidResource.of(Fluids.WATER);

    private static FluidDistributorBlockEntity fluid(GameTestHelper helper, Direction facing) {
        helper.setBlock(
                POSITION,
                ModMachines.block(MachineKind.FLUID_DISTRIBUTOR)
                        .defaultBlockState()
                        .setValue(MachineBlock.FACING, facing));
        return helper.getBlockEntity(POSITION, FluidDistributorBlockEntity.class);
    }

    private static WeightedFluidDistributorBlockEntity weightedFluid(
            GameTestHelper helper, Direction facing) {
        helper.setBlock(
                POSITION,
                ModMachines.block(MachineKind.WEIGHTED_FLUID_DISTRIBUTOR)
                        .defaultBlockState()
                        .setValue(MachineBlock.FACING, facing));
        return helper.getBlockEntity(POSITION, WeightedFluidDistributorBlockEntity.class);
    }

    private static WeightedItemDistributorBlockEntity weightedItem(GameTestHelper helper) {
        helper.setBlock(
                POSITION,
                ModMachines.block(MachineKind.WEIGHTED_ITEM_DISTRIBUTOR)
                        .defaultBlockState()
                        .setValue(MachineBlock.FACING, Direction.NORTH));
        return helper.getBlockEntity(POSITION, WeightedItemDistributorBlockEntity.class);
    }

    private static TankBlockEntity tank(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, ModMachines.block(MachineKind.TANK));
        return helper.getBlockEntity(pos, TankBlockEntity.class);
    }

    private static StorageBoxBlockEntity box(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, ModMachines.block(MachineKind.WOODEN_STORAGE_BOX));
        return helper.getBlockEntity(pos, StorageBoxBlockEntity.class);
    }

    private static void fill(FluidDistributorBlockEntity machine, int amount) {
        try (var transaction = Transaction.openRoot()) {
            machine.tank().insert(0, WATER, amount, transaction);
            transaction.commit();
        }
    }

    private static void insert(MachineInventory inventory, int slot, ItemStack stack) {
        try (var transaction = Transaction.openRoot()) {
            inventory.insert(slot, ItemResource.of(stack), stack.getCount(), transaction);
            transaction.commit();
        }
    }

    private static long stored(StorageBoxBlockEntity box, ItemStack stack) {
        long total = 0;
        for (int slot = 0; slot < box.inventory().size(); slot++) {
            var found = box.inventory().getResource(slot);
            if (found.getItem() == stack.getItem()) total += box.inventory().getAmountAsLong(slot);
        }
        return total;
    }

    static void fluidActivePushesFront(GameTestHelper helper) {
        var machine = fluid(helper, Direction.EAST);
        var front = tank(helper, POSITION.east());
        helper.assertTrue(machine.menuAction(0), "Action 0 must flip the distribution mode");
        fill(machine, 1000);
        machine.serverTick(helper.getLevel());
        helper.assertTrue(
                front.tank().getAmountAsInt(0) == 1000 && machine.tank().getAmountAsInt(0) == 0,
                "Active mode must push the whole tank out of the front");
        fill(machine, 1000);
        insert(machine.inventory(), FluidDistributorBlockEntity.INPUT, new ItemStack(Items.BUCKET));
        machine.serverTick(helper.getLevel());
        helper.assertTrue(
                machine.inventory().stack(FluidDistributorBlockEntity.OUTPUT).is(Items.WATER_BUCKET)
                        && machine.tank().getAmountAsInt(0) == 0,
                "Containers must fill from the tank and leave through the output");
        helper.succeed();
    }

    static void fluidIdleBalancesSides(GameTestHelper helper) {
        var machine = fluid(helper, Direction.NORTH);
        var east = tank(helper, POSITION.east());
        var west = tank(helper, POSITION.west());
        fill(machine, 800);
        machine.serverTick(helper.getLevel());
        helper.assertTrue(
                east.tank().getAmountAsInt(0) == 400 && west.tank().getAmountAsInt(0) == 400,
                "Idle mode must balance the tank evenly across every side but the front");
        helper.assertTrue(
                machine.tank().getAmountAsInt(0) == 0,
                "Balanced distribution must drain the tank completely");
        helper.succeed();
    }

    static void fluidPortsFollowMode(GameTestHelper helper) {
        var machine = fluid(helper, Direction.EAST);
        try (var transaction = Transaction.openRoot()) {
            helper.assertTrue(
                    machine.fluidAutomation(Direction.NORTH).insert(0, WATER, 1, transaction) == 0
                            && machine.fluidAutomation(Direction.EAST)
                                    .insert(0, WATER, 1, transaction)
                                    == 1,
                    "Idle mode must take fluid only through the front");
            transaction.commit();
        }
        machine.menuAction(0);
        try (var transaction = Transaction.openRoot()) {
            helper.assertTrue(
                    machine.fluidAutomation(Direction.NORTH).insert(0, WATER, 1, transaction) == 1
                            && machine.fluidAutomation(Direction.EAST)
                                    .insert(0, WATER, 1, transaction)
                                    == 0,
                    "Active mode must flip the accepted side set");
            transaction.commit();
        }
        var weighted = weightedFluid(helper, Direction.EAST);
        try (var transaction = Transaction.openRoot()) {
            helper.assertTrue(
                    weighted.fluidAutomation(Direction.NORTH).insert(0, WATER, 1, transaction) == 0
                            && weighted.fluidAutomation(Direction.EAST)
                                    .insert(0, WATER, 1, transaction)
                                    == 1,
                    "The weighted distributor always drinks from the front alone");
            transaction.commit();
        }
        helper.succeed();
    }

    static void weightedFluidFollowsPriority(GameTestHelper helper) {
        var machine = weightedFluid(helper, Direction.NORTH);
        var east = tank(helper, POSITION.east());
        var west = tank(helper, POSITION.west());
        helper.assertTrue(!machine.menuAction(2), "The facing must never enter the priority list");
        helper.assertTrue(
                machine.menuAction(5) && machine.menuAction(4),
                "East and west must join the priority list");
        helper.assertTrue(
                machine.menuValue(0) == 5 && machine.menuValue(1) == 4 && machine.menuValue(2) == -1,
                "The priority readout must list east then west");
        fill(machine, 1000);
        machine.serverTick(helper.getLevel());
        helper.assertTrue(
                east.tank().getAmountAsInt(0) == 1000 && west.tank().getAmountAsInt(0) == 0,
                "The first priority neighbour must take the whole tank");
        helper.assertTrue(
                machine.menuAction(5) && machine.menuValue(0) == 4,
                "Toggling east again must remove it from the list");
        fill(machine, 1000);
        machine.serverTick(helper.getLevel());
        helper.assertTrue(
                west.tank().getAmountAsInt(0) == 1000,
                "After east leaves the list, west takes the tank");
        helper.succeed();
    }

    static void weightedItemFollowsPriority(GameTestHelper helper) {
        var machine = weightedItem(helper);
        var east = box(helper, POSITION.east());
        var west = box(helper, POSITION.west());
        helper.assertTrue(
                !machine.menuAction(2), "The facing must never enter the priority list");
        helper.assertTrue(
                machine.menuAction(5) && machine.menuAction(4),
                "East and west must join the priority list");
        insert(machine.inventory(), 0, new ItemStack(Items.COBBLESTONE, 64));
        machine.serverTick(helper.getLevel());
        helper.assertTrue(
                stored(east, new ItemStack(Items.COBBLESTONE)) == 64,
                "The first priority neighbour must take the whole buffer");
        helper.assertTrue(
                machine.inventory().getAmountAsLong(0) == 0,
                "The buffer must empty before later neighbours are tried");
        helper.assertTrue(
                machine.menuAction(5) && machine.menuValue(0) == 4,
                "Toggling east again must remove it from the list");
        insert(machine.inventory(), 0, new ItemStack(Items.COBBLESTONE, 32));
        machine.serverTick(helper.getLevel());
        helper.assertTrue(
                stored(west, new ItemStack(Items.COBBLESTONE)) == 32,
                "The next priority neighbour must serve after the first leaves");
        helper.assertTrue(
                stored(east, new ItemStack(Items.COBBLESTONE)) == 64,
                "A removed neighbour must no longer draw from the buffer");
        helper.succeed();
    }

    static void priorityPersistsAcrossReload(GameTestHelper helper) {
        var machine = weightedItem(helper);
        machine.menuAction(5);
        machine.menuAction(4);
        var restored =
                (WeightedItemDistributorBlockEntity)
                        BlockEntity.loadStatic(
                                machine.getBlockPos(),
                                machine.getBlockState(),
                                machine.saveWithFullMetadata(helper.getLevel().registryAccess()),
                                helper.getLevel().registryAccess());
        restored.setLevel(helper.getLevel());
        helper.assertTrue(
                restored.menuValue(0) == 5 && restored.menuValue(1) == 4,
                "The priority order must survive a save-load round trip");
        helper.assertTrue(
                restored.menuAction(5) && restored.menuValue(0) == 4 && restored.menuValue(1) == -1,
                "Toggling a stored side must remove it and close the gap");
        helper.succeed();
    }

    private DistributorTests() {}
}
