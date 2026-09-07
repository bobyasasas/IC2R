package ic2.neoforge.world;

import com.mojang.serialization.MapCodec;

import ic2.neoforge.registration.ModWorldContent;

import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;

public final class RubberWoodBlock extends Block {
    public static final MapCodec<RubberWoodBlock> CODEC = simpleCodec(RubberWoodBlock::new);

    public RubberWoodBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    public BlockState getToolModifiedState(
            BlockState state, UseOnContext context, ItemAbility ability, boolean simulate) {
        return ability == ItemAbilities.AXE_STRIP
                        && context.getItemInHand().canPerformAction(ability)
                ? ModWorldContent.STRIPPED_RUBBER_WOOD.get().defaultBlockState()
                : super.getToolModifiedState(state, context, ability, simulate);
    }
}
