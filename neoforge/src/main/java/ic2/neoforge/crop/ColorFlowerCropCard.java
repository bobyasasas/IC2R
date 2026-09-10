package ic2.neoforge.crop;

import ic2.core.crop.CropProperties;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.function.Supplier;

/**
 * Legacy CropColorFlower (dandelion, poppy, blackthorn, tulip, cyazint): sun-hungry flowers that
 * yield their dye once a bloom and regrow to the harvest age.
 */
public class ColorFlowerCropCard implements CropCard {
    private final String id;
    private final Supplier<Block> block;
    private final Supplier<ItemStack> gain;
    private final String[] attributes;

    public ColorFlowerCropCard(
            String id, Supplier<Block> block, Supplier<ItemStack> gain, String[] attributes) {
        this.id = id;
        this.block = block;
        this.gain = gain;
        this.attributes = attributes;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Block getCropBlock() {
        return block.get();
    }

    @Override
    public CropProperties getProperties() {
        return new CropProperties(2, 1, 1, 0, 5, 1);
    }

    @Override
    public int getMaxAge() {
        return 3;
    }

    @Override
    public boolean canGrow(CropBlockEntity crop) {
        return crop.getCurrentAge() <= getMaxAge() - 1 && crop.getLightLevel() >= 12;
    }

    @Override
    public List<ItemStack> getGains(CropBlockEntity crop) {
        return List.of(gain.get());
    }

    @Override
    public int getAgeAfterHarvest(CropBlockEntity crop) {
        return getMaxAge() - 1;
    }

    @Override
    public int getGrowthDuration(CropBlockEntity crop) {
        return crop.getCurrentAge() == getMaxAge() - 1 ? 600 : 400;
    }

    @Override
    public String[] getAttributes() {
        return attributes;
    }
}
