package ic2.neoforge.world;

import ic2.neoforge.entity.ItntEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;

import org.jspecify.annotations.Nullable;

/**
 * Industrial TNT (legacy TileEntityExplosive/TileEntityITnt): a tnt-grade charge with a shorter
 * sixty-tick fuse and a 5.5-strength blast. Like the legacy explosive block it primes on redstone,
 * fire, any player removal (explodeOnRemoval, creative included) and neighbouring explosions
 * (short-fuse chain reaction).
 */
public class ItntBlock extends Block {
    public ItntBlock(Properties properties) {
        super(properties);
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
                && prime(level, pos, null)) {
            level.removeBlock(pos, false);
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
        if (level.hasNeighborSignal(pos) && prime(level, pos, null)) {
            level.removeBlock(pos, false);
        }
    }

    @Override
    public BlockState playerWillDestroy(
            Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && prime(level, pos, player)) {
            level.removeBlock(pos, false);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void wasExploded(ServerLevel level, BlockPos pos, Explosion explosion) {
        if (level.getGameRules().get(GameRules.TNT_EXPLODES)) {
            ItntEntity charge =
                    ItntEntity.prime(
                            level,
                            pos.getX() + 0.5,
                            pos.getY(),
                            pos.getZ() + 0.5,
                            explosion.getIndirectSourceEntity() instanceof LivingEntity living
                                    ? living
                                    : null);
            level.addFreshEntity(charge);
            int fuse = charge.getFuse();
            // Legacy shortFuse(): neighbouring explosions set a fraction of the normal fuse.
            charge.setFuse(level.getRandom().nextInt(fuse / 4) + fuse / 8);
        }
    }

    public static boolean prime(Level level, BlockPos pos, @Nullable LivingEntity source) {
        if (level instanceof ServerLevel serverLevel
                && serverLevel.getGameRules().get(GameRules.TNT_EXPLODES)) {
            ItntEntity charge =
                    ItntEntity.prime(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, source);
            serverLevel.addFreshEntity(charge);
            serverLevel.playSound(
                    null,
                    charge.getX(),
                    charge.getY(),
                    charge.getZ(),
                    SoundEvents.TNT_PRIMED,
                    SoundSource.BLOCKS,
                    1.0F,
                    1.0F);
            serverLevel.gameEvent(source, GameEvent.PRIME_FUSE, pos);
            return true;
        }
        return false;
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
        if (prime(level, pos, player)) {
            level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 11);
            Item item = stack.getItem();
            if (stack.is(Items.FLINT_AND_STEEL)) {
                stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
            } else {
                stack.consume(1, player);
            }
            player.awardStat(Stats.ITEM_USED.get(item));
        } else if (level instanceof ServerLevel serverLevel
                && !serverLevel.getGameRules().get(GameRules.TNT_EXPLODES)) {
            player.sendOverlayMessage(Component.translatable("block.minecraft.tnt.disabled"));
            return InteractionResult.PASS;
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void onProjectileHit(
            Level level, BlockState state, BlockHitResult hit, Projectile projectile) {
        if (level instanceof ServerLevel serverLevel) {
            BlockPos pos = hit.getBlockPos();
            var owner = projectile.getOwner();
            if (projectile.isOnFire()
                    && projectile.mayInteract(serverLevel, pos)
                    && prime(level, pos, owner instanceof LivingEntity living ? living : null)) {
                level.removeBlock(pos, false);
            }
        }
    }

    @Override
    public boolean dropFromExplosion(Explosion explosion) {
        return false;
    }
}
