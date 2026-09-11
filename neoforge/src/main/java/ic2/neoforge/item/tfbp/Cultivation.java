package ic2.neoforge.item.tfbp;

import ic2.neoforge.machine.TerraformerBlockEntity;
import ic2.neoforge.registration.ModWorldContent;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

import java.util.ArrayList;
import java.util.List;

/** Legacy Cultivation: dirt spreads over sand/end stone and grass sprouts a random plant. */
public final class Cultivation extends TerraformerProgram {
    // Legacy fills this from the item registry hook before any world runs; the port builds it on
    // first use because sapling tags are not loaded at registration time.
    private static List<BlockState> plants;

    static List<BlockState> plants() {
        if (plants == null) {
            List<BlockState> list = new ArrayList<>();
            list.add(Blocks.SHORT_GRASS.defaultBlockState());
            list.add(Blocks.SHORT_GRASS.defaultBlockState());
            list.add(Blocks.FERN.defaultBlockState());
            list.add(Blocks.POPPY.defaultBlockState());
            list.add(Blocks.DANDELION.defaultBlockState());
            list.add(Blocks.SHORT_GRASS.defaultBlockState());
            list.add(Blocks.ROSE_BUSH.defaultBlockState());
            list.add(Blocks.SUNFLOWER.defaultBlockState());
            for (Holder<Block> entry : BuiltInRegistries.BLOCK.getTagOrEmpty(BlockTags.SAPLINGS)) {
                Block block = entry.value();
                if (isVanilla(block)) {
                    list.add(block.defaultBlockState());
                }
            }
            list.add(Blocks.WHEAT.defaultBlockState());
            list.add(Blocks.RED_MUSHROOM.defaultBlockState());
            list.add(Blocks.BROWN_MUSHROOM.defaultBlockState());
            list.add(Blocks.PUMPKIN.defaultBlockState());
            list.add(Blocks.MELON.defaultBlockState());
            list.add(ModWorldContent.RUBBER_SAPLING.get().defaultBlockState());
            plants = list;
        }
        return plants;
    }

    private static boolean growPlantsOn(Level world, BlockPos pos) {
        RandomSource rng = world.getRandom();
        BlockPos above = pos.above();
        BlockState state = world.getBlockState(above);
        Block block = state.getBlock();
        if (state.isAir() || block == Blocks.SHORT_GRASS && rng.nextInt(4) == 0) {
            BlockState plant = pickRandomPlant(rng);
            if (plant.hasProperty(DirectionalBlock.FACING)) {
                plant = plant.setValue(
                        DirectionalBlock.FACING, Direction.from2DDataValue(rng.nextInt(4)));
            }
            if (plant.getBlock() instanceof CropBlock) {
                world.setBlockAndUpdate(pos, Blocks.FARMLAND.defaultBlockState());
            } else if (plant.getBlock() instanceof DoublePlantBlock) {
                world.setBlockAndUpdate(
                        above, plant.setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER));
                world.setBlockAndUpdate(
                        above.above(), plant.setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER));
                return true;
            }
            world.setBlockAndUpdate(above, plant);
            return true;
        }
        return false;
    }

    private static BlockState pickRandomPlant(RandomSource random) {
        List<BlockState> list = plants();
        return list.get(random.nextInt(list.size()));
    }

    @Override
    boolean terraform(Level world, BlockPos pos) {
        pos = TerraformerBlockEntity.getFirstSolidBlockFrom(world, pos, 10);
        if (pos == null) {
            return false;
        }
        if (TerraformerBlockEntity.switchGround(
                world, pos, Blocks.SAND, Blocks.DIRT.defaultBlockState(), true)) {
            return true;
        }
        if (TerraformerBlockEntity.switchGround(
                world, pos, Blocks.END_STONE, Blocks.DIRT.defaultBlockState(), true)) {
            int i = 4;
            while (--i > 0
                    && TerraformerBlockEntity.switchGround(
                            world, pos, Blocks.END_STONE, Blocks.DIRT.defaultBlockState(), true)) {}
        }
        Block block = world.getBlockState(pos).getBlock();
        if (block == Blocks.DIRT) {
            world.setBlockAndUpdate(pos, Blocks.GRASS_BLOCK.defaultBlockState());
            return true;
        }
        return block == Blocks.GRASS_BLOCK ? growPlantsOn(world, pos) : false;
    }
}
