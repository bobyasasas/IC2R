package ic2.neoforge.item.tfbp;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/** Legacy TerraformerBase: one world-editing program per terraforming blueprint. */
public abstract class TerraformerProgram {

    protected static boolean isVanilla(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block).getNamespace().equals("minecraft");
    }

    abstract boolean terraform(Level world, BlockPos pos);
}
