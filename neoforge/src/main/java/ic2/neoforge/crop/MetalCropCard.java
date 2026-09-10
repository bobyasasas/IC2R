package ic2.neoforge.crop;

import ic2.core.crop.CropProperties;
import ic2.neoforge.registration.ModCrops;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.function.Supplier;

/**
 * Shared base of the six metal crops (legacy CropBaseMetalCommon and its uncommon subclass): the
 * last growth step only proceeds when one of the requirement tags sits in the root zone, and the
 * harvest is the crop's small dust. Common cards halve the gain drop chance; the uncommon pair
 * (aurelia, shining) keeps the tier default but grows slower.
 */
public class MetalCropCard implements CropCard {
    private final String id;
    private final Supplier<Block> block;
    private final Supplier<ItemStack> cropDrop;
    private final List<TagKey<Block>> rootsRequirement;
    private final boolean uncommon;

    public MetalCropCard(
            String id,
            Supplier<Block> block,
            List<TagKey<Block>> rootsRequirement,
            Supplier<ItemStack> cropDrop,
            boolean uncommon) {
        this.id = id;
        this.block = block;
        this.rootsRequirement = rootsRequirement;
        this.cropDrop = cropDrop;
        this.uncommon = uncommon;
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
        return uncommon ? new CropProperties(6, 2, 0, 0, 2, 0) : new CropProperties(6, 2, 0, 0, 1, 0);
    }

    @Override
    public int getMaxAge() {
        return 3;
    }

    @Override
    public boolean canGrow(CropBlockEntity crop) {
        int sizeReq = getMaxAge() - 1;
        if (crop.getCurrentAge() < sizeReq) return true;
        if (crop.getCurrentAge() == sizeReq) {
            for (TagKey<Block> tag : rootsRequirement) {
                if (crop.isBlockBelow(tag)) return true;
            }
        }
        return false;
    }

    @Override
    public int getRootsLength(CropBlockEntity crop) {
        return 5;
    }

    @Override
    public List<ItemStack> getGains(CropBlockEntity crop) {
        return List.of(cropDrop.get().copy());
    }

    @Override
    public double dropGainChance() {
        return uncommon ? Math.pow(0.95, getProperties().tier())
                        : CropCard.super.dropGainChance() / 2.0;
    }

    @Override
    public int getGrowthDuration(CropBlockEntity crop) {
        return crop.getCurrentAge() == getMaxAge() - 1 ? (uncommon ? 2200 : 2000)
                                                       : (uncommon ? 750 : 800);
    }

    @Override
    public int getAgeAfterHarvest(CropBlockEntity crop) {
        return 1;
    }
}
