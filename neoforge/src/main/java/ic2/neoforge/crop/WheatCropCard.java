package ic2.neoforge.crop;

import ic2.core.crop.CropProperties;
import ic2.neoforge.registration.ModCrops;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

import java.util.List;

/** The wheat crop (legacy CropWheat): a tier-one vanilla-food crop with vanilla seed drops. */
public class WheatCropCard implements CropCard {
    @Override
    public String getId() {
        return "wheat";
    }

    @Override
    public Block getCropBlock() {
        return ModCrops.WHEAT_CROP.get();
    }

    @Override
    public CropProperties getProperties() {
        return new CropProperties(1, 0, 4, 0, 0, 2);
    }

    @Override
    public int getMaxAge() {
        return 7;
    }

    @Override
    public boolean canGrow(CropBlockEntity crop) {
        return crop.getCurrentAge() < getMaxAge() && crop.getLightLevel() >= 9;
    }

    @Override
    public List<ItemStack> getGains(CropBlockEntity crop) {
        return List.of(new ItemStack(Items.WHEAT, 1));
    }

    @Override
    public ItemStack getSeedsItem(CropBlockEntity crop) {
        if (crop.getStatGain() <= 1 && crop.getStatGrowth() <= 1 && crop.getStatResistance() <= 1) {
            return new ItemStack(Items.WHEAT_SEEDS);
        }
        return crop.generateSeeds(
                this,
                crop.getStatGrowth(),
                crop.getStatGain(),
                crop.getStatResistance(),
                crop.getScanLevel());
    }

    @Override
    public int getAgeAfterHarvest(CropBlockEntity crop) {
        return 2;
    }

    @Override
    public String[] getAttributes() {
        return new String[] { "Yellow", "Food", "Wheat" };
    }
}
