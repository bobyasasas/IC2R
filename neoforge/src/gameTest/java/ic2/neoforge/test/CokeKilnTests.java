package ic2.neoforge.test;

import ic2.neoforge.fluid.FluidDefinition;
import ic2.neoforge.machine.CokeKilnBlockEntity;
import ic2.neoforge.machine.CokeKilnGrateBlockEntity;
import ic2.neoforge.machine.CokeKilnHatchBlockEntity;
import ic2.neoforge.machine.MachineBlock;
import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.menu.MachineMenu;
import ic2.neoforge.registration.ModFluids;
import ic2.neoforge.registration.ModItems;
import ic2.neoforge.registration.ModMaterialBlocks;
import ic2.neoforge.registration.ModMachines;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/** The 3x3x3 coke kiln: charring rates, housing integrity and the hatch/grate ports. */
final class CokeKilnTests {
    private static final BlockPos KILN = new BlockPos(8, 3, 7);
    // The kiln faces north by default, so its hollow interior sits one block south.
    private static final BlockPos CENTRE = new BlockPos(8, 3, 8);
    private static final BlockPos HATCH = CENTRE.above();
    private static final BlockPos GRATE = CENTRE.below();

    private static void buildStructure(GameTestHelper helper) {
        helper.setBlock(KILN, ModMachines.block(MachineKind.COKE_KILN));
        buildHousing(helper);
    }

    /** Every housing block around an already placed kiln controller. */
    private static void buildHousing(GameTestHelper helper) {
        var bricks = ModMaterialBlocks.REFRACTORY_BRICKS.get();
        helper.setBlock(HATCH, ModMachines.block(MachineKind.COKE_KILN_HATCH));
        helper.setBlock(GRATE, ModMachines.block(MachineKind.COKE_KILN_GRATE));
        for (int x = -1; x <= 1; x++)
            for (int z = -1; z <= 1; z++) {
                if (x == 0 && z == 0) continue;
                BlockPos ring = CENTRE.offset(x, 0, z);
                helper.setBlock(ring.below(), bricks);
                helper.setBlock(ring.above(), bricks);
                if (!ring.equals(KILN)) helper.setBlock(ring, bricks);
            }
    }

    private static CokeKilnBlockEntity kiln(GameTestHelper helper) {
        return helper.getBlockEntity(KILN, CokeKilnBlockEntity.class);
    }

    private static CokeKilnHatchBlockEntity hatch(GameTestHelper helper) {
        return helper.getBlockEntity(HATCH, CokeKilnHatchBlockEntity.class);
    }

    private static CokeKilnGrateBlockEntity grate(GameTestHelper helper) {
        return helper.getBlockEntity(GRATE, CokeKilnGrateBlockEntity.class);
    }

    /**
     * The kiln validates and produces once every 20 ticks at a per-kiln random phase; 20
     * consecutive calls guarantee exactly one pass.
     */
    private static void passes(CokeKilnBlockEntity machine, GameTestHelper helper, int passes) {
        for (int pass = 0; pass < passes * CokeKilnBlockEntity.TICK_RATE; pass++)
            machine.serverTick(helper.getLevel());
    }

    private static FluidResource creosote() {
        return FluidResource.of(ModFluids.FAMILIES.get(FluidDefinition.CREOSOTE).source().get());
    }

