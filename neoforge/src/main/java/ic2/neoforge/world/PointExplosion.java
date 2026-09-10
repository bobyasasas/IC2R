package ic2.neoforge.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;

/**
 * Legacy PointExplosion: a fixed three-by-three-by-three blast around the epicentre that destroys
 * every block whose explosion resistance is below ten times the power, drops all of them, hurts
 * entities within two blocks for a flat amount and plays the vanilla explosion feedback. Unlike a
 * vanilla ray-marched explosion the radius does not decay, so a power of one still clears the whole
 * cube — thrown dynamite relies on that.
 */
public final class PointExplosion implements Explosion {
    private static final int ENTITY_DAMAGE_RADIUS = 2;

    private final ServerLevel level;
    private final @Nullable Entity source;
    private final @Nullable LivingEntity igniter;
    private final float power;
    private final int entityDamage;
    private final Vec3 center;

    public PointExplosion(
            ServerLevel level,
            @Nullable Entity source,
            @Nullable LivingEntity igniter,
            double x,
            double y,
            double z,
            float power,
            int entityDamage) {
        this.level = level;
        this.source = source;
        this.igniter = igniter;
        this.power = power;
        this.entityDamage = entityDamage;
        this.center = new Vec3(x, y, z);
    }

    public void doExplosion() {
        this.destroyBlocks();
        this.hurtEntities();
        this.level.playSound(
                null,
                this.center.x,
                this.center.y,
                this.center.z,
                SoundEvents.GENERIC_EXPLODE,
                SoundSource.BLOCKS,
                4.0F,
                (1.0F
                                + (this.level.getRandom().nextFloat()
                                                - this.level.getRandom().nextFloat())
                                        * 0.2F)
                        * 0.7F);
        this.level.sendParticles(
                ParticleTypes.EXPLOSION,
                this.center.x,
                this.center.y,
                this.center.z,
                1,
                0.0,
                0.0,
                0.0,
                0.0);
    }

    private void destroyBlocks() {
        int cx = BlockPos.containing(this.center).getX();
        int cy = BlockPos.containing(this.center).getY();
        int cz = BlockPos.containing(this.center).getZ();
        for (int x = cx - 1; x <= cx + 1; x++) {
            for (int y = cy - 1; y <= cy + 1; y++) {
                for (int z = cz - 1; z <= cz + 1; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = this.level.getBlockState(pos);
                    if (state.isAir()
                            || state.getBlock().getExplosionResistance() >= this.power * 10.0F) {
                        continue;
                    }
                    state.onExplosionHit(
                            this.level,
                            pos,
                            this,
                            (stack, hitPos) -> Block.popResource(this.level, hitPos, stack));
                }
            }
        }
    }

    private void hurtEntities() {
        DamageSource damageSource = this.level.damageSources().explosion(this);
        AABB box =
                new AABB(
                        this.center.x - ENTITY_DAMAGE_RADIUS,
                        this.center.y - ENTITY_DAMAGE_RADIUS,
                        this.center.z - ENTITY_DAMAGE_RADIUS,
                        this.center.x + ENTITY_DAMAGE_RADIUS,
                        this.center.y + ENTITY_DAMAGE_RADIUS,
                        this.center.z + ENTITY_DAMAGE_RADIUS);
        for (Entity target :
                this.level.getEntities(
                        this.source, box, target -> target != this.source && !target.isRemoved())) {
            target.hurtServer(this.level, damageSource, this.entityDamage);
        }
    }

    @Override
    public ServerLevel level() {
        return this.level;
    }

    @Override
    public BlockInteraction getBlockInteraction() {
        return BlockInteraction.DESTROY;
    }

    @Override
    public @Nullable LivingEntity getIndirectSourceEntity() {
        return this.igniter;
    }

    @Override
    public @Nullable Entity getDirectSourceEntity() {
        return this.source;
    }

    @Override
    public float radius() {
        return this.power;
    }

    @Override
    public Vec3 center() {
        return this.center;
    }

    @Override
    public boolean canTriggerBlocks() {
        return false;
    }

    @Override
    public boolean shouldAffectBlocklikeEntities() {
        return true;
    }
}
