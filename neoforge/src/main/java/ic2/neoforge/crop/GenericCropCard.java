package ic2.neoforge.crop;

import ic2.core.crop.CropProperties;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * The legacy data-driven crop card (legacy GenericCropCard): fixed drops plus a special-drop
 * roulette, tier-scaled growth durations and configurable harvest window. Fifteen IC2 crops use
 * it directly; item stacks are supplied lazily because the material items register later.
 */
public class GenericCropCard implements CropCard {
    private final String id;
    private final Supplier<Block> block;
    private final CropProperties properties;
    private final int maxSize;
    private final List<Supplier<ItemStack>> drops;
    private final List<Supplier<ItemStack>> specialDrops;
    private final int growthSpeed;
    private final int harvestSize;
    private final int optimalHarvestSize;
    private final int afterHarvestSize;
    private final List<TagKey<Block>> rootRequirements;

    /** The shape all fifteen legacy data crops use: default harvest window, no root gate. */
    public GenericCropCard(
            String id,
            Supplier<Block> block,
            CropProperties properties,
            int maxSize,
            List<Supplier<ItemStack>> drops,
            List<Supplier<ItemStack>> specialDrops,
            int growthSpeed,
            int afterHarvestSize) {
        this(id, block, properties, maxSize, drops, specialDrops, growthSpeed,
                0, 0, afterHarvestSize, List.of());
    }

    public GenericCropCard(
            String id,
            Supplier<Block> block,
            CropProperties properties,
            int maxSize,
            List<Supplier<ItemStack>> drops,
            List<Supplier<ItemStack>> specialDrops,
            int growthSpeed,
            int harvestSize,
            int optimalHarvestSize,
            int afterHarvestSize,
            List<TagKey<Block>> rootRequirements) {
        this.id = id;
        this.block = block;
        this.properties = properties;
        this.maxSize = maxSize;
        this.drops = List.copyOf(drops);
        this.specialDrops = List.copyOf(specialDrops);
        this.growthSpeed = growthSpeed;
        // Legacy register() normalization: unset sizes fall back to the defaults.
        this.harvestSize = harvestSize >= 2 ? harvestSize : maxSize - 1;
        this.optimalHarvestSize =
                optimalHarvestSize >= 2 ? optimalHarvestSize : this.harvestSize;
        this.afterHarvestSize = afterHarvestSize;
        this.rootRequirements = List.copyOf(rootRequirements);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Block getCropBlock() {
        return block.get();
    }

    @Override
    public CropProperties getProperties() {
        return properties;
    }

    @Override
    public int getMaxAge() {
        return maxSize;
    }

    @Override
    public boolean canGrow(CropBlockEntity crop) {
        if (!rootRequirements.isEmpty() && crop.getCurrentAge() == maxSize - 1) {
            for (TagKey<Block> tag : rootRequirements) {
                if (crop.isBlockBelow(tag)) return true;
            }
            return false;
        }
        return crop.getCurrentAge() < maxSize;
    }

    @Override
    public boolean canBeHarvested(CropBlockEntity crop) {
        return crop.getCurrentAge() >= harvestSize;
    }

    @Override
    public int getOptimalHarvestAge() {
        return optimalHarvestSize;
    }

    @Override
    public int getAgeAfterHarvest(CropBlockEntity crop) {
        return afterHarvestSize;
    }

    @Override
    public int getGrowthDuration(CropBlockEntity crop) {
        return growthSpeed < 200 ? properties.tier() * 200 : properties.tier() * growthSpeed;
    }

    @Override
    public List<ItemStack> getGains(CropBlockEntity crop) {
        List<ItemStack> gains = new ArrayList<>();
        for (Supplier<ItemStack> drop : drops) gains.add(drop.get().copy());
        if (!specialDrops.isEmpty() && crop.getLevel() != null) {
            // Legacy roulette: one roll where each special entry wins one slot of
            // length*2+2, so a single special drops a quarter of the harvests.
            int roulette =
                    crop.getLevel()
                            .getRandom()
                            .nextInt(specialDrops.size() * 2 + 2);
            if (roulette < specialDrops.size()) {
                gains.add(specialDrops.get(roulette).get().copy());
            }
        }
        return gains;
    }

    /** Legacy canCross: a crop joins cross-breeding once it is two steps from full. */
    public boolean canCross(CropBlockEntity crop) {
        return crop.getCurrentAge() + 2 > maxSize;
    }
}
