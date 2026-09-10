package ic2.neoforge.crop;

import ic2.core.crop.CropProperties;
import ic2.neoforge.registration.ModCrops;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

import java.util.List;

/**
 * The reed crop (legacy CropReed): humid fast cane, harvestable from age one with age-scaled gains.
 */
public class ReedCropCard implements CropCard {
    @Override
    public String getId() {
        return "reed";
    }

    @Override
    public Block getCropBlock() {
        return ModCrops.REED_CROP.get();
    }

    @Override
    public CropProperties getProperties() {
        return new CropProperties(2, 0, 0, 1, 0, 2);
    }

    @Override
    public int getMaxAge() {
        return 2;
    }

    @Override
    public int getWeightInfluences(CropBlockEntity crop, int humidity, int nutrients, int air) {
        return (int) (humidity * 1.2 + nutrients + air * 0.8);
    }

    @Override
    public boolean canBeHarvested(CropBlockEntity crop) {
        return crop.getCurrentAge() > 0;
    }

    @Override
    public List<ItemStack> getGains(CropBlockEntity crop) {
        return List.of(new ItemStack(Items.SUGAR_CANE, crop.getCurrentAge()));
    }

    @Override
    public boolean onEntityCollision(CropBlockEntity crop, net.minecraft.world.entity.Entity entity) {
        return false;
    }

    @Override
    public int getGrowthDuration(CropBlockEntity crop) {
        return 200;
    }
}
