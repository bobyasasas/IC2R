package ic2.neoforge.crop;

import ic2.core.crop.CropProperties;
import ic2.neoforge.item.MetalArmorLike;
import ic2.neoforge.registration.ModCrops;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * The eating plant crop (legacy CropEating): lava feeds its late growth, ripe stalks are cut
 * before the full age for cactus, and from age one on it bites the creatures around it —
 * dragging them in, hurting them and blinding them unless they wear IC2 metal armor.
 */
public class EatingPlantCropCard implements CropCard {
    public static final ResourceKey<DamageType> CROP_EATING_TYPE =
            ResourceKey.create(
                    Registries.DAMAGE_TYPE,
                    Identifier.fromNamespaceAndPath("ic2", "crop_eating"));

    @Override
    public String getId() {
        return "eating_plant";
    }

    @Override
    public Block getCropBlock() {
        return ModCrops.EATING_PLANT_CROP.get();
    }

    @Override
    public CropProperties getProperties() {
        return new CropProperties(6, 1, 1, 3, 1, 4);
    }

    @Override
    public int getMaxAge() {
        return 5;
    }

    @Override
    public boolean canGrow(CropBlockEntity crop) {
        if (crop.getLightLevel() <= 10) return false;
        if (crop.getCurrentAge() < 2) return true;
        return crop.isBlockBelow(Blocks.LAVA) && crop.getCurrentAge() < getMaxAge();
    }

    @Override
    public int getOptimalHarvestAge() {
        return getMaxAge() - 2;
    }

    @Override
    public boolean canBeHarvested(CropBlockEntity crop) {
        return crop.getCurrentAge() >= getOptimalHarvestAge()
                && crop.getCurrentAge() < getMaxAge();
    }

    @Override
    public List<ItemStack> getGains(CropBlockEntity crop) {
        return canBeHarvested(crop) ? List.of(new ItemStack(Blocks.CACTUS)) : List.of();
    }

    @Override
    public void tick(CropBlockEntity crop) {
        if (crop.getCurrentAge() == 0) return;
        if (!(crop.getLevel() instanceof ServerLevel level)) return;
        BlockPos pos = crop.getBlockPos();
        double x = pos.getX() + 0.5;
        double z = pos.getZ() + 0.5;
        if (crop.hasEaten()) {
            crop.setEaten(false);
            net.minecraft.world.Containers.dropItemStack(
                    level, pos.getX(), pos.getY(), pos.getZ(),
                    new ItemStack(Items.ROTTEN_FLESH));
        }
        List<LivingEntity> list = level.getEntitiesOfClass(
                LivingEntity.class,
                new AABB(
                        x - 1.0, pos.getY(), z - 1.0,
                        x + 1.0, pos.getY() + 2.0, z + 1.0),
                EntitySelector.NO_CREATIVE_OR_SPECTATOR);
        if (list.isEmpty()) return;
        shuffle(list, level.getRandom());
        for (LivingEntity entity : list) {
            if (entity instanceof Player player && player.getAbilities().instabuild) continue;
            entity.setDeltaMovement(
                    (x - entity.getX()) * 0.5,
                    Math.min(entity.getDeltaMovement().y, -0.05),
                    (z - entity.getZ()) * 0.5);
            DamageSource source = new DamageSource(
                    level.damageSources().damageTypes.getOrThrow(CROP_EATING_TYPE));
            entity.hurtServer(level, source, (crop.getCurrentAge() + 1) * 2.0F);
            if (!hasMetalArmor(entity)) {
                entity.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 64, 50));
                entity.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 64, 0));
                entity.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 64, 0));
            }
            if (canGrow(crop)) {
                crop.setGrowthPoints(crop.getGrowthPoints() + 100);
            }
            level.playSound(
                    null, x, pos.getY() + 0.5, z,
                    SoundEvents.GENERIC_EAT, SoundSource.BLOCKS,
                    1.0F, level.getRandom().nextFloat() * 0.1F + 0.9F);
            crop.setEaten(true);
            break;
        }
    }

    @Override
    public int getRootsLength(CropBlockEntity crop) {
        return 5;
    }

    @Override
    public int getGrowthDuration(CropBlockEntity crop) {
        // Legacy additionally divides by 1.5 in swamp or mountain biomes; the environment biome
        // types are not ported, so that bonus stays out (documented slice-one policy).
        float multiplier = 1.0F;
        multiplier /= 1.0F + crop.getTerrainAirQuality() / 10.0F;
        return (int) (CropCard.super.getGrowthDuration(crop) * multiplier);
    }

    /** Legacy Collections.shuffle over the shared random (RandomSource is not a java.util.Random). */
    private static void shuffle(List<LivingEntity> list, net.minecraft.util.RandomSource random) {
        for (int i = list.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            LivingEntity held = list.set(i, list.get(j));
            list.set(j, held);
        }
    }

    private static boolean hasMetalArmor(LivingEntity entity) {
        if (!(entity instanceof Player player)) return false;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR) continue;
            if (player.getItemBySlot(slot).getItem() instanceof MetalArmorLike) return true;
        }
        return false;
    }

    @Override
    public String[] getAttributes() {
        return new String[] { "Bad", "Food" };
    }
}
