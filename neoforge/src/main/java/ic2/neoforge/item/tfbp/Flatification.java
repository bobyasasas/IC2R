package ic2.neoforge.item.tfbp;

import ic2.neoforge.machine.TerraformerBlockEntity;
import ic2.neoforge.registration.ModWorldContent;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/** Legacy Flatification: shaves everything down to the machine's own level. */
public final class Flatification extends TerraformerProgram {
    // Legacy fills this from the item registry hook; the port builds it on first use.
    private static Set<Block> removable;

    private static Set<Block> removable() {
        if (removable == null) {
            Set<Block> set = Collections.newSetFromMap(new IdentityHashMap<>());
            set.add(Blocks.SNOW);
            set.add(Blocks.ICE);
            set.add(Blocks.GRASS_BLOCK);
            set.add(Blocks.STONE);
            set.add(Blocks.GRAVEL);
            set.add(Blocks.SAND);
            set.add(Blocks.DIRT);
            set.add(Blocks.OAK_LEAVES);
            set.add(Blocks.SPRUCE_LEAVES);
            set.add(Blocks.BIRCH_LEAVES);
            set.add(Blocks.JUNGLE_LEAVES);
            set.add(Blocks.ACACIA_LEAVES);
            set.add(Blocks.DARK_OAK_LEAVES);
            set.add(Blocks.SHORT_GRASS);
            set.add(Blocks.POPPY);
            set.add(Blocks.DANDELION);
            set.add(Blocks.WHEAT);
            set.add(Blocks.RED_MUSHROOM);
            set.add(Blocks.BROWN_MUSHROOM);
            set.add(Blocks.PUMPKIN);
            set.add(Blocks.MELON);
            set.add(ModWorldContent.RUBBER_LEAVES.get());
            set.add(ModWorldContent.RUBBER_SAPLING.get());
            set.add(ModWorldContent.RUBBER_LOG.get());
            removable = set;
        }
        return removable;
    }

    private static boolean canRemove(Block block) {
        return removable().contains(block)
                || block.builtInRegistryHolder().is(BlockTags.SAPLINGS)
                || block.builtInRegistryHolder().is(BlockTags.LOGS);
    }

    @Override
    boolean terraform(Level world, BlockPos pos) {
        BlockPos workPos = TerraformerBlockEntity.getFirstBlockFrom(world, pos, 20);
        if (workPos == null) {
            return false;
        }
        if (world.getBlockState(workPos).getBlock() == Blocks.SNOW) {
            workPos = workPos.below();
        }
        if (pos.getY() == workPos.getY()) {
            return false;
        } else if (workPos.getY() < pos.getY()) {
            world.setBlockAndUpdate(workPos.above(), Blocks.DIRT.defaultBlockState());
            return true;
        } else if (canRemove(world.getBlockState(workPos).getBlock())) {
            world.removeBlock(workPos, false);
            return true;
        }
        return false;
    }
}
