package ic2.neoforge.entity;

import ic2.neoforge.registration.ModEntities;
import ic2.neoforge.registration.ModExplosives;
import ic2.neoforge.world.PointExplosion;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;

/**
 * Thrown dynamite (legacy DynamiteEntity/StickyDynamiteEntity): a hundred-tick fuse thrown from the
 * player's hand, bouncing off blocks with its own ray-marched physics. Sticky charges anchor to the
 * first block they hit, burn their fuse three ticks faster per tick while anchored and fall off
 * once their support disappears. Submerging in water adds two thousand ticks to the fuse, so a
 * dynamite thrown into water effectively becomes a dud; a charge stuck in place for two hundred
 * ticks is discarded the same way. The blast is the fixed {@link PointExplosion} cube, not a
 * vanilla explosion.
 */
public class DynamiteEntity extends Entity implements TraceableEntity, ItemSupplier {
    public static final int DEFAULT_FUSE = 100;
    private static final int MAX_GROUND_TICKS = 200;
    private static final float WATER_DRAG = 0.75F;
    private static final float DRAG = 0.98F;
    private static final float GRAVITY = 0.04F;
    private static final EntityDataAccessor<Integer> DATA_FUSE =
            SynchedEntityData.defineId(DynamiteEntity.class, EntityDataSerializers.INT);

    private static final String TAG_IN_GROUND = "in_ground";
    private static final String TAG_FUSE = "fuse";
    private static final String TAG_STICK_POS = "stick_pos";

    private boolean inGround;
    private int ticksInGround;
    private @Nullable BlockPos stickPos;
    private @Nullable EntityReference<LivingEntity> owner;

    public DynamiteEntity(EntityType<? extends DynamiteEntity> type, Level level) {
        super(type, level);
    }

    /** Throws a charge from the owner's eye height along their look vector, legacy style. */
    public static DynamiteEntity thrown(Level level, LivingEntity owner, boolean sticky) {
        DynamiteEntity entity = create(level, sticky);
        entity.owner = EntityReference.of(owner);
        Vec3 eye = owner.getEyePosition();
        entity.setPos(
                eye.x - Math.cos(Math.toRadians(owner.getYRot())) * 0.16,
                eye.y - 0.1,
                eye.z - Math.sin(Math.toRadians(owner.getYRot())) * 0.16);
        entity.setYRot(owner.getYRot());
        entity.setXRot(owner.getXRot());
        double motionX =
                -Math.sin(Math.toRadians(entity.getYRot()))
                        * Math.cos(Math.toRadians(entity.getXRot()));
        double motionZ =
                Math.cos(Math.toRadians(entity.getYRot()))
                        * Math.cos(Math.toRadians(entity.getXRot()));
        double motionY = -Math.sin(Math.toRadians(entity.getXRot()));
        entity.shoot(motionX, motionY, motionZ, 1.0F, 1.0F);
        return entity;
    }

    /** Spawns a charge at a fixed position, used by block priming and dispensers. */
    public static DynamiteEntity at(Level level, double x, double y, double z, boolean sticky) {
        DynamiteEntity entity = create(level, sticky);
        entity.setPos(x, y, z);
        return entity;
    }

    private static DynamiteEntity create(Level level, boolean sticky) {
        EntityType<DynamiteEntity> type =
                sticky ? ModEntities.STICKY_DYNAMITE.get() : ModEntities.DYNAMITE.get();
        return new DynamiteEntity(type, level);
    }

    public boolean isSticky() {
        return this.getType() == ModEntities.STICKY_DYNAMITE.get();
    }

    public int getFuse() {
        return this.entityData.get(DATA_FUSE);
    }

    public void setFuse(int fuse) {
        this.entityData.set(DATA_FUSE, fuse);
    }

    public void setOwner(@Nullable LivingEntity owner) {
        this.owner = owner == null ? null : EntityReference.of(owner);
    }

