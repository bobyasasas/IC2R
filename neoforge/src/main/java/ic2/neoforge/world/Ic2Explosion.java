package ic2.neoforge.world;

import ic2.neoforge.registration.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Legacy Ic2Explosion: a ray marched explosion engine. Rays are shot from the epicentre in a dense
 * sphere grid; every non-air block absorbs resistance based power and is destroyed (dropping only
 * when every destroying ray carried at most eight power), ultra resistant blocks above one thousand
 * absorption are passed through without being destroyed, and entities accumulate ray based damage
 * with capped knockback. Drops are rolled with the explosion drop rate and merged per two-by-two
 * column like the legacy implementation.
 */
public class Ic2Explosion implements Explosion {
    public enum Type {
        Normal,
        Heat,
        Electrical,
        Nuclear,
        ReactorMeltdown
    }

    private static final ResourceKey<DamageType> NUKE_TYPE =
            ResourceKey.create(
                    Registries.DAMAGE_TYPE, Identifier.fromNamespaceAndPath("ic2", "nuke"));
    private static final ResourceKey<DamageType> REACTOR_EXPLOSION_TYPE =
            ResourceKey.create(
                    Registries.DAMAGE_TYPE,
                    Identifier.fromNamespaceAndPath("ic2", "reactor_explosion"));

    /** Ray absorption cost of air and of every step past an ultra resistant block. */
    private static final double AIR_ABSORPTION = 0.5;

    /** Blocks whose absorption exceeds this are passed through instead of destroyed. */
    private static final double ULTRA_RESISTANT = 1000.0;

    /** Rays carrying more than this power destroy without drops. */
    private static final double NO_DROP_POWER = 8.0;

    /** Damage dealt per point of remaining ray power to entities near the ray. */
    private static final double DAMAGE_PER_POWER = 4.0;

    /** Knockback impulse per point of ray power along the epicentre direction. */
    private static final double MOTION_PER_POWER = 0.0875;

    /** Squared speed above which the accumulated knockback gets clamped. */
    private static final double MAX_KNOCKBACK_SQUARED = 3600.0;

    /** Entities are hurt by a ray when they are within five blocks of the sampled point. */
    private static final double ENTITY_HIT_RANGE_SQUARED = 25.0;

    /** Item entities resist five points of accumulated damage before they are hurt. */
    private static final double ITEM_ENTITY_HEALTH = 5.0;

    private final ServerLevel level;
    private final @Nullable Entity source;
    private final @Nullable LivingEntity igniter;
    private final float power;
    private final float explosionDropRate;
    private final Type type;
    private final int radiationRange;
    private final DamageSource damageSource;
    private final Random rng = new Random();
    private final double maxDistance;
    private final Vec3 center;
    private final Map<BlockPos, Boolean> destroyedBlocks = new LinkedHashMap<>();
    private final List<EntityDamage> entitiesInRange = new ArrayList<>();

    public Ic2Explosion(
            ServerLevel level,
            @Nullable Entity source,
            @Nullable LivingEntity igniter,
            double x,
            double y,
            double z,
            float power,
            float dropRate,
            Type type,
            int radiationRange) {
        this.level = level;
        this.source = source;
        this.igniter = igniter;
        this.power = power;
        this.explosionDropRate = dropRate;
        this.type = type;
        this.radiationRange = radiationRange;
        this.center = new Vec3(x, y, z);
        this.maxDistance = power / 0.4;
        this.damageSource = this.createDamageSource();
    }

    private DamageSource createDamageSource() {
        if (this.type == Type.ReactorMeltdown) {
            return new DamageSource(this.damageTypeHolder(REACTOR_EXPLOSION_TYPE));
        }
        if (this.isNuclear()) {
            return this.igniter != null
                    ? new DamageSource(this.damageTypeHolder(NUKE_TYPE), this.igniter)
                    : new DamageSource(this.damageTypeHolder(NUKE_TYPE));
        }
        return this.level.damageSources().explosion(this);
    }

    private Holder<DamageType> damageTypeHolder(ResourceKey<DamageType> key) {
        return this.level.damageSources().damageTypes.getOrThrow(key);
    }

    private boolean isNuclear() {
        return this.type == Type.Nuclear || this.type == Type.ReactorMeltdown;
    }

