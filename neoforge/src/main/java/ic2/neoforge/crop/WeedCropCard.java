package ic2.neoforge.crop;

import ic2.core.crop.CropProperties;
import ic2.neoforge.registration.ModCrops;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.List;

/**
 * The weed crop (legacy CropWeed): grows on its own, spreads over neighbouring sticks, yields
 * nothing and cannot be picked.
 */
public class WeedCropCard implements CropCard {
    @Override
    public String getId() {
        return "weed";
    }

    @Override
    public Block getCropBlock() {
        return ModCrops.WEED_CROP.get();
    }

    @Override
    public CropProperties getProperties() {
        return new CropProperties(0, 0, 0, 1, 0, 5);
    }

    @Override
    public int getMaxAge() {
        return 4;
    }

    @Override
    public int getGrowthDuration(CropBlockEntity crop) {
        return 300;
    }

    @Override
    public boolean canBeHarvested(CropBlockEntity crop) {
        return false;
    }

    @Override
    public List<ItemStack> getGains(CropBlockEntity crop) {
        return List.of();
    }

    @Override
    public ItemStack getSeedsItem(CropBlockEntity crop) {
        return crop.generateSeeds(
                this,
                crop.getStatGrowth(),
                crop.getStatGain(),
                crop.getStatResistance(),
                crop.getScanLevel());
    }

    @Override
    public boolean onEntityCollision(CropBlockEntity crop, net.minecraft.world.entity.Entity entity) {
        return false;
    }

    @Override
    public String[] getAttributes() {
        return new String[] { "Weed", "Bad" };
    }
}
