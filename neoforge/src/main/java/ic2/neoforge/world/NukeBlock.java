package ic2.neoforge.world;

import ic2.neoforge.machine.NukeBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;

import org.jspecify.annotations.Nullable;

/**
 * The nuke charge block (legacy Ic2TileEntityBlock around TileEntityNuke): an instant-break
 * inventory charge. Redstone, fire charges, flint and steel, burning arrows and neighbouring
 * explosions prime it; a plain hand break just uninstalls it. The loaded payload travels to the
 * primed {@link ic2.neoforge.entity.NukeEntity}.
 */
public class NukeBlock extends Block implements EntityBlock {
    public NukeBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NukeBlockEntity(pos, state);
    }

    @Nullable
    private static NukeBlockEntity charge(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof NukeBlockEntity nuke ? nuke : null;
    }

    private static void primeIfLoaded(Level level, BlockPos pos) {
        NukeBlockEntity nuke = charge(level, pos);
        if (nuke != null) nuke.explode(null, false);
    }

    @Override
    protected void onPlace(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState oldState,
            boolean movedByPiston) {
        if (!oldState.is(state.getBlock())
                && level.hasNeighborSignal(pos)
                && level instanceof ServerLevel) {
            primeIfLoaded(level, pos);
        }
    }

    @Override
    protected void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block block,
            @Nullable Orientation orientation,
            boolean movedByPiston) {
        if (level.hasNeighborSignal(pos) && level instanceof ServerLevel) {
            primeIfLoaded(level, pos);
        }
    }

    @Override
    public void onBlockExploded(
            BlockState state, ServerLevel level, BlockPos pos, Explosion explosion) {
        // Legacy TileEntityExplosive.onExploded chains into a charge without dropping itself;
        // an unloaded charge was never primed and falls back to its loot drop.
        BlockEntity blockEntity = level.getBlockEntity(pos);
        // Legacy TileEntityExplosive.explode primes first and removes the block itself; a
        // refused priming (no payload, nukes disabled) falls back to uninstall-and-drop.
        if (!(blockEntity instanceof NukeBlockEntity nuke && nuke.onExploded(explosion))) {
            level.removeBlock(pos, false);
            Block.dropResources(state, level, pos, null, null, ItemStack.EMPTY);
        }
    }

    @Override
    public boolean dropFromExplosion(Explosion explosion) {
        return false;
    }

    @Override
    protected void onProjectileHit(
            Level level, BlockState state, BlockHitResult hit, Projectile projectile) {
        if (level instanceof ServerLevel && projectile.isOnFire()) {
            BlockPos pos = hit.getBlockPos();
            var owner = projectile.getOwner();
            NukeBlockEntity nuke = charge(level, pos);
            if (nuke != null) {
                nuke.explode(owner instanceof LivingEntity living ? living : null, false);
            }
        }
    }

    @Override
    protected InteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult) {
        if (!stack.is(Items.FLINT_AND_STEEL) && !stack.is(Items.FIRE_CHARGE)) {
            return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
        }
        NukeBlockEntity nuke = charge(level, pos);
        if (nuke == null || !nuke.explode(player, false)) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel) {
            if (stack.is(Items.FLINT_AND_STEEL)) {
                stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
            } else {
                stack.consume(1, player);
            }
            player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        NukeBlockEntity nuke = charge(level, pos);
        if (nuke != null && !level.isClientSide()) {
            player.openMenu(nuke, buffer -> buffer.writeBlockPos(pos));
        }
        return InteractionResult.SUCCESS;
    }
}
