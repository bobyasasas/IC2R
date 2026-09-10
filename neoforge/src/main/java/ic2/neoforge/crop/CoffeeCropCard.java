package ic2.neoforge.crop;

import ic2.core.crop.CropProperties;
import ic2.neoforge.registration.ModCrops;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.List;

/**
 * The coffee crop (legacy CropCoffee): tier seven with per-stage growth durations, a harvest window
 * that opens one age before maturity and beans only on the fully ripe plant.
 */
public class CoffeeCropCard implements CropCard {
    @Override
    public String getId() {
        return "coffee";
    }

    @Override
    public Block getCropBlock() {
        return ModCrops.COFFEE_CROP.get();
    }

    @Override
    public CropProperties getProperties() {
        return new CropProperties(7, 1, 4, 1, 2, 0);
    }

    @Override
    public int getMaxAge() {
        return 4;
    }

    @Override
    public boolean canGrow(CropBlockEntity crop) {
        return crop.getCurrentAge() < getMaxAge() && crop.getLightLevel() >= 9;
    }

    @Override
    public int getWeightInfluences(CropBlockEntity crop, int humidity, int nutrients, int air) {
        return (int) (0.4 * humidity + 1.4 * nutrients + 1.2 * air);
    }

    @Override
    public int getGrowthDuration(CropBlockEntity crop) {
        int base = getProperties().tier() * 200;
        if (crop.getCurrentAge() == getMaxAge() - 2) return (int) (base * 0.5);
        return crop.getCurrentAge() == getMaxAge() - 3 ? (int) (base * 1.5) : base;
    }

    @Override
    public boolean canBeHarvested(CropBlockEntity crop) {
        return crop.getCurrentAge() >= getMaxAge() - 1;
    }

    @Override
    public List<ItemStack> getGains(CropBlockEntity crop) {
        if (crop.getCurrentAge() == getMaxAge() - 1) return List.of();
        return List.of(
                new ItemStack(
                        ic2.neoforge.registration.ModItems.MATERIALS
                                .get(ic2.neoforge.registration.MaterialDefinition.COFFEE_BEANS)
                                .get()));
    }

    @Override
    public int getOptimalHarvestAge() {
        return getMaxAge() - 2;
    }
}
