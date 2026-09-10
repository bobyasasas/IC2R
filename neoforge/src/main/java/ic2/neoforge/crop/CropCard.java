package ic2.neoforge.crop;

import ic2.core.crop.CropProperties;
import ic2.neoforge.registration.ModCrops;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.List;

/**
 * Legacy CropCard surface used by the crop tile: identification, growth rules and drops. Cards
 * resolve the plant block and the tile drives the shared growth loop.
 */
public interface CropCard {
    /** Unique legacy id, e.g. {@code wheat}. */
    String getId();

    Block getCropBlock();

    CropProperties getProperties();

    int getMaxAge();

    default int getGrowthDuration(CropBlockEntity crop) {
        return getProperties().tier() * 200;
    }

    default boolean canGrow(CropBlockEntity crop) {
        return crop.getCurrentAge() < getMaxAge();
    }

    default int getWeightInfluences(CropBlockEntity crop, int humidity, int nutrients, int air) {
        return humidity + nutrients + air;
    }

    default double dropGainChance() {
        return Math.pow(0.95, getProperties().tier());
    }

    default float dropSeedChance(CropBlockEntity crop) {
        if (crop.getCurrentAge() == 0) return 0.0F;
        float base = 0.5F;
        if (crop.getCurrentAge() == 1) base /= 2.0F;
        for (int i = 0; i < getProperties().tier(); i++) base = (float) (base * 0.8);
        return base;
    }

    default boolean canBeHarvested(CropBlockEntity crop) {
        return crop.getCurrentAge() == getMaxAge();
    }

    /** How deep below the stick the crop's roots reach (legacy getRootsLength). */
    default int getRootsLength(CropBlockEntity crop) {
        return 1;
    }

    default int getOptimalHarvestAge() {
        return getMaxAge();
    }

    default int getAgeAfterHarvest(CropBlockEntity crop) {
        return 0;
    }

    default boolean isWeed(CropBlockEntity crop) {
        return crop.getCurrentAge() >= 1
                && (this == ModCrops.WEED_CARD || crop.getStatGrowth() >= 24);
    }

    default boolean onEntityCollision(CropBlockEntity crop, Entity entity) {
        return entity instanceof LivingEntity living && living.isSprinting();
    }

    /** Legacy onRightClick: harvest a harvestable crop; cards add their side effects. */
    default boolean onRightClick(CropBlockEntity crop, Player player) {
        return crop.performManualHarvest();
    }

    /** Legacy onLeftClick: pick the seeds; cards add their side effects. */
    default boolean onLeftClick(CropBlockEntity crop, Player player) {
        return crop.pick();
    }

    default void tick(CropBlockEntity crop) {}

    default List<ItemStack> getGains(CropBlockEntity crop) {
        return List.of();
    }

    /** The pick drop (legacy getSeeds(ICropTile)); defaults to a stat-carrying seed bag. */
    default ItemStack getSeedsItem(CropBlockEntity crop) {
        return crop.generateSeeds(
                this,
                crop.getStatGrowth(),
                crop.getStatGain(),
                crop.getStatResistance(),
                crop.getScanLevel());
    }
}