    @Override
    public @Nullable LivingEntity getOwner() {
        return EntityReference.getLivingEntity(this.owner, this.level());
    }

    /** Legacy shoot(): normalises, spreads and scales the motion, then faces it. */
    public void shoot(double x, double y, double z, float velocity, float inaccuracy) {
        double length = Math.sqrt(x * x + y * y + z * z);
        x /= length;
        y /= length;
        z /= length;
        x += this.random.nextGaussian() * 0.0075 * inaccuracy;
        y += this.random.nextGaussian() * 0.0075 * inaccuracy;
        z += this.random.nextGaussian() * 0.0075 * inaccuracy;
        x *= velocity;
        y *= velocity;
        z *= velocity;
        this.setDeltaMovement(x, y, z);
        double horizontal = Math.sqrt(x * x + z * z);
        this.setYRot((float) (Math.atan2(x, z) * 180.0 / Math.PI));
        this.setXRot((float) (Math.atan2(y, horizontal) * 180.0 / Math.PI));
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
        this.ticksInGround = 0;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder data) {
        data.define(DATA_FUSE, DEFAULT_FUSE);
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 motion = this.getDeltaMovement();
        if (this.xRotO == 0.0F && this.yRotO == 0.0F) {
            double horizontal = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
            this.setYRot((float) Math.toDegrees(Math.atan2(motion.x, motion.z)));
            this.setXRot((float) Math.toDegrees(Math.atan2(motion.y, horizontal)));
            this.yRotO = this.getYRot();
            this.xRotO = this.getXRot();
        }

        int fuse = this.getFuse() - 1;
        this.setFuse(fuse);
        if (fuse <= 0) {
            this.discard();
            if (!this.level().isClientSide()) {
                this.explode();
            }
            return;
        }

        if (fuse < DEFAULT_FUSE && fuse % 2 == 0 && this.level().isClientSide()) {
            this.level()
                    .addParticle(
                            ParticleTypes.SMOKE,
                            this.getX(),
                            this.getY() + 0.5,
                            this.getZ(),
                            0.0,
                            0.0,
                            0.0);
        }

        if (this.inGround) {
            this.ticksInGround++;
            if (this.ticksInGround >= MAX_GROUND_TICKS) {
                // Legacy behaviour: a charge stuck in place silently fizzles out.
                this.discard();
                return;
            }

            if (this.isSticky()) {
                this.setFuse(fuse - 3);
                this.setDeltaMovement(Vec3.ZERO);
                if (this.stickPos != null && !this.level().isEmptyBlock(this.stickPos)) {
                    return;
                }
            }
        }

        this.advance(motion);
    }

    /** Legacy per-tick movement: clip for blocks, bounce, rotate, then apply drag and gravity. */
    private void advance(Vec3 motion) {
        Vec3 start = this.position();
        Vec3 end = start.add(motion);
        BlockHitResult hit =
                this.level()
                        .clip(
                                new ClipContext(
                                        start,
                                        end,
                                        ClipContext.Block.COLLIDER,
                                        ClipContext.Fluid.NONE,
                                        this));
        if (hit.getType() != HitResult.Type.MISS) {
            end = hit.getLocation();
            float remainX = (float) (end.x - this.getX());
            float remainY = (float) (end.y - this.getY());
            float remainZ = (float) (end.z - this.getZ());
            double remaining =
                    Math.sqrt(
                            remainX * (double) remainX
                                    + remainY * (double) remainY
                                    + remainZ * (double) remainZ);
            this.stickPos = hit.getBlockPos();
            if (remaining > 0.0) {
                this.setPos(
                        this.getX() + remainX - remainX / remaining * 0.05,
                        this.getY() + remainY - remainY / remaining * 0.05,
                        this.getZ() + remainZ - remainZ / remaining * 0.05);
            }

            this.setDeltaMovement(
                    motion.x * (0.75F - this.random.nextFloat()),
                    motion.y * -0.3F,
                    motion.z * (0.75F - this.random.nextFloat()));
            this.inGround = true;
        } else {
            this.setPos(this.getX() + motion.x, this.getY() + motion.y, this.getZ() + motion.z);
            this.inGround = false;
        }

        this.faceMotion();
        float drag = DRAG;
        if (this.isInWater()) {
            // Legacy quirk: water saturates the fuse instead of extinguishing it, a dud either way.
            this.setFuse(this.getFuse() + 2000);
            for (int i = 0; i < 4; i++) {
                float offset = 0.25F;
                this.level()
                        .addParticle(
                                ParticleTypes.BUBBLE,
                                this.getX() - motion.x * offset,
                                this.getY() - motion.y * offset,
                                this.getZ() - motion.z * offset,
                                motion.x,
                                motion.y,
                                motion.z);
            }

            drag = WATER_DRAG;
        }

        Vec3 moved = this.getDeltaMovement();
        this.setDeltaMovement(moved.x * drag, moved.y * drag - GRAVITY, moved.z * drag);
    }

