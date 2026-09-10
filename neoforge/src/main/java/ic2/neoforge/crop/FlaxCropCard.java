package ic2.neoforge.crop;

import ic2.core.crop.CropProperties;
import ic2.neoforge.registration.ModCrops;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

import java.util.List;

/** The flax crop (legacy CropFlax): a silk crop dropping string. */
public class FlaxCropCard implements CropCard {
    @Override
    public String getId() {
        return "flax";
    }

    @Override
    public Block getCropBlock() {
        return ModCrops.FLAX_CROP.get();
    }

    @Override
    public CropProperties getProperties() {
        return new CropProperties(2, 1, 1, 2, 0, 1);
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
    public List<ItemStack> getGains(CropBlockEntity crop) {
        return List.of(new ItemStack(Items.STRING));
    }

    @Override
    public String[] getAttributes() {
        return new String[] { "Silk", "Vine", "Addictive" };
    }
}