    static void charsCoalIntoCoke(GameTestHelper helper) {
        buildStructure(helper);
        var machine = kiln(helper);
        hatch(helper).inventory().set(0, ItemResource.of(Items.COAL), 2);
        passes(machine, helper, 90);
        helper.assertTrue(
                machine.inventory().stack(0).is(ModItems.COKE.get())
                        && machine.inventory().stack(0).getCount() == 1,
                "One coal must char into exactly one coke");
        helper.assertTrue(
                grate(helper).tank().getAmountAsInt(0) == 500
                        && grate(helper).tank().getResource(0).equals(creosote()),
                "Charring one coal must collect exactly 500 mB of creosote in the grate");
        helper.assertTrue(
                hatch(helper).input().getCount() == 1
                        && helper.getBlockState(KILN).getValue(MachineBlock.ACTIVE),
                "The kiln must keep working while input remains");
        passes(machine, helper, 90);
        helper.assertTrue(
                machine.inventory().stack(0).getCount() == 2
                        && grate(helper).tank().getAmountAsInt(0) == 1000
                        && hatch(helper).input().isEmpty(),
                "Two coals must yield two coke and 1000 mB of creosote");
        passes(machine, helper, 2);
        helper.assertTrue(
                !helper.getBlockState(KILN).getValue(MachineBlock.ACTIVE),
                "An empty hatch must idle the kiln");
        try (var transaction = Transaction.openRoot()) {
            helper.assertTrue(
                    machine.automation(Direction.NORTH)
                                    .insert(0, ItemResource.of(ModItems.COKE.get()), 1, transaction)
                            == 0,
                    "The coke kiln output slot must reject automation inserts");
            helper.assertTrue(
                    machine.automation(Direction.NORTH)
                                    .extract(
                                            0,
                                            ItemResource.of(ModItems.COKE.get()),
                                            1,
                                            transaction)
                            == 1,
                    "The coke kiln output slot must extract to automation from any face");
        }
        var player = helper.makeMockPlayer(GameType.CREATIVE);
        player.setPos(
                machine.getBlockPos().getX() + .5,
                machine.getBlockPos().getY(),
                machine.getBlockPos().getZ() + .5);
        var menu = new MachineMenu(1, player.getInventory(), machine);
        player.containerMenu = menu;
        helper.assertTrue(
                menu.slots.size() == 37,
                "The kiln menu must show its single output slot plus the player inventory");
        int burnTime =
                new ItemStack(ModItems.COKE.get())
                        .getBurnTime(RecipeType.SMELTING, helper.getLevel().fuelValues());
        helper.assertTrue(
                burnTime == 3200,
                "Coke must register as furnace fuel at the legacy 3200 tick burn time");
        helper.succeed();
    }

    static void charsLogsIntoCharcoal(GameTestHelper helper) {
        buildStructure(helper);
        var machine = kiln(helper);
        hatch(helper).inventory().set(0, ItemResource.of(Items.OAK_LOG), 1);
        passes(machine, helper, 90);
        helper.assertTrue(
                machine.inventory().stack(0).is(Items.CHARCOAL)
                        && machine.inventory().stack(0).getCount() == 1,
                "Any log must char into charcoal");
        helper.assertTrue(
                grate(helper).tank().getAmountAsInt(0) == 250
                        && grate(helper).tank().getResource(0).equals(creosote()),
                "Charring a log must collect exactly 250 mB of creosote");
        helper.assertTrue(hatch(helper).input().isEmpty(), "One log must be fully consumed");
        helper.succeed();
    }

    static void structureAndPortGates(GameTestHelper helper) {
        helper.setBlock(KILN, ModMachines.block(MachineKind.COKE_KILN));
        var machine = kiln(helper);
        passes(machine, helper, 3);
        helper.assertTrue(
                machine.progress() == 0
                        && !helper.getBlockState(KILN).getValue(MachineBlock.ACTIVE),
                "A lone kiln block must stay idle without its housing");
        buildHousing(helper);
        hatch(helper).inventory().set(0, ItemResource.of(Items.COAL), 1);
        passes(machine, helper, 90);
        helper.assertTrue(
                machine.inventory().stack(0).is(ModItems.COKE.get()),
                "The formed housing must let the kiln char coal");
        helper.setBlock(KILN.east().above(), Blocks.AIR);
        passes(machine, helper, 3);
        helper.assertTrue(
                machine.progress() == 0
                        && !helper.getBlockState(KILN).getValue(MachineBlock.ACTIVE),
                "Losing any housing brick must reset the current operation");
        var hatch = hatch(helper);
        Direction facing = hatch.getBlockState().getValue(MachineBlock.FACING);
        try (var transaction = Transaction.openRoot()) {
            helper.assertTrue(
                    hatch.automation(facing)
                                    .insert(0, ItemResource.of(Items.COAL), 4, transaction)
                            == 4,
                    "The hatch must accept coal through its own face");
            helper.assertTrue(
                    hatch.automation(facing.getOpposite())
                                    .insert(0, ItemResource.of(Items.COAL), 1, transaction)
                            == 0,
                    "Every hatch face except its own must reject inserts");
            helper.assertTrue(
                    hatch.automation(facing)
                                    .extract(0, hatch.inventory().getResource(0), 1, transaction)
                            == 0,
                    "The hatch must never release its input to automation");
            transaction.commit();
        }
        helper.assertTrue(
                hatch.input().getCount() == 4,
                "Rejected and rolled-back port moves must leave the hatch untouched");
        helper.succeed();
    }