    private void faceMotion() {
        Vec3 motion = this.getDeltaMovement();
        double horizontal = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        this.setYRot((float) Math.toDegrees(Math.atan2(motion.x, motion.z)));
        this.setXRot((float) Math.toDegrees(Math.atan2(motion.y, horizontal)));
        while (this.getXRot() - this.xRotO < -180.0F) {
            this.xRotO -= 360.0F;
        }
        while (this.getXRot() - this.xRotO >= 180.0F) {
            this.xRotO += 360.0F;
        }
        while (this.getYRot() - this.yRotO < -180.0F) {
            this.yRotO -= 360.0F;
        }
        while (this.getYRot() - this.yRotO >= 180.0F) {
            this.yRotO += 360.0F;
        }
        this.setXRot(this.xRotO + (this.getXRot() - this.xRotO) * 0.2F);
        this.setYRot(this.yRotO + (this.getYRot() - this.yRotO) * 0.2F);
    }

    private void explode() {
        if (this.level() instanceof ServerLevel level
                && level.getGameRules().get(GameRules.TNT_EXPLODES)) {
            new PointExplosion(
                            level,
                            this,
                            this.getOwner(),
                            this.getX(),
                            this.getY(),
                            this.getZ(),
                            1.0F,
                            20)
                    .doExplosion();
        }
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(
                this.isSticky()
                        ? ModExplosives.DYNAMITE_STICKY.get()
                        : ModExplosives.DYNAMITE.get());
    }

    @Override
    public final boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putBoolean(TAG_IN_GROUND, this.inGround);
        output.putInt(TAG_FUSE, this.getFuse());
        if (this.stickPos != null) {
            output.store(TAG_STICK_POS, BlockPos.CODEC, this.stickPos);
        }
        EntityReference.store(this.owner, output, "owner");
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.inGround = input.getBooleanOr(TAG_IN_GROUND, false);
        this.setFuse(input.getIntOr(TAG_FUSE, DEFAULT_FUSE));
        this.stickPos = input.read(TAG_STICK_POS, BlockPos.CODEC).orElse(null);
        this.owner = EntityReference.read(input, "owner");
    }

    @Override
    public void restoreFrom(Entity oldEntity) {
        super.restoreFrom(oldEntity);
        if (oldEntity instanceof DynamiteEntity dynamite) {
            this.inGround = dynamite.inGround;
            this.stickPos = dynamite.stickPos;
            this.owner = dynamite.owner;
        }
    }

    @Override
    public @Nullable Entity teleport(TeleportTransition transition) {
        Entity teleported = super.teleport(transition);
        if (teleported instanceof DynamiteEntity dynamite && this.getOwner() != null) {
            dynamite.owner = EntityReference.of(this.getOwner());
        }
        return teleported;
    }
}
