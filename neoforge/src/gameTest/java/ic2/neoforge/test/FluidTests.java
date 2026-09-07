package ic2.neoforge.test;

import ic2.neoforge.component.ModDataComponents;
import ic2.neoforge.machine.CannerBlockEntity;
import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.registration.ModCells;
import ic2.neoforge.registration.ModFluids;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.transfer.MachineInventory;
import ic2.neoforge.transfer.ResourcePort;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStackTemplate;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

final class FluidTests {
    static void families(GameTestHelper helper) {
        helper.assertTrue(
                ModFluids.FAMILIES.size() == 17 && ModCells.CELLS.size() == 20,
                "All fluid families and classic cell IDs must register");
        for (var entry : ModFluids.FAMILIES.entrySet()) {
            var definition = entry.getKey();
            var family = entry.getValue();
            helper.assertTrue(
                    family.source().get().getFluidType() == family.type().get()
                            && family.flowing().get().getSource() == family.source().get(),
                    "Flowing and source fluids must share their registered family");
            var inventory = new MachineInventory(1, () -> {}, (slot, item) -> true);
            inventory.set(0, ItemResource.of(family.bucket().get()), 1);
            var handler =
                    ItemAccess.forHandlerIndexStrict(inventory, 0)
                            .getCapability(Capabilities.Fluid.ITEM);
            helper.assertTrue(
                    handler != null && handler.getAmountAsInt(0) == 1000,
                    "Bucket must expose native fluid capability: " + definition.id());
            try (var transaction = Transaction.openRoot()) {
                helper.assertTrue(
                        handler.extract(
                                        0,
                                        FluidResource.of(family.source().get()),
                                        1000,
                                        transaction)
                                == 1000,
                        "Bucket must drain exactly one bucket");
                helper.assertTrue(
                        inventory.stack(0).is(Items.BUCKET), "Draining must return empty bucket");
            }
            helper.assertTrue(
                    inventory.stack(0).is(family.bucket().get()),
                    "Aborted bucket drain must restore the original item");
        }
        helper.succeed();
    }

    static void cells(GameTestHelper helper) {
        var inventory = new MachineInventory(2, () -> {}, (slot, item) -> true);
        inventory.set(0, ItemResource.of(ModCells.EMPTY.get()), 64);
        var port = new ResourcePort<>(inventory, slot -> slot == 1, slot -> slot == 0);
        var access = ItemAccess.forHandlerIndex(port, 0).oneByOne();
        var handler = access.getCapability(Capabilities.Fluid.ITEM);
        var water = FluidResource.of(Fluids.WATER);
        try (var simulation = Transaction.openRoot()) {
            helper.assertTrue(
                    handler.insert(0, water, 999, simulation) == 0,
                    "Whole-cell filling must reject a partial bucket");
            helper.assertTrue(
                    handler.insert(0, water, 1000, simulation) == 1000,
                    "One-by-one access must fill exactly one cell");
            helper.assertTrue(
                    inventory.getAmountAsInt(0) == 63
                            && inventory.stack(1).is(ModCells.WATER.get()),
                    "Filled cell must move into output");
        }
        helper.assertTrue(
                inventory.getAmountAsInt(0) == 64 && inventory.stack(1).isEmpty(),
                "Simulation must restore both item slots");
        inventory.set(1, ItemResource.of(Items.COBBLESTONE), 64);
        try (var transaction = Transaction.openRoot()) {
            helper.assertTrue(
                    handler.insert(0, water, 1000, transaction) == 0,
                    "Blocked output must reject filling");
            transaction.commit();
        }
        helper.assertTrue(
                inventory.getAmountAsInt(0) == 64,
                "Failed exchange must not consume the empty cell");
        inventory.set(1, ItemResource.EMPTY, 0);
        try (var transaction = Transaction.openRoot()) {
            handler.insert(0, water, 1000, transaction);
            transaction.commit();
        }
        var filledAccess = ItemAccess.forHandlerIndexStrict(inventory, 1).oneByOne();
        var filledHandler = filledAccess.getCapability(Capabilities.Fluid.ITEM);
        try (var transaction = Transaction.openRoot()) {
            helper.assertTrue(
                    filledHandler.extract(0, water, 500, transaction) == 0,
                    "Whole-cell draining must reject partial extraction");
            helper.assertTrue(
                    filledHandler.extract(0, water, 1000, transaction) == 1000,
                    "Full extraction must match its reported transfer");
            transaction.commit();
        }
        helper.assertTrue(
                inventory.stack(1).is(ModCells.EMPTY.get()),
                "Drained cell must retain its container");
        // A component imported with a partial amount must never become a free full bucket.
        inventory.set(
                1,
                ItemResource.of(ModCells.EMPTY.get())
                        .with(
                                ModDataComponents.FLUID,
                                FluidStackTemplate.fromNonEmptyStack(water.toStack(500))),
                1);
        helper.assertTrue(
                filledHandler.getAmountAsInt(0) == 500, "Component amount must be honored");
        try (var transaction = Transaction.openRoot()) {
            helper.assertTrue(
                    filledHandler.extract(0, water, 1000, transaction) == 500,
                    "Imported partial cell must report exactly what it contained");
            transaction.commit();
        }
        helper.succeed();
    }

    static void worldInteraction(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(1, 2, 1));
        level.setBlockAndUpdate(pos, ModMachines.block(MachineKind.CANNER).defaultBlockState());
        var machine = (CannerBlockEntity) level.getBlockEntity(pos);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, ModCells.WATER.toStack());
        var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        var result =
                level.getBlockState(pos)
                        .useItemOn(
                                player.getMainHandItem(),
                                level,
                                player,
                                InteractionHand.MAIN_HAND,
                                hit);
        helper.assertTrue(
                result.consumesAction()
                        && machine.inputTank().getAmountAsInt(0) == 1000
                        && player.getMainHandItem().is(ModCells.EMPTY.get()),
                "Right-click must fill the machine before opening its menu");
        var waterPos = pos.offset(2, 0, 0);
        level.setBlockAndUpdate(waterPos.below(), Blocks.STONE.defaultBlockState());
        player.setItemInHand(InteractionHand.MAIN_HAND, ModCells.WATER.toStack());
        var handler =
                ItemAccess.forPlayerInteraction(player, InteractionHand.MAIN_HAND)
                        .oneByOne()
                        .getCapability(Capabilities.Fluid.ITEM);
        helper.assertTrue(
                FluidUtil.tryPlaceFluid(handler, player, level, waterPos, true, null).getAmount()
                        == 1000,
                "Native world placement must consume a full cell");
        helper.assertTrue(
                player.getMainHandItem().is(ModCells.EMPTY.get())
                        && level.getFluidState(waterPos).isSource(),
                "Placed fluid must preserve the empty container");
        helper.assertTrue(
                FluidUtil.tryPickupFluid(handler, player, level, waterPos, Direction.UP, null)
                                        .getAmount()
                                == 1000
                        && player.getMainHandItem().is(ModCells.WATER.get()),
                "Picking up the source must restore the filled cell");
        helper.succeed();
    }

    private FluidTests() {}
}
