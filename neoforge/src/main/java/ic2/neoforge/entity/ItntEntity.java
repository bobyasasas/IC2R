package ic2.neoforge.entity;

import ic2.neoforge.registration.ModEntities;
import ic2.neoforge.registration.ModExplosives;
import ic2.neoforge.world.Ic2Explosion;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jspecify.annotations.Nullable;

/**
 * Industrial TNT charge (legacy ExplosiveEntity/ITntEntity): a primed block-styled entity with a
 * sixty-tick fuse that detonates a 5.5-strength TNT-grade blast.
 */
public class ItntEntity extends Entity implements TraceableEntity {
    private static final EntityDataAccessor<Integer> DATA_FUSE =
            SynchedEntityData.defineId(ItntEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<BlockState> DATA_BLOCK_STATE =
            SynchedEntityData.defineId(ItntEntity.class, EntityDataSerializers.BLOCK_STATE);

    static final int DEFAULT_FUSE = 60;
    static final float DEFAULT_EXPLOSION_POWER = 5.5F;
    // Legacy ITntEntity blast profile: most blocks drop, entity damage stays deliberately low.
    static final float DROP_RATE = 0.9F;
    static final float DAMAGE_VS_ENTITIES = 0.3F;
    private static final String TAG_FUSE = "fuse";
    private static final String TAG_BLOCK_STATE = "block_state";
    private static final String TAG_EXPLOSION_POWER = "explosion_power";

    private @Nullable EntityReference<LivingEntity> owner;
    private float explosionPower = DEFAULT_EXPLOSION_POWER;

    public ItntEntity(EntityType<? extends ItntEntity> type, Level level) {
        super(type, level);
        this.blocksBuilding = true;
    }

    private ItntEntity(Level level, double x, double y, double z, @Nullable LivingEntity owner) {
        this(ModEntities.ITNT.get(), level);
        this.setPos(x, y, z);
        double angle = level.getRandom().nextDouble() * (Math.PI * 2);
        this.setDeltaMovement(-Math.sin(angle) * 0.02, 0.2F, -Math.cos(angle) * 0.02);
        this.xo = x;
        this.yo = y;
        this.zo = z;
        this.setFuse(DEFAULT_FUSE);
        this.setBlockState(ModExplosives.ITNT.get().defaultBlockState());
        this.owner = owner == null ? null : EntityReference.of(owner);
    }

    /** Primes a charge at the given position, mirroring the vanilla TNT priming flow. */
    public static ItntEntity prime(
            Level level, double x, double y, double z, @Nullable LivingEntity owner) {
        return new ItntEntity(level, x, y, z, owner);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder data) {
        data.define(DATA_FUSE, DEFAULT_FUSE);
        data.define(DATA_BLOCK_STATE, ModExplosives.ITNT.get().defaultBlockState());
    }

    @Override
    protected Entity.MovementEmission getMovementEmission() {
        return Entity.MovementEmission.NONE;
    }

    @Override
    public boolean isPickable() {
        return !this.isRemoved();
    }

    @Override
    protected double getDefaultGravity() {
        return 0.04;
    }

    @Override
    public void tick() {
        this.handlePortal();
        this.applyGravity();
        this.move(MoverType.SELF, this.getDeltaMovement());
        this.applyEffectsFromBlocks();
        this.setDeltaMovement(this.getDeltaMovement().scale(0.98));
        if (this.onGround()) {
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.7, -0.5, 0.7));
        }

        int fuse = this.getFuse() - 1;
        this.setFuse(fuse);
        if (fuse <= 0) {
            this.discard();
            if (!this.level().isClientSide()) {
                this.explode();
            }
        } else {
            this.updateFluidInteraction();
            if (this.level().isClientSide()) {
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
        }
    }

    private void explode() {
        if (this.level() instanceof ServerLevel level
                && level.getGameRules().get(GameRules.TNT_EXPLODES)) {
            new Ic2Explosion(
                            level,
                            this,
                            this.getOwner(),
                            this.getX(),
                            this.getY(0.0625),
                            this.getZ(),
                            this.explosionPower,
                            DROP_RATE,
                            Ic2Explosion.Type.Normal,
                            0)
                    .doExplosion();
        }
    }

    @Override
    public @Nullable Entity teleport(TeleportTransition transition) {
        Entity teleported = super.teleport(transition);
        if (teleported instanceof ItntEntity charge && this.getOwner() != null) {
            charge.owner = EntityReference.of(this.getOwner());
        }
        return teleported;
    }

    @Override
    public final boolean hurtServer(
            ServerLevel level, net.minecraft.world.damagesource.DamageSource source, float damage) {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putShort(TAG_FUSE, (short) this.getFuse());
        output.store(TAG_BLOCK_STATE, BlockState.CODEC, this.getBlockState());
        if (this.explosionPower != DEFAULT_EXPLOSION_POWER) {
            output.putFloat(TAG_EXPLOSION_POWER, this.explosionPower);
        }
        EntityReference.store(this.owner, output, "owner");
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.setFuse(input.getShortOr(TAG_FUSE, (short) DEFAULT_FUSE));
        this.setBlockState(
                input.read(TAG_BLOCK_STATE, BlockState.CODEC)
                        .orElse(ModExplosives.ITNT.get().defaultBlockState()));
        this.explosionPower =
                net.minecraft.util.Mth.clamp(
                        input.getFloatOr(TAG_EXPLOSION_POWER, DEFAULT_EXPLOSION_POWER),
                        0.0F,
                        128.0F);
        this.owner = EntityReference.read(input, "owner");
    }

    @Override
    public void restoreFrom(Entity oldEntity) {
        super.restoreFrom(oldEntity);
        if (oldEntity instanceof ItntEntity charge) {
            this.owner = charge.owner;
        }
    }

    public @Nullable LivingEntity getOwner() {
        return EntityReference.getLivingEntity(this.owner, this.level());
    }

    public int getFuse() {
        return this.entityData.get(DATA_FUSE);
    }

    public void setFuse(int fuse) {
        this.entityData.set(DATA_FUSE, fuse);
    }

    public void setBlockState(BlockState state) {
        this.entityData.set(DATA_BLOCK_STATE, state);
    }

    public BlockState getBlockState() {
        return this.entityData.get(DATA_BLOCK_STATE);
    }
}
