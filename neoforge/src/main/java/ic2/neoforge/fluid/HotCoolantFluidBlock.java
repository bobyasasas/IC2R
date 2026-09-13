package ic2.neoforge.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;

/** Legacy HotCoolantBlock: hot coolant sets whatever wades into it on fire. */
public class HotCoolantFluidBlock extends LiquidBlock {
    public HotCoolantFluidBlock(FlowingFluid fluid, Properties properties) {
        super(fluid, properties);
    }

    @Override
    protected void entityInside(
            BlockState state,
            Level level,
            BlockPos pos,
            Entity entity,
            net.minecraft.world.entity.InsideBlockEffectApplier applier,
            boolean isPrecise) {
        if (!level.isClientSide()) {
            entity.igniteForSeconds(30);
        }
    }
}
