package ic2.neoforge.crop;

import ic2.core.crop.CropProperties;
import ic2.neoforge.registration.ModCrops;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.List;

/**
 * The red wheat crop (legacy CropRedWheat): grows only in dim light, glows and emits a full
 * redstone signal once ripe, and the harvest splits between wheat and redstone depending on
 * whether any neighbor powers the crop.
 */
public class RedWheatCropCard implements CropCard {
    @Override
    public String getId() {
        return "red_wheat";
    }

    @Override
    public Block getCropBlock() {
        return ModCrops.RED_WHEAT_CROP.get();
    }

    @Override
    public CropProperties getProperties() {
        return new CropProperties(6, 3, 0, 0, 2, 0);
    }

    @Override
    public int getMaxAge() {
        return 6;
    }

    @Override
    public boolean canGrow(CropBlockEntity crop) {
        int light = crop.getLightLevel();
        return crop.getCurrentAge() < getMaxAge() && light <= 10 && light >= 5;
    }

    @Override
    public double dropGainChance() {
        return 0.5;
    }

    @Override
    public List<ItemStack> getGains(CropBlockEntity crop) {
        Level level = crop.getLevel();
        // Legacy: an unpowered crop splits evenly between wheat and redstone; any neighbor
        // signal forces the redstone drop.
        Item drop = Items.REDSTONE;
        if (level != null && level.getBestNeighborSignal(crop.getBlockPos()) <= 0
                && !level.getRandom().nextBoolean()) {
            drop = Items.WHEAT;
        }
        return List.of(new ItemStack(drop));
    }

    @Override
    public int getGrowthDuration(CropBlockEntity crop) {
        return 600;
    }

    @Override
    public int getAgeAfterHarvest(CropBlockEntity crop) {
        return 1;
    }

    @Override
    public String[] getAttributes() {
        return new String[] { "Red", "Redstone", "Wheat" };
    }
}
