package ic2.neoforge.crop;

import ic2.core.crop.CropProperties;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.function.Supplier;

/** Legacy CropBaseMushroom: a fast mushroom crop dropping its own mushroom. */
public class MushroomCropCard implements CropCard {
    private final String id;
    private final Supplier<Block> block;
    private final Supplier<ItemStack> drop;

    public MushroomCropCard(String id, Supplier<Block> block, Supplier<ItemStack> drop) {
        this.id = id;
        this.block = block;
        this.drop = drop;
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
        return new CropProperties(2, 0, 4, 0, 0, 4);
    }

    @Override
    public int getMaxAge() {
        return 2;
    }

    @Override
    public int getGrowthDuration(CropBlockEntity crop) {
        return 200;
    }

    @Override
    public List<ItemStack> getGains(CropBlockEntity crop) {
        return List.of(drop.get());
    }
}
