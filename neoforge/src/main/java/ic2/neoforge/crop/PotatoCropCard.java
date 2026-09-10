package ic2.neoforge.crop;

import ic2.core.crop.CropProperties;
import ic2.neoforge.registration.ModCrops;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

import java.util.List;

/**
 * Legacy CropPotato: light-gated growth; a fully ripe potato has a 5% chance to be poisonous.
 */
public class PotatoCropCard implements CropCard {
    @Override
    public String getId() {
        return "potato";
    }

    @Override
    public Block getCropBlock() {
        return ModCrops.POTATO_CROP.get();
    }

    @Override
    public CropProperties getProperties() {
        return new CropProperties(2, 0, 4, 0, 0, 2);
    }

    @Override
    public int getMaxAge() {
        return 3;
    }

    @Override
    public boolean canGrow(CropBlockEntity crop) {
        return crop.getCurrentAge() < getMaxAge() && crop.getLightLevel() >= 9;
    }

    @Override
    public int getOptimalHarvestAge() {
        return getMaxAge() - 1;
    }

    @Override
    public boolean canBeHarvested(CropBlockEntity crop) {
        return crop.getCurrentAge() >= getMaxAge() - 1;
    }

    @Override
    public List<ItemStack> getGains(CropBlockEntity crop) {
        if (crop.getCurrentAge() >= getMaxAge()
                && crop.getLevel() != null
                && crop.getLevel().getRandom().nextInt(20) == 0) {
            return List.of(new ItemStack(Items.POISONOUS_POTATO));
        }
        if (crop.getCurrentAge() >= getMaxAge() - 1) {
            return List.of(new ItemStack(Items.POTATO));
        }
        return List.of();
    }
}
