package ic2.neoforge.crop;

import ic2.core.crop.CropProperties;
import ic2.neoforge.registration.ModCrops;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

import java.util.List;

/** The cocoa crop (legacy CropCocoa): refuses to grow without three stored nutrients. */
public class CocoaCropCard implements CropCard {
    @Override
    public String getId() {
        return "cocoa";
    }

    @Override
    public Block getCropBlock() {
        return ModCrops.COCOA_CROP.get();
    }

    @Override
    public CropProperties getProperties() {
        return new CropProperties(3, 1, 3, 0, 4, 0);
    }

    @Override
    public int getMaxAge() {
        return 3;
    }

    @Override
    public boolean canGrow(CropBlockEntity crop) {
        return crop.getCurrentAge() <= getMaxAge() - 1 && crop.getStorageNutrients() >= 3;
    }

    @Override
    public int getGrowthDuration(CropBlockEntity crop) {
        return crop.getCurrentAge() == getMaxAge() - 1 ? 900 : 400;
    }

    @Override
    public boolean canBeHarvested(CropBlockEntity crop) {
        return crop.getCurrentAge() == getMaxAge() - 1;
    }

    @Override
    public List<ItemStack> getGains(CropBlockEntity crop) {
        return List.of(new ItemStack(Items.COCOA_BEANS));
    }

    @Override
    public String[] getAttributes() {
        return new String[] { "Brown", "Food", "Stem" };
    }
}
