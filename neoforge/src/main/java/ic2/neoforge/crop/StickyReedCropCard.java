package ic2.neoforge.crop;

import ic2.core.crop.CropProperties;
import ic2.neoforge.registration.MaterialDefinition;
import ic2.neoforge.registration.ModItems;
import ic2.neoforge.registration.ModCrops;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

import java.util.List;

/**
 * The sticky reed crop (legacy CropStickyReed): a reed whose full bloom oozes resin instead of
 * cane; it resists trampling and skips an age after a ripe harvest.
 */
public class StickyReedCropCard implements CropCard {
    @Override
    public String getId() {
        return "sticky_reed";
    }

    @Override
    public Block getCropBlock() {
        return ModCrops.STICKY_REED_CROP.get();
    }

    @Override
    public CropProperties getProperties() {
        return new CropProperties(4, 2, 0, 1, 0, 1);
    }

    @Override
    public int getMaxAge() {
        return 3;
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
        if (crop.getCurrentAge() <= getMaxAge() - 1) {
            return List.of(new ItemStack(Items.SUGAR_CANE, crop.getCurrentAge()));
        }
        return List.of(new ItemStack(ModItems.MATERIALS.get(MaterialDefinition.RESIN).get()));
    }

    @Override
    public int getAgeAfterHarvest(CropBlockEntity crop) {
        if (crop.getCurrentAge() != getMaxAge() || crop.getLevel() == null) return 0;
        return 2 - crop.getLevel().getRandom().nextInt(2);
    }

    @Override
    public boolean onEntityCollision(CropBlockEntity crop, net.minecraft.world.entity.Entity entity) {
        return false;
    }

    @Override
    public int getGrowthDuration(CropBlockEntity crop) {
        return crop.getCurrentAge() == getMaxAge() ? 400 : 100;
    }
}
