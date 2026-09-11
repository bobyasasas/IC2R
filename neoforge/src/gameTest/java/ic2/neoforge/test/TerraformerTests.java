package ic2.neoforge.test;

import ic2.neoforge.machine.MachineBlock;
import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.machine.TerraformerBlockEntity;
import ic2.neoforge.registration.ModItems;
import ic2.neoforge.registration.ModMachines;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The dirt yards make the legacy random walk provably contained: the small yard hosts the
 * walk-free tests, while the 85x85 large yard holds the walk tests — worst-case reach from the
 * centre is one first spread of ±10 plus five ±5 steps, which stays inside the floor, so every
 * edit lands in the box and the chilling ledger is exact. The other programs are driven directly
 * on separate probe columns.
 */
final class TerraformerTests {
    private static final BlockPos POSITION = new BlockPos(12, 1, 12);
    private static final BlockPos LARGE_POSITION = new BlockPos(42, 1, 42);

    static void chillingLedger(GameTestHelper helper) {
        var machine = machine(helper, LARGE_POSITION);
        insert(machine, player(helper), new ItemStack(ModItems.CHILLING_TFBP.get()));
        machine.energy().insert(TerraformerBlockEntity.CAPACITY);
        // Six ticks keep the walk at most one first spread of ±10 plus five ±5 steps from the
        // centre — always inside the 85x85 yard — so every tick edits the yard and pays the
        // exact 2000 EU price.
        for (int tick = 0; tick < 6; tick++) machine.serverTick(helper.getLevel());
        helper.assertValueEqual(
                (int) machine.energy().stored(),
                TerraformerBlockEntity.CAPACITY - 6 * 2000,
                "Six successful edits drain exactly 12000 EU");
        helper.assertTrue(
                machine.getBlockState().getValue(MachineBlock.ACTIVE),
                "A fed terraformer reports active");
        helper.assertTrue(anySnow(helper), "The terrain actually froze");
        helper.succeed();
    }

    static void energyGate(GameTestHelper helper) {
        var machine = machine(helper, LARGE_POSITION);
        insert(machine, player(helper), new ItemStack(ModItems.CHILLING_TFBP.get()));
        machine.energy().insert(1999);
        for (int tick = 0; tick < 5; tick++) machine.serverTick(helper.getLevel());
        helper.assertValueEqual(
                (int) machine.energy().stored(), 1999, "Below the price the machine must idle");
        helper.assertTrue(
                !machine.getBlockState().getValue(MachineBlock.ACTIVE), "Idling stays inactive");
        machine.energy().insert(1);
        machine.serverTick(helper.getLevel());
        helper.assertValueEqual(
                (int) machine.energy().stored(), 0, "The next tick buys one 2000 EU edit");
        helper.assertTrue(
                machine.getBlockState().getValue(MachineBlock.ACTIVE), "Working means active");
        helper.succeed();
    }

    static void blankBlueprint(GameTestHelper helper) {
        var machine = machine(helper);
        insert(machine, player(helper), new ItemStack(ModItems.BLANK_TFBP.get()));
        machine.energy().insert(50000);
        for (int tick = 0; tick < 10; tick++) machine.serverTick(helper.getLevel());
        helper.assertValueEqual(
                (int) machine.energy().stored(), 50000, "The blank cartridge burns nothing");
        helper.assertTrue(
                machine.getBlockState().getValue(MachineBlock.ACTIVE),
                "Legacy keeps a blank terraformer running");
        helper.succeed();
    }