    static void grateTankAndAutomation(GameTestHelper helper) {
        buildStructure(helper);
        var grate = grate(helper);
        var port = grate.fluidAutomation(Direction.EAST);
        try (var transaction = Transaction.openRoot()) {
            helper.assertTrue(
                    port.insert(0, creosote(), 1000, transaction) == 1000,
                    "The grate tank must accept creosote through pipes from any face");
            helper.assertTrue(
                    port.insert(0, FluidResource.of(Fluids.WATER), 1000, transaction) == 0,
                    "The single-slot grate tank must not mix in a second fluid");
            transaction.commit();
        }
        try (var transaction = Transaction.openRoot()) {
            helper.assertTrue(
                    port.extract(0, creosote(), 1000, transaction) == 1000,
                    "The grate tank must drain through pipes from any face");
            transaction.commit();
        }
        var player = helper.makeMockPlayer(GameType.CREATIVE);
        player.setPos(
                grate.getBlockPos().getX() + .5,
                grate.getBlockPos().getY(),
                grate.getBlockPos().getZ() + .5);
        var menu = new MachineMenu(1, player.getInventory(), grate);
        player.containerMenu = menu;
        menu.setCarried(new ItemStack(Items.WATER_BUCKET));
        helper.assertTrue(
                menu.clickMenuButton(player, 0)
                        && grate.tank().getAmountAsInt(0) == 1000
                        && menu.getCarried().is(Items.BUCKET),
                "The grate GUI must drain a carried bucket into the tank one click at a time");
        menu.setCarried(new ItemStack(Items.BUCKET));
        helper.assertTrue(
                menu.clickMenuButton(player, 1)
                        && grate.tank().getAmountAsInt(0) == 0
                        && menu.getCarried().is(Items.WATER_BUCKET),
                "Shift-clicking must move the tank's whole content into the carried bucket");
        helper.assertTrue(!menu.clickMenuButton(player, 7), "Unknown grate buttons must be rejected");
        try (var transaction = Transaction.openRoot()) {
            helper.assertTrue(
                    port.insert(
                                    0,
                                    creosote(),
                                    CokeKilnGrateBlockEntity.CAPACITY + 5000,
                                    transaction)
                            == CokeKilnGrateBlockEntity.CAPACITY,
                    "The grate tank must cap at the legacy 64 bucket capacity");
        }
        helper.assertTrue(
                grate.tank().getAmountAsInt(0) == 0,
                "A rolled-back fill must leave the tank empty");
        helper.assertTrue(
                grate.automation(Direction.DOWN).size() == 0,
                "The grate must expose no item automation at all");
        helper.succeed();
    }

    static void persistence(GameTestHelper helper) {
        buildStructure(helper);
        var machine = kiln(helper);
        hatch(helper).inventory().set(0, ItemResource.of(Items.COAL), 2);
        passes(machine, helper, 45);
        helper.assertTrue(machine.progress() == 900, "Half a cycle must hold 900 progress");
        var saved = machine.saveWithFullMetadata(helper.getLevel().registryAccess());
        var restored =
                (CokeKilnBlockEntity)
                        BlockEntity.loadStatic(
                                machine.getBlockPos(),
                                machine.getBlockState(),
                                saved,
                                helper.getLevel().registryAccess());
        helper.assertTrue(
                restored.progress() == 900 && restored.progressMaximum() == 1800,
                "Progress and operation length must survive a restart");
        // Install the reloaded kiln so its structure identity check sees itself.
        helper.getLevel().setBlockEntity(restored);
        passes(restored, helper, 45);
        helper.assertTrue(
                restored.inventory().stack(0).is(ModItems.COKE.get())
                        && restored.inventory().stack(0).getCount() == 1,
                "A reloaded kiln must finish its remaining progress");
        helper.assertTrue(
                grate(helper).tank().getAmountAsInt(0) == 500,
                "The reloaded kiln must deliver creosote to the grate");
        var restoredHatch =
                (CokeKilnHatchBlockEntity)
                        BlockEntity.loadStatic(
                                hatch(helper).getBlockPos(),
                                hatch(helper).getBlockState(),
                                hatch(helper).saveWithFullMetadata(helper.getLevel().registryAccess()),
                                helper.getLevel().registryAccess());
        helper.assertTrue(
                restoredHatch.input().getCount() == 1,
                "Hatch contents must survive a restart");
        var restoredGrate =
                (CokeKilnGrateBlockEntity)
                        BlockEntity.loadStatic(
                                grate(helper).getBlockPos(),
                                grate(helper).getBlockState(),
                                grate(helper).saveWithFullMetadata(helper.getLevel().registryAccess()),
                                helper.getLevel().registryAccess());
        helper.assertTrue(
                restoredGrate.tank().getAmountAsInt(0) == 500
                        && restoredGrate.tank().getResource(0).equals(creosote()),
                "Grate fluid contents must survive a restart");
        helper.succeed();
    }

    private CokeKilnTests() {}
}
