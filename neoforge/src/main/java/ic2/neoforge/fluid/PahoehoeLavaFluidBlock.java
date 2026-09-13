package ic2.neoforge.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.redstone.Orientation;

import javax.annotation.Nullable;

/** Legacy PahoehoeLavaBlock: pahoehoe lava burns like lava and solidifies to basalt, faster near water. */
public class PahoehoeLavaFluidBlock extends LiquidBlock {
    public PahoehoeLavaFluidBlock(FlowingFluid fluid, Properties properties) {
        super(fluid, properties);
    }

    private static boolean isTouchingWater(LevelAccessor level, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            if (level.getFluidState(pos.relative(dir)).is(FluidTags.WATER)) {
                return true;
            }
        }

        return false;
    }

    @Override
    protected void onPlace(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState oldState,
            boolean movedByPiston) {
        if (state.getValue(LiquidBlock.LEVEL) == 0) {
            level.scheduleTick(pos, this, 360);
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(LiquidBlock.LEVEL) == 0) {
            level.setBlockAndUpdate(pos, Blocks.BASALT.defaultBlockState());
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
        super.neighborChanged(state, level, pos, neighborBlock, orientation, movedByPiston);
        if (state.getValue(LiquidBlock.LEVEL) == 0 && isTouchingWater(level, pos)) {
            level.setBlockAndUpdate(pos, Blocks.BASALT.defaultBlockState());
        }
    }

    @Override
    protected void entityInside(
            BlockState state,
            Level level,
            BlockPos pos,
            Entity entity,
            InsideBlockEffectApplier applier,
            boolean isPrecise) {
        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            entity.hurtServer(serverLevel, entity.damageSources().lava(), 4.0F);
            entity.igniteForSeconds(30);
        }
    }
}
