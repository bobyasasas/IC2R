package ic2.neoforge.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;

/** Legacy AirBlock: an intentionally inert fluid block, kept as a sibling of the other
 *  special fluid blocks so the family dispatch mirrors the legacy factory one to one. */
public class AirFluidBlock extends LiquidBlock {
    public AirFluidBlock(FlowingFluid fluid, Properties properties) {
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
    }
}
