package ic2.neoforge.crop;

import ic2.core.crop.CropProperties;
import ic2.neoforge.registration.ModCrops;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

/**
 * The terra wart crop (legacy CropTerraWart): snow under the roots fuels growth, soul sand slowly
 * transmutes it back into a nether wart, and the warts themselves cure ailments.
 */
public class TerraWartCropCard implements CropCard {
    @Override
    public String getId() {
        return "terra_wart";
    }

    @Override
    public Block getCropBlock() {
        return ModCrops.TERRA_WART_CROP.get();
    }

    @Override
    public CropProperties getProperties() {
        return new CropProperties(5, 2, 4, 0, 3, 0);
    }

    @Override
    public int getMaxAge() {
        return 2;
    }

    @Override
    public double dropGainChance() {
        return 0.8;
    }

    @Override
    public void tick(CropBlockEntity crop) {
        if (crop.isBlockBelow(Blocks.SNOW)) {
            if (canGrow(crop)) crop.setGrowthPoints(crop.getGrowthPoints() + 100);
        } else if (crop.isBlockBelow(Blocks.SOUL_SAND)
                && crop.getLevel() instanceof ServerLevel level
                && level.getRandom().nextInt(300) == 0) {
            crop.setCrop(level, ModCrops.NETHER_WART_CARD);
        }
    }

    @Override
    public List<ItemStack> getGains(CropBlockEntity crop) {
        return List.of(new ItemStack(ModCrops.TERRA_WART.get()));
    }

    @Override
    public int getRootsLength(CropBlockEntity crop) {
        return 5;
    }

    @Override
    public String[] getAttributes() {
        return new String[] { "Blue", "Aether", "Consumable", "Snow" };
    }
}
