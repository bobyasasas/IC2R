package ic2.neoforge.item.tfbp;

import ic2.neoforge.machine.TerraformerBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Legacy Desertification: dries dirt/grass into sand and burns off the local flora. */
public final class Desertification extends TerraformerProgram {

    private static boolean isPlant(Block block) {
        for (BlockState state : Cultivation.plants()) {
            if (state.getBlock() == block) {
                return true;
            }
        }
        return block.builtInRegistryHolder().is(BlockTags.SAPLINGS)
                || block.builtInRegistryHolder().is(BlockTags.CROPS)
                || block.builtInRegistryHolder().is(BlockTags.FLOWERS);
    }

    @Override
    boolean terraform(Level world, BlockPos pos) {
        RandomSource rng = world.getRandom();
        pos = TerraformerBlockEntity.getFirstBlockFrom(world, pos, 10);
        if (pos == null) {
            return false;
        }
        BlockState sand = Blocks.SAND.defaultBlockState();
        if (!TerraformerBlockEntity.switchGround(world, pos, Blocks.DIRT, sand, false)
                && !TerraformerBlockEntity.switchGround(world, pos, Blocks.GRASS_BLOCK, sand, false)
                && !TerraformerBlockEntity.switchGround(world, pos, Blocks.FARMLAND, sand, false)) {
            BlockState state = world.getBlockState(pos);
            Block block = state.getBlock();
            if (block == Blocks.WATER
                    || block == Blocks.SNOW
                    || state.is(BlockTags.LEAVES)
                    || isPlant(block)) {
                world.removeBlock(pos, false);
                if (isPlant(world.getBlockState(pos.above()).getBlock())) {
                    world.removeBlock(pos.above(), false);
                }
                return true;
            } else if (block != Blocks.ICE && block != Blocks.SNOW) {
                if ((state.is(BlockTags.PLANKS) || state.is(BlockTags.LOGS_THAT_BURN))
                        && rng.nextInt(15) == 0) {
                    world.setBlockAndUpdate(pos, Blocks.FIRE.defaultBlockState());
                    return true;
                }
                return false;
            } else {
                world.setBlockAndUpdate(pos, Blocks.WATER.defaultBlockState());
                return true;
            }
        }
        TerraformerBlockEntity.switchGround(world, pos, Blocks.DIRT, sand, false);
        return true;
    }
}
