package ic2.neoforge.entity;

import ic2.neoforge.registration.ModEntities;
import ic2.neoforge.world.Ic2Explosion;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StainedGlassBlock;
import net.minecraft.world.level.block.StainedGlassPaneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

/**
 * Legacy LaserBulletEntity: a zero-gravity projectile that mines, smelts or detonates on
 * impact. It punches through glass and dies on unbreakable or too-hard blocks; every hit and
 * every travelled tick eats into the shot's power, so range is power-driven. Non-explosive
 * shots mine one block per remaining {@code blockBreaks} and place smelted replacements back
 * into the mined position.
 */
public class LaserBulletEntity extends ThrowableProjectile {
    public static final float EXPLOSION_POWER = 5.0F;
    public static final float EXPLOSION_DROP_RATE = 0.85F;

    public float range = 0.0F;
    public float power = 0.0F;
    public int blockBreaks = 0;
    public boolean isExplosiveMode = false;
    public boolean isSmeltMode = false;
    public boolean removeBlock = false;

    public LaserBulletEntity(EntityType<? extends LaserBulletEntity> type, Level level) {
        super(type, level);
    }

    public LaserBulletEntity(
            Level level,
            Vec3 start,
            LivingEntity owner,
            float range,
            float power,
            int blockBreaks,
            boolean isExplosiveMode) {
        super(ModEntities.LASER_BULLET.get(), level);
        this.setOwner(owner);
        this.setPos(start.x, start.y, start.z);
        this.range = range;
        this.power = power;
        this.blockBreaks = blockBreaks;
        this.isExplosiveMode = isExplosiveMode;
    }

