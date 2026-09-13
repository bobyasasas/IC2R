package ic2.neoforge.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

/** Legacy UUMatterBlock: UU matter regenerates, bottles into water and annihilates foreign fluids. */
public class UUMatterFluidBlock extends LiquidBlock {
    private final FlowingFluid fluid;

    public UUMatterFluidBlock(FlowingFluid fluid, Properties properties) {
        super(fluid, properties);
        this.fluid = fluid;
    }

    @Override
    protected void entityInside(
            BlockState state,
            Level level,
            BlockPos pos,
            Entity entity,
            InsideBlockEffectApplier applier,
            boolean isPrecise) {
        if (!level.isClientSide() && entity instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1));
        }
    }

    @Override
    public InteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult) {
        if (stack.is(Items.GLASS_BOTTLE) && state.getValue(LiquidBlock.LEVEL) == 0) {
            if (!level.isClientSide()) {
                ItemStack waterBottle = PotionContents.createItemStack(Items.POTION, Potions.WATER);
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                if (stack.isEmpty()) {
                    player.setItemInHand(hand, waterBottle);
                } else if (!player.getInventory().add(waterBottle)) {
                    player.drop(waterBottle, false);
                }
            }
            level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
            return InteractionResult.SUCCESS;
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    @Override
    public void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block neighborBlock,
            @Nullable Orientation orientation,
            boolean movedByPiston) {
        if (state.getValue(LiquidBlock.LEVEL) != 0) return;

        // 26.1 drops the legacy per-neighbor position argument, so react to whichever
        // face neighbours a foreign fluid, mirroring the single-neighbor check.
        for (Direction side : Direction.values()) {
            BlockPos neighborPos = pos.relative(side);
            var neighborFluidState = level.getFluidState(neighborPos);
            if (neighborFluidState.isEmpty() || neighborFluidState.getType() == this.fluid) continue;

            if (neighborFluidState.is(FluidTags.LAVA)) {
                if (neighborFluidState.isSource()) {
                    level.setBlockAndUpdate(neighborPos, Blocks.OBSIDIAN.defaultBlockState());
                } else {
                    level.setBlockAndUpdate(neighborPos, Blocks.COBBLESTONE.defaultBlockState());
                }
            } else if (neighborFluidState.isSource()) {
                level.setBlock(neighborPos, Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }
}
