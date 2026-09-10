package ic2.neoforge.crop;

import ic2.core.crop.CropProperties;
import ic2.neoforge.registration.ModCrops;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

/** The nether wart crop (legacy CropNetherWart): soul sand fuels growth, snow transmutes it. */
public class NetherWartCropCard implements CropCard {
    @Override
    public String getId() {
        return "nether_wart";
    }

    @Override
    public Block getCropBlock() {
        return ModCrops.NETHER_WART_CROP.get();
    }

    @Override
    public CropProperties getProperties() {
        return new CropProperties(5, 4, 2, 0, 2, 1);
    }

    @Override
    public int getMaxAge() {
        return 2;
    }

    @Override
    public double dropGainChance() {
        return 2.0;
    }

    @Override
    public void tick(CropBlockEntity crop) {
        if (crop.isBlockBelow(Blocks.SOUL_SAND)) {
            if (canGrow(crop)) crop.setGrowthPoints(crop.getGrowthPoints() + 100);
        } else if (crop.isBlockBelow(Blocks.SNOW)
                && crop.getLevel() instanceof ServerLevel level
                && level.getRandom().nextInt(300) == 0) {
            crop.setCrop(level, ModCrops.TERRA_WART_CARD);
        }
    }

    @Override
    public List<ItemStack> getGains(CropBlockEntity crop) {
        return List.of(new ItemStack(Items.NETHER_WART));
    }

    @Override
    public int getRootsLength(CropBlockEntity crop) {
        return 5;
    }

    @Override
    public String[] getAttributes() {
        return new String[] { "Red", "Nether", "Ingredient", "Soulsand" };
    }
}