    public void init(
            LivingEntity owner,
            float range,
            float power,
            int blockBreaks,
            boolean explosive,
            boolean smelt,
            boolean removeBlock) {
        this.setOwner(owner);
        this.range = range;
        this.power = power;
        this.blockBreaks = blockBreaks;
        this.isExplosiveMode = explosive;
        this.isSmeltMode = smelt;
        this.removeBlock = removeBlock;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected double getDefaultGravity() {
        return 0.0;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level() instanceof ServerLevel) {
            if (this.range < 1.0F || this.power <= 0.0F || this.blockBreaks <= 0) {
                if (this.isExplosiveMode) {
                    this.explode();
                }
                this.discard();
            } else {
                this.power -= 0.5F;
            }
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult hitResult) {
        super.onHitBlock(hitResult);
        this.handleHit(hitResult);
    }

    @Override
    protected void onHitEntity(EntityHitResult hitResult) {
        super.onHitEntity(hitResult);
        this.handleHit(hitResult);
    }

    private void handleHit(HitResult hitResult) {
        if (this.isExplosiveMode) {
            if (hitResult instanceof EntityHitResult entityHit
                    && this.isBossEntity(entityHit.getEntity())) {
                this.hitEntity(entityHit.getEntity());
            } else {
                this.explode();
            }
            this.discard();
            return;
        }

        switch (hitResult.getType()) {
            case ENTITY -> {
                if (this.hitEntity(((EntityHitResult) hitResult).getEntity())) {
                    this.power -= 0.5F;
                } else {
                    this.discard();
                }
            }
            case BLOCK -> {
                BlockHitResult blockHit = (BlockHitResult) hitResult;
                if (this.hitBlock(blockHit.getBlockPos(), blockHit.getDirection())) {
                    this.discard();
                } else {
                    this.power -= 0.5F;
                }
            }
            default -> throw new RuntimeException("invalid hit type: " + hitResult.getType());
        }
    }

    private void explode() {
        if (this.level() instanceof ServerLevel level) {
            new Ic2Explosion(
                            level,
                            this,
                            this.getOwner() instanceof LivingEntity living ? living : null,
                            this.getX(),
                            this.getY(),
                            this.getZ(),
                            EXPLOSION_POWER,
                            EXPLOSION_DROP_RATE,
                            Ic2Explosion.Type.Normal,
                            0)
                    .doExplosion();
        }
    }

    private boolean hitEntity(Entity entity) {
        int damage = (int) this.power;
        if (this.isBossEntity(entity)) {
            damage = Math.min(damage, 5);
        }

        if (damage > 0 && this.level() instanceof ServerLevel serverLevel) {
            entity.igniteForSeconds(damage * (this.isSmeltMode ? 2 : 1));
            return entity.hurtServer(
                    serverLevel,
                    serverLevel
                            .damageSources()
                            .mobProjectile(
                                    this,
                                    this.getOwner() instanceof LivingEntity living ? living : null),
                    damage);
        }
        return true;
    }

    private boolean isBossEntity(Entity entity) {
        return entity instanceof EnderDragon || entity instanceof WitherBoss;
    }

    private boolean hitBlock(BlockPos pos, Direction side) {
        if (!(this.level() instanceof ServerLevel level)) {
            return true;
        }

        Player playerOwner = this.getOwner() instanceof Player player ? player : null;
        if (playerOwner == null) {
            // Legacy fell back to a shared fake player; the gun only ever spawns shots with a
            // living player owner, so without one the ray keeps flying.
            return false;
        }

        var server = level.getServer();
        if (server != null
                && playerOwner.blockActionRestricted(
                        level, pos, server.getDefaultGameType())) {
            return false;
        }

        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        if (state.isAir()
                || block == Blocks.GLASS
                || block == Blocks.GLASS_PANE
                || block instanceof StainedGlassPaneBlock
                || block instanceof StainedGlassBlock) {
            return false;
        }

        float hardness = state.getDestroySpeed(level, pos);
        if (hardness < 0.0F) {
            this.discard();
            return true;
        }

        this.power -= hardness / 1.5F;
        if (this.power < 0.0F) {
            return true;
        }

        boolean dropBlock = true;
        List<ItemStack> replacements = new ArrayList<>();
        if (block == Blocks.TNT) {
            // 26.1.2 turned Explosion into an interface, so prime the charge with the inline
            // equivalent of TntBlock.wasExploded (randomized short fuse, owner credited).
            if (level.getGameRules().get(GameRules.TNT_EXPLODES)) {
                PrimedTnt primed =
                        new PrimedTnt(
                                level,
                                pos.getX() + 0.5,
                                pos.getY(),
                                pos.getZ() + 0.5,
                                playerOwner);
                int fuse = primed.getFuse();
                primed.setFuse((short) (level.getRandom().nextInt(fuse / 4) + fuse / 8));
                level.addFreshEntity(primed);
            }
        } else if (this.isSmeltMode) {
            if (state.getFlammability(level, pos, side) > 0) {
                dropBlock = false;
            } else {
                for (ItemStack drop : this.drops(state, level, pos)) {
                    this.appendSmeltItemStack(level, block, drop, replacements);
                }
                dropBlock = replacements.isEmpty();
            }
        }

        if (this.removeBlock) {
            if (dropBlock) {
                Block.dropResources(state, level, pos);
            }

            level.removeBlock(pos, false);

            for (ItemStack replacement : replacements) {
                if (!placeBlock(replacement, level, pos)) {
                    Block.popResource(level, pos, replacement);
                }
                this.power = 0.0F;
            }

            if (this.random.nextInt(10) == 0
                    && state.getFlammability(level, pos, Direction.UP) > 0) {
                level.setBlockAndUpdate(pos, Blocks.FIRE.defaultBlockState());
            }
        }

        this.blockBreaks--;
        return true;
    }

    private List<ItemStack> drops(BlockState state, ServerLevel level, BlockPos pos) {
        return state.getDrops(
                new LootParams.Builder(level)
                        .withParameter(
                                LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                        .withParameter(LootContextParams.TOOL, ItemStack.EMPTY));
    }

    private void appendSmeltItemStack(
            ServerLevel level,
            Block targetBlock,
            ItemStack inputItemStack,
            List<ItemStack> replacementList) {
        if (inputItemStack.getItem() instanceof BlockItem blockItem
                && blockItem.getBlock() != targetBlock) {
            inputItemStack = new ItemStack(targetBlock.asItem());
        }

        SingleRecipeInput input = new SingleRecipeInput(inputItemStack);
        var recipe =
                level.recipeAccess()
                        .getRecipeFor(RecipeType.SMELTING, input, level)
                        .orElse(null);
        if (recipe != null) {
            ItemStack replacement = recipe.value().assemble(input);
            if (!replacement.isEmpty()) {
                replacementList.add(replacement);
            }
        }
    }

    private static boolean placeBlock(ItemStack stack, ServerLevel level, BlockPos pos) {
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return false;
        }

        return level.setBlockAndUpdate(pos, blockItem.getBlock().defaultBlockState());
    }

    @Nullable
    public LivingEntity getOwnerLiving() {
        return this.getOwner() instanceof LivingEntity living ? living : null;
    }
}