    public void doExplosion() {
        if (this.power <= 0.0F) {
            return;
        }

        this.collectEntities();
        int steps = (int) Math.ceil(Math.PI / Math.atan(1.0 / this.maxDistance));
        for (int phiN = 0; phiN < 2 * steps; phiN++) {
            for (int thetaN = 0; thetaN < steps; thetaN++) {
                double phi = (Math.PI * 2) / steps * phiN;
                double theta = Math.PI / steps * thetaN;
                this.shootRay(
                        this.center.x,
                        this.center.y,
                        this.center.z,
                        phi,
                        theta,
                        this.power,
                        phiN % 8 == 0 && thetaN % 8 == 0);
            }
        }

        for (EntityDamage entry : this.entitiesInRange) {
            Entity entity = entry.entity;
            entity.hurtServer(this.level, this.damageSource, (float) entry.damage);
            double motionSquared =
                    entry.motionX * entry.motionX
                            + entry.motionY * entry.motionY
                            + entry.motionZ * entry.motionZ;
            double reduction =
                    motionSquared > MAX_KNOCKBACK_SQUARED
                            ? Math.sqrt(MAX_KNOCKBACK_SQUARED / motionSquared)
                            : 1.0;
            entity.setDeltaMovement(
                    entity.getDeltaMovement()
                            .add(
                                    entry.motionX * reduction,
                                    entry.motionY * reduction,
                                    entry.motionZ * reduction));
        }

        // The legacy radiation sickness loop over mobs without a hazmat suit needs the P16
        // radiation potion and armour items and lands with that package.

        this.playExplosionEffect();
        this.destroyAndCollectDrops();
    }

    private void collectEntities() {
        int range = (int) Math.ceil(this.maxDistance);
        BlockPos start =
                new BlockPos(
                        (int) this.center.x - range,
                        (int) this.center.y - range,
                        (int) this.center.z - range);
        BlockPos end =
                new BlockPos(
                        (int) this.center.x + range,
                        (int) this.center.y + range,
                        (int) this.center.z + range);
        for (Entity entity :
                this.level.getEntities(
                        this.source,
                        new AABB(
                                start.getX(),
                                start.getY(),
                                start.getZ(),
                                end.getX(),
                                end.getY(),
                                end.getZ()),
                        candidate ->
                                candidate instanceof LivingEntity
                                        || candidate instanceof ItemEntity)) {
            int distance =
                    (int)
                            (square(entity.getX() - this.center.x)
                                    + square(entity.getY() - this.center.y)
                                    + square(entity.getZ() - this.center.z));
            double health = entity instanceof ItemEntity ? ITEM_ENTITY_HEALTH : Double.MAX_VALUE;
            this.entitiesInRange.add(new EntityDamage(entity, distance, health));
        }
        this.entitiesInRange.sort(Comparator.comparingInt(entry -> entry.distance));
    }

    private void shootRay(
            double x,
            double y,
            double z,
            double phi,
            double theta,
            double power1,
            boolean killEntities) {
        double deltaX = Math.sin(theta) * Math.cos(phi);
        double deltaY = Math.cos(theta);
        double deltaZ = Math.sin(theta) * Math.sin(phi);
        int step = 0;

        while (true) {
            int blockY = (int) Math.floor(y);
            if (blockY < this.level.getMinY() || blockY >= this.level.getMaxY()) {
                break;
            }

            BlockPos pos = new BlockPos((int) Math.floor(x), blockY, (int) Math.floor(z));
            BlockState state = this.level.getBlockState(pos);
            Block block = state.getBlock();
            double absorption = this.getAbsorption(state, pos);
            if (absorption < 0.0) {
                break;
            }

            if (absorption > ULTRA_RESISTANT && !ExplosionWhitelist.isBlockWhitelisted(block)) {
                absorption = AIR_ABSORPTION;
            } else {
                if (absorption > power1) {
                    break;
                }
                if (!state.isAir()) {
                    this.destroyUnchecked(pos, power1 > NO_DROP_POWER);
                }
            }

            if (killEntities
                    && (step + 4) % 8 == 0
                    && !this.entitiesInRange.isEmpty()
                    && power1 >= 0.25) {
                this.damageEntities(x, y, z, step, power1);
            }

            if (absorption > 10.0) {
                for (int i = 0; i < 5; i++) {
                    this.shootRay(
                            x,
                            y,
                            z,
                            this.rng.nextDouble() * 2.0 * Math.PI,
                            this.rng.nextDouble() * Math.PI,
                            absorption * 0.4,
                            false);
                }
            }

            power1 -= absorption;
            x += deltaX;
            y += deltaY;
            z += deltaZ;
            step++;
        }
    }

