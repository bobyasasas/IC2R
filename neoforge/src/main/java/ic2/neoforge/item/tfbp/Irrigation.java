package ic2.neoforge.item.tfbp;

import ic2.neoforge.machine.TerraformerBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Legacy Irrigation: turns sand to soil, bonemeals crops and grows trees, douses fires. */
public final class Irrigation extends TerraformerProgram {

    private static BlockState getLeaves(Level world, BlockPos pos) {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            BlockState state = world.getBlockState(pos.relative(facing));
            if (state.is(BlockTags.LEAVES)) {
                return state;
            }
        }
        return null;
    }

    private static void createLeaves(Level world, BlockPos pos, BlockState state) {
        BlockPos above = pos.above();
        if (world.isEmptyBlock(above)) {
            world.setBlockAndUpdate(above, state);
        }
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            BlockPos cPos = pos.relative(facing);
            if (world.isEmptyBlock(cPos)) {
                world.setBlockAndUpdate(cPos, state);
            }
        }
    }

    private static boolean spreadGrass(Level world, BlockPos pos) {
        RandomSource rng = world.getRandom();
        if (rng.nextBoolean()) {
            return false;
        }
        pos = TerraformerBlockEntity.getFirstBlockFrom(world, pos, 0);
        if (pos == null) {
            return false;
        }
        Block block = world.getBlockState(pos).getBlock();
        if (block == Blocks.DIRT) {
            world.setBlockAndUpdate(pos, Blocks.GRASS_BLOCK.defaultBlockState());
            return true;
        } else if (block == Blocks.GRASS_BLOCK) {
            world.setBlockAndUpdate(pos.above(), Blocks.SHORT_GRASS.defaultBlockState());
            return true;
        }
        return false;
    }

    @Override
    boolean terraform(Level world, BlockPos pos) {
        RandomSource rng = world.getRandom();
        if (rng.nextInt(48000) == 0) {
            // Legacy LevelData.setRaining(true); 26.1.2 routes weather through the server.
            ((ServerLevel) world)
                    .getServer()
                    .setWeatherParameters(
                            0, ServerLevel.RAIN_DURATION.sample(world.getRandom()), true, false);
            return true;
        }
        pos = TerraformerBlockEntity.getFirstBlockFrom(world, pos, 10);
        if (pos == null) {
            return false;
        }
        if (TerraformerBlockEntity.switchGround(
                world, pos, Blocks.SAND, Blocks.DIRT.defaultBlockState(), true)) {
            TerraformerBlockEntity.switchGround(
                    world, pos, Blocks.SAND, Blocks.DIRT.defaultBlockState(), true);
            return true;
        }
        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        if (block instanceof BonemealableBlock growable
                && growable.isValidBonemealTarget(world, pos, state)) {
            growable.performBonemeal((ServerLevel) world, world.getRandom(), pos, state);
            return true;
        }
        if (block != Blocks.SHORT_GRASS) {
            if (state.is(BlockTags.LOGS)) {
                BlockPos above = pos.above();
                world.setBlockAndUpdate(above, state);
                BlockState leaves = getLeaves(world, pos);
                if (leaves != null) {
                    createLeaves(world, above, leaves);
                }
                return true;
            } else if (block == Blocks.FIRE) {
                world.removeBlock(pos, false);
                return true;
            }
            return false;
        }
        return spreadGrass(world, pos.north())
                || spreadGrass(world, pos.east())
                || spreadGrass(world, pos.south())
                || spreadGrass(world, pos.west());
    }
}
