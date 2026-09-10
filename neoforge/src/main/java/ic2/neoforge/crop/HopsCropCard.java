package ic2.neoforge.crop;

import ic2.core.crop.CropProperties;
import ic2.neoforge.registration.ModCrops;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.List;

/** The hops crop (legacy CropHops): a tier-five brewing ingredient with a long season. */
public class HopsCropCard implements CropCard {
    @Override
    public String getId() {
        return "hops";
    }

    @Override
    public Block getCropBlock() {
        return ModCrops.HOPS_CROP.get();
    }

    @Override
    public CropProperties getProperties() {
        return new CropProperties(5, 2, 2, 0, 1, 1);
    }

    @Override
    public int getMaxAge() {
        return 6;
    }

    @Override
    public int getGrowthDuration(CropBlockEntity crop) {
        return 600;
    }

    @Override
    public boolean canGrow(CropBlockEntity crop) {
        return crop.getCurrentAge() < getMaxAge() && crop.getLightLevel() >= 9;
    }

    @Override
    public List<ItemStack> getGains(CropBlockEntity crop) {
        return List.of(
                new ItemStack(
                        ic2.neoforge.registration.ModItems.MATERIALS
                                .get(ic2.neoforge.registration.MaterialDefinition.HOPS)
                                .get()));
    }

    @Override
    public int getAgeAfterHarvest(CropBlockEntity crop) {
        return 2;
    }

    @Override
    public String[] getAttributes() {
        return new String[] { "Green", "Ingredient", "Wheat" };
    }
}
