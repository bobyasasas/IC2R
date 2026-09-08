package ic2.neoforge.explosion;

import ic2.core.explosion.HeatBlast;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ExplosionParticleInfo;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;

/** Thermal tracing with native cancellation, block hooks, damage and client synchronization. */
public final class HeatExplosion extends ServerExplosion {
    private static final WeightedList<ExplosionParticleInfo> PARTICLES =
            WeightedList.<ExplosionParticleInfo>builder()
                    .add(new ExplosionParticleInfo(ParticleTypes.POOF, .5f, 1))
                    .add(new ExplosionParticleInfo(ParticleTypes.SMOKE, 1, 1))
                    .build();
    private final BlockPos origin;
    private final int power;
    private final float dropChance;
    private final boolean removeOrigin;

    private HeatExplosion(
            ServerLevel level, BlockPos origin, int power, float dropChance, boolean removeOrigin) {
        super(
                level,
                null,
                null,
                null,
                Vec3.atCenterOf(origin),
                power,
                false,
                BlockInteraction.DESTROY);
        if (power < 1
                || power > 10
                || !Float.isFinite(dropChance)
                || dropChance < 0
                || dropChance > 1) throw new IllegalArgumentException("Invalid thermal explosion");
        this.origin = origin.immutable();
        this.power = power;
        this.dropChance = dropChance;
        this.removeOrigin = removeOrigin;
    }

    /** Returns false when a protection listener cancels the burst; origin removal is included. */
    public static boolean trigger(
            ServerLevel level, BlockPos origin, int power, float dropChance, boolean removeOrigin) {
        var explosion = new HeatExplosion(level, origin, power, dropChance, removeOrigin);
        if (EventHooks.onExplosionStart(level, explosion)) return false;
        int blocks = explosion.explode();
        for (var player : level.players())
            if (player.distanceToSqr(explosion.center()) < 4096)
                player.connection.send(
                        new ClientboundExplodePacket(
                                explosion.center(),
                                power,
                                blocks,
                                Optional.ofNullable(explosion.getHitPlayers().get(player)),
                                power < 2
                                        ? ParticleTypes.EXPLOSION
                                        : ParticleTypes.EXPLOSION_EMITTER,
                                SoundEvents.GENERIC_EXPLODE,
                                PARTICLES));
        return true;
    }

    @Override
    public int explode() {
        var trace = HeatBlast.trace(power, center().x, center().y, center().z, this::cell);
        var blocks = new ArrayList<BlockPos>();
        var noDrops = new HashSet<BlockPos>();
        trace.blocks()
                .forEach(
                        (pos, vaporized) -> {
                            var block = new BlockPos(pos.x(), pos.y(), pos.z());
                            blocks.add(block);
                            if (vaporized) noDrops.add(block);
                        });
        if (removeOrigin) {
            blocks.add(origin);
            noDrops.add(origin);
        }
        var entities =
                new ArrayList<>(
                        level().getEntities(
                                        (Entity) null,
                                        new AABB(center(), center()).inflate(power / .4),
                                        entity ->
                                                entity instanceof LivingEntity
                                                        || entity instanceof ItemEntity));
        EventHooks.onExplosionDetonate(level(), this, entities, blocks);
        level().gameEvent(null, GameEvent.EXPLODE, center());
        for (var entity : entities) affectEntity(entity, trace, blocks);
        boolean drops = level().getGameRules().get(GameRules.BLOCK_DROPS);
        for (var pos : new HashSet<>(blocks)) {
            if (!loaded(pos)) continue;
            level().getBlockState(pos)
                    .onExplosionHit(
                            level(),
                            pos,
                            this,
                            (stack, position) -> {
                                if (drops
                                        && !noDrops.contains(pos)
                                        && level().getRandom().nextFloat() < dropChance)
                                    Block.popResource(level(), position, stack);
                            });
        }
        return blocks.size();
    }

    private void affectEntity(Entity entity, HeatBlast.Result trace, ArrayList<BlockPos> blocks) {
        if (entity.ignoreExplosion(this) || entity.level() != level()) return;
        double power = 0, distanceSquared = entity.distanceToSqr(center());
        for (var sample : trace.samples()) {
            double low = Math.max(0, sample.step() - 5), high = sample.step() + 5;
            if (distanceSquared < low * low || distanceSquared >= high * high) continue;
            if (entity.distanceToSqr(sample.x(), sample.y(), sample.z()) <= 25)
                power += sample.power();
        }
        if (power <= 0) return;
        entity.hurtServer(level(), getDamageSource(), (float) (4 * power));
        double resistance =
                entity instanceof LivingEntity living
                        ? living.getAttributeValue(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE)
                        : 0;
        var impulse =
                entity.position()
                        .subtract(center())
                        .normalize()
                        .scale(Math.min(60, power * .0875) * (1 - resistance));
        impulse = EventHooks.getExplosionKnockback(level(), this, entity, impulse, blocks);
        entity.push(impulse);
        if (entity instanceof Player player
                && !player.isSpectator()
                && (!player.isCreative() || !player.getAbilities().flying))
            getHitPlayers().put(player, impulse);
        entity.onExplosionHit(null);
    }

    private HeatBlast.Cell cell(HeatBlast.Position position) {
        var pos = new BlockPos(position.x(), position.y(), position.z());
        if (!loaded(pos)) return HeatBlast.Cell.BARRIER;
        if (removeOrigin && pos.equals(origin)) return HeatBlast.Cell.AIR;
        var state = level().getBlockState(pos);
        if (state.isAir()) return HeatBlast.Cell.AIR;
        if (state.is(Blocks.WATER)) return HeatBlast.Cell.WATER;
        return HeatBlast.Cell.solid(state.getExplosionResistance(level(), pos, this));
    }

    private boolean loaded(BlockPos pos) {
        return level().isInWorldBounds(pos)
                && level().getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4);
    }
}
