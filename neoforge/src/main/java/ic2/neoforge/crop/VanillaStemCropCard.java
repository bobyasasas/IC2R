package ic2.neoforge.crop;

import ic2.core.crop.CropProperties;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.function.Supplier;

/**
 * Legacy CropVanillaStem: vanilla produce crops grown on the stick (pumpkin, melon) weight the
 * terrain qualities towards humidity and regrow one age below full ripeness.
 */
public class VanillaStemCropCard extends VanillaProduceCropCard {
    public VanillaStemCropCard(
            String id, Supplier<Block> block, int maxAge, CropProperties properties,
            Supplier<ItemStack> produce, Supplier<ItemStack> seeds, String[] attributes) {
        super(id, block, maxAge, properties, produce, seeds, attributes);
    }

    @Override
    public int getWeightInfluences(CropBlockEntity crop, int humidity, int nutrients, int air) {
        return (int) (humidity * 1.1 + nutrients * 0.9 + air);
    }

    @Override
    public int getAgeAfterHarvest(CropBlockEntity crop) {
        return getMaxAge() - 1;
    }
}
