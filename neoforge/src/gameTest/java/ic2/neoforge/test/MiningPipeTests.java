package ic2.neoforge.test;

import ic2.neoforge.registration.ModMaterialBlocks;
import ic2.neoforge.world.MiningPipeBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

final class MiningPipeTests {
    private static final BlockPos POSITION = new BlockPos(2, 1, 2);

    static void pipeShapeAndTools(GameTestHelper helper) {
        helper.setBlock(POSITION, ModMaterialBlocks.MINING_PIPE.get());
        var state = helper.getBlockState(POSITION);
        helper.assertTrue(state.getBlock() instanceof MiningPipeBlock, "The item places a pipe");
        var shape = state.getShape(helper.getLevel(), helper.absolutePos(POSITION));
        helper.assertTrue(
                shape.min(Direction.Axis.X) == 0.375 && shape.max(Direction.Axis.X) == 0.625,
                "The pipe keeps its legacy quarter-block column cross section");
        helper.assertTrue(
                shape.min(Direction.Axis.Y) == 0.0 && shape.max(Direction.Axis.Y) == 1.0,
                "The pipe spans the full height of its cell");
        helper.assertTrue(
                state.useShapeForLightOcclusion(), "The narrow shape bounds light occlusion");
        helper.assertTrue(
                state.is(BlockTags.MINEABLE_WITH_PICKAXE) && state.is(BlockTags.NEEDS_STONE_TOOL),
                "The pipe mines like the legacy stone-tool pickaxe block");
        var item = ModMaterialBlocks.MINING_PIPE.get().asItem();
        helper.assertTrue(
                item instanceof BlockItem block && block.getBlock() == state.getBlock(),
                "The pipe block item places the pipe block");
        helper.succeed();
    }

    static void tipIsPlaceOnly(GameTestHelper helper) {
        helper.setBlock(POSITION, ModMaterialBlocks.MINING_PIPE_TIP.get());
        var state = helper.getBlockState(POSITION);
        helper.assertTrue(
                state.getBlock() == ModMaterialBlocks.MINING_PIPE_TIP.get(),
                "The tip registers as its own block");
        helper.assertTrue(
                state.getBlock() != ModMaterialBlocks.MINING_PIPE.get(),
                "The tip is not the pipe block");
        helper.assertTrue(
                state.is(BlockTags.MINEABLE_WITH_PICKAXE) && state.is(BlockTags.NEEDS_STONE_TOOL),
                "The tip keeps the legacy tool requirements");
        helper.assertTrue(
                ModMaterialBlocks.MINING_PIPE_TIP.get().asItem() == Items.AIR,
                "The tip keeps no item form, matching legacy");
        helper.succeed();
    }

    static void breakingDropsNothing(GameTestHelper helper) {
        var level = helper.getLevel();
        helper.setBlock(POSITION, ModMaterialBlocks.MINING_PIPE.get());
        level.destroyBlock(helper.absolutePos(POSITION), true);
        helper.assertTrue(
                level.getEntities(
                                EntityType.ITEM,
                                new AABB(helper.absolutePos(POSITION)).inflate(2.0),
                                Entity::isAlive)
                        .isEmpty(),
                "Breaking a pipe drops nothing; recovery goes through the miner withdraw mode");
        helper.setBlock(POSITION, ModMaterialBlocks.MINING_PIPE_TIP.get());
        level.destroyBlock(helper.absolutePos(POSITION), true);
        helper.assertTrue(
                level.getEntities(
                                EntityType.ITEM,
                                new AABB(helper.absolutePos(POSITION)).inflate(2.0),
                                Entity::isAlive)
                        .isEmpty(),
                "The tip equally drops nothing");
        helper.succeed();
    }
}