    private double getAbsorption(BlockState state, BlockPos pos) {
        double ret = AIR_ABSORPTION;
        if (!state.isAir()) {
            if (state.is(Blocks.WATER) && this.type != Type.Normal) {
                ret++;
            } else {
                float resistance = state.getExplosionResistance(this.level, pos, this);
                if (resistance < 0.0F) {
                    return resistance;
                }
                double extra = (resistance + 4.0F) * 0.3;
                ret += this.type == Type.Heat ? extra * 6.0 : extra;
            }
        }
        return ret;
    }

    private void damageEntities(double x, double y, double z, int step, double power) {
        int index = this.getIndex(step);
        int distanceMax = square(step + 5);

        for (int i = index; i < this.entitiesInRange.size(); i++) {
            EntityDamage entry = this.entitiesInRange.get(i);
            if (entry.distance >= distanceMax) {
                break;
            }

            Entity entity = entry.entity;
            if (square(entity.getX() - x) + square(entity.getY() - y) + square(entity.getZ() - z)
                    <= ENTITY_HIT_RANGE_SQUARED) {
                double damage = DAMAGE_PER_POWER * power;
                entry.damage += damage;
                entry.health -= damage;
                double dx = entity.getX() - this.center.x;
                double dy = entity.getY() - this.center.y;
                double dz = entity.getZ() - this.center.z;
                double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
                entry.motionX += dx / distance * MOTION_PER_POWER * power;
                entry.motionY += dy / distance * MOTION_PER_POWER * power;
                entry.motionZ += dz / distance * MOTION_PER_POWER * power;
                if (entry.health <= 0.0) {
                    entity.hurtServer(this.level, this.damageSource, (float) entry.damage);
                    if (!entity.isAlive()) {
                        this.entitiesInRange.remove(i);
                        i--;
                    }
                }
            }
        }
    }

    /**
     * Binary searches the first tracked entity far enough from the epicentre that earlier ray steps
     * cannot have reached it yet, mirroring the legacy stepped damage window.
     */
    private int getIndex(int step) {
        int index;
        if (step != 4) {
            int distanceMin = square(step - 5);
            int indexStart = 0;
            int indexEnd = this.entitiesInRange.size() - 1;
            do {
                index = (indexStart + indexEnd) / 2;
                int distance = this.entitiesInRange.get(index).distance;
                if (distance < distanceMin) {
                    indexStart = index + 1;
                } else if (distance > distanceMin) {
                    indexEnd = index - 1;
                } else {
                    indexEnd = index;
                }
            } while (indexStart < indexEnd);
        } else {
            index = 0;
        }
        return index;
    }

    private void destroyUnchecked(BlockPos pos, boolean noDrop) {
        // Once any destroying ray carried more than eight power the block keeps no drops.
        this.destroyedBlocks.merge(pos, !noDrop, Boolean::logicalAnd);
    }

    private void playExplosionEffect() {
        SoundEvent sound;
        float volume;
        float pitch;
        switch (this.type) {
            case Heat -> {
                sound = SoundEvents.FIRE_EXTINGUISH;
                volume = 4.0F;
                pitch = (1.0F + (this.rng.nextFloat() - this.rng.nextFloat()) * 0.2F) * 0.7F;
            }
            case Electrical -> {
                sound = ModSounds.MACHINE_OVERLOAD.get();
                volume = 1.0F;
                pitch = 1.0F;
            }
            case Nuclear, ReactorMeltdown -> {
                sound = ModSounds.BLOCK_NUKE_EXPLODE.get();
                volume = 1.0F;
                pitch = 1.0F;
            }
            default -> {
                sound = SoundEvents.GENERIC_EXPLODE.value();
                volume = 4.0F;
                pitch = (1.0F + (this.rng.nextFloat() - this.rng.nextFloat()) * 0.2F) * 0.7F;
            }
        }
        this.level.playSound(
                null,
                this.center.x,
                this.center.y,
                this.center.z,
                sound,
                SoundSource.BLOCKS,
                volume,
                pitch);
        this.level.sendParticles(
                ParticleTypes.EXPLOSION_EMITTER,
                this.center.x,
                this.center.y,
                this.center.z,
                1,
                0.0,
                0.0,
                0.0,
                0.0);
    }

