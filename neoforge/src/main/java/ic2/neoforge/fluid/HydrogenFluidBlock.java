package ic2.neoforge.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.redstone.Orientation;

import javax.annotation.Nullable;

/** Legacy HydrogenBlock: hydrogen detonates (2.0 block-breaking explosion) when a fire
 *  block sits anywhere in the 3x3x3 volume around a hydrogen source. */
public class HydrogenFluidBlock extends LiquidBlock {
    public HydrogenFluidBlock(FlowingFluid fluid, Properties properties) {
        super(fluid, properties);
    }

    private static void checkFireAndExplode(Level level, BlockPos pos) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    BlockPos neighborPos = pos.offset(dx, dy, dz);
                    if (level.getBlockState(neighborPos).is(Blocks.FIRE)) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                        level.explode(
                                null,
                                pos.getX() + 0.5,
                                pos.getY() + 0.5,
                                pos.getZ() + 0.5,
                                2.0F,
                                Level.ExplosionInteraction.BLOCK);
                        return;
                    }
                }
            }
        }
    }

    @Override
    protected void entityInside(
            BlockState state,
            Level level,
            BlockPos pos,
            Entity entity,
            net.minecraft.world.entity.InsideBlockEffectApplier applier,
            boolean isPrecise) {
    }

    @Override
    protected void onPlace(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState oldState,
            boolean movedByPiston) {
        if (state.getValue(LiquidBlock.LEVEL) == 0) {
            checkFireAndExplode(level, pos);
        }
    }

    @Override
    public void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block neighborBlock,
            @Nullable Orientation orientation,
            boolean movedByPiston) {
        if (state.getValue(LiquidBlock.LEVEL) == 0) {
            checkFireAndExplode(level, pos);
        }
    }
}