    static void handInsertEject(GameTestHelper helper) {
        var machine = machine(helper);
        var player = player(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND,
                new ItemStack(ModItems.DESERTIFICATION_TFBP.get()));
        machine.useFromHand(player);
        helper.assertTrue(
                machine.blueprint().is(ModItems.DESERTIFICATION_TFBP.get()),
                "Right-click inserts a held blueprint");
        helper.assertTrue(player.getMainHandItem().isEmpty(), "The hand gives up its cartridge");
        machine.useFromHand(player);
        helper.assertTrue(machine.blueprint().isEmpty(), "Right-click again ejects it");
        helper.assertTrue(
                helper.getEntities(EntityType.ITEM).stream()
                        .anyMatch(entity -> entity.getItem().is(ModItems.DESERTIFICATION_TFBP.get())),
                "The ejected cartridge lands as an item entity");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIRT));
        machine.useFromHand(player);
        helper.assertTrue(machine.blueprint().isEmpty(), "Non-blueprints stay out");
        helper.succeed();
    }

    static void programTransforms(GameTestHelper helper) {
        var level = helper.getLevel();
        helper.assertTrue(
                level.getBlockState(helper.absolutePos(new BlockPos(4, 0, 4))).is(Blocks.DIRT),
                "the yard floor is loaded");
        var cultivation = ModItems.CULTIVATION_TFBP.get();
        var desertification = ModItems.DESERTIFICATION_TFBP.get();
        var irrigation = ModItems.IRRIGATION_TFBP.get();
        var chilling = ModItems.CHILLING_TFBP.get();
        var flatification = ModItems.FLATIFICATION_TFBP.get();
        var mushroom = ModItems.MUSHROOM_TFBP.get();

        BlockPos probe = helper.absolutePos(new BlockPos(4, 1, 4));
        helper.assertTrue(cultivation.terraform(level, probe), "cultivation runs");
        helper.assertTrue(
                level.getBlockState(probe.below()).is(Blocks.GRASS_BLOCK),
                "cultivation turns dirt into grass");

        BlockPos dry = helper.absolutePos(new BlockPos(4, 1, 12));
        helper.setBlock(new BlockPos(4, 0, 12), Blocks.GRASS_BLOCK);
        helper.assertTrue(desertification.terraform(level, dry), "desertification runs");
        helper.assertTrue(
                level.getBlockState(dry.below()).is(Blocks.SAND), "desertification dries grass");

        BlockPos wet = helper.absolutePos(new BlockPos(12, 1, 4));
        helper.setBlock(new BlockPos(12, 0, 4), Blocks.SAND);
        helper.assertTrue(irrigation.terraform(level, wet), "irrigation runs");
        helper.assertTrue(
                level.getBlockState(wet.below()).is(Blocks.DIRT), "irrigation softens sand");

        BlockPos cold = helper.absolutePos(new BlockPos(12, 1, 20));
        helper.setBlock(new BlockPos(12, 0, 20), Blocks.WATER);
        helper.assertTrue(chilling.terraform(level, cold), "chilling runs");
        helper.assertTrue(level.getBlockState(cold.below()).is(Blocks.ICE), "chilling freezes water");

        BlockPos flat = helper.absolutePos(new BlockPos(20, 4, 12));
        helper.assertTrue(flatification.terraform(level, flat), "flatification fills");
        helper.assertTrue(
                level.getBlockState(helper.absolutePos(new BlockPos(20, 1, 12))).is(Blocks.DIRT),
                "flatification raises low ground to the machine level");
        helper.setBlock(new BlockPos(20, 3, 12), Blocks.SHORT_GRASS);
        // The shave probe sits below the plant: Flatification only removes blocks found above
        // its reference level, lower finds get raised instead.
        BlockPos shave = helper.absolutePos(new BlockPos(20, 1, 12));
        helper.assertTrue(flatification.terraform(level, shave), "flatification shaves");
        helper.assertTrue(
                level.getBlockState(helper.absolutePos(new BlockPos(20, 3, 12))).isAir(),
                "flatification removes plants");

        BlockPos shroom = helper.absolutePos(new BlockPos(20, 1, 20));
        helper.assertTrue(mushroom.terraform(level, shroom), "mushroom runs");
        helper.assertTrue(
                level.getBlockState(helper.absolutePos(new BlockPos(18, 0, 18))).is(Blocks.MYCELIUM),
                "mushroom seeds the north-west strip with mycelium");
        helper.succeed();
    }

    private static TerraformerBlockEntity machine(GameTestHelper helper) {
        return machine(helper, POSITION);
    }

    private static TerraformerBlockEntity machine(GameTestHelper helper, BlockPos position) {
        helper.setBlock(position, ModMachines.block(MachineKind.TERRAFORMER));
        return helper.getBlockEntity(position, TerraformerBlockEntity.class);
    }

    private static Player player(GameTestHelper helper) {
        return helper.makeMockPlayer(GameType.SURVIVAL);
    }

    private static void insert(
            TerraformerBlockEntity machine, Player player, ItemStack stack) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        machine.useFromHand(player);
    }

    private static boolean anySnow(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        for (int x = 0; x < 85; x++) {
            for (int z = 0; z < 85; z++) {
                for (int y = 1; y < 4; y++) {
                    BlockState state = level.getBlockState(helper.absolutePos(new BlockPos(x, y, z)));
                    if (state.is(Blocks.SNOW) || state.is(Blocks.SNOW_BLOCK)) return true;
                }
            }
        }
        return false;
    }

    private TerraformerTests() {}
}