    private void destroyAndCollectDrops() {
        boolean doDrops = this.level.getGameRules().get(GameRules.BLOCK_DROPS);
        Map<DropColumn, Map<ItemStack, DropData>> blocksToDrop = new LinkedHashMap<>();

        // Bottom-up ordering keeps chained explosive reactions deterministic.
        List<BlockPos> ordered = new ArrayList<>(this.destroyedBlocks.keySet());
        ordered.sort(
                (first, second) -> {
                    int byY = Integer.compare(first.getY(), second.getY());
                    if (byY != 0) {
                        return byY;
                    }
                    int byZ = Integer.compare(first.getZ(), second.getZ());
                    return byZ != 0 ? byZ : Integer.compare(first.getX(), second.getX());
                });

        for (BlockPos pos : ordered) {
            BlockState state = this.level.getBlockState(pos);
            if (state.isAir()) {
                // A chained reaction removed it while this explosion was still marching.
                continue;
            }
            if (doDrops
                    && state.canDropFromExplosion(this.level, pos, this)
                    && this.destroyedBlocks.get(pos)) {
                BlockEntity blockEntity =
                        state.hasBlockEntity() ? this.level.getBlockEntity(pos) : null;
                LootParams.Builder params =
                        new LootParams.Builder(this.level)
                                .withParameter(LootContextParams.ORIGIN, this.center)
                                .withParameter(LootContextParams.TOOL, ItemStack.EMPTY)
                                .withOptionalParameter(LootContextParams.BLOCK_ENTITY, blockEntity)
                                .withOptionalParameter(LootContextParams.THIS_ENTITY, this.source);
                for (ItemStack stack : state.getDrops(params)) {
                    if (!(this.rng.nextFloat() > this.explosionDropRate)) {
                        blocksToDrop
                                .computeIfAbsent(
                                        new DropColumn(pos.getX() / 2, pos.getZ() / 2),
                                        key -> new LinkedHashMap<>())
                                .merge(
                                        stack,
                                        new DropData(stack.getCount(), pos.getY()),
                                        (existing, added) -> {
                                            existing.add(added.count(), added.maxY());
                                            return existing;
                                        });
                    }
                }
            }
            state.onBlockExploded(this.level, pos, this);
        }

        for (Map.Entry<DropColumn, Map<ItemStack, DropData>> columnEntry :
                blocksToDrop.entrySet()) {
            DropColumn column = columnEntry.getKey();
            for (Map.Entry<ItemStack, DropData> stackEntry : columnEntry.getValue().entrySet()) {
                int count = stackEntry.getValue().count();
                while (count > 0) {
                    int stackSize = Math.min(count, stackEntry.getKey().getMaxStackSize());
                    ItemStack merged = stackEntry.getKey().copy();
                    merged.setCount(stackSize);
                    ItemEntity entityItem =
                            new ItemEntity(
                                    this.level,
                                    (column.x() + this.rng.nextFloat()) * 2.0F,
                                    stackEntry.getValue().maxY() + 0.5,
                                    (column.z() + this.rng.nextFloat()) * 2.0F,
                                    merged);
                    entityItem.setDefaultPickUpDelay();
                    this.level.addFreshEntity(entityItem);
                    count -= stackSize;
                }
            }
        }
    }

    private static int square(int value) {
        return value * value;
    }

    private static double square(double value) {
        return value * value;
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

    /** Legacy XZPosition: merges drop spawn points per two-by-two column (truncating division). */
    private record DropColumn(int x, int z) {}

    private static final class DropData {
        private int count;
        private int maxY;

        DropData(int count, int y) {
            this.count = count;
            this.maxY = y;
        }

        int count() {
            return this.count;
        }

        int maxY() {
            return this.maxY;
        }

        void add(int count, int y) {
            this.count += count;
            if (y > this.maxY) {
                this.maxY = y;
            }
        }
    }

    private static class EntityDamage {
        final Entity entity;
        final int distance;
        double health;
        double damage;
        double motionX;
        double motionY;
        double motionZ;

        EntityDamage(Entity entity, int distance, double health) {
            this.entity = entity;
            this.distance = distance;
            this.health = health;
        }
    }
}
