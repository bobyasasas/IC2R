package ic2.neoforge.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Legacy Ic2GlassBlock: like vanilla glass but blast-resistant, and — getBlockSupportShape —
 * nothing can be mounted on it, so a luminator hung on reinforced glass pops off.
 */
public class ReinforcedGlassBlock extends HalfTransparentBlock {
    public ReinforcedGlassBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }
}
