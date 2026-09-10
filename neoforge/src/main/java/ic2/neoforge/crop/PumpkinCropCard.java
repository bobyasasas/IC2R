package ic2.neoforge.crop;

import ic2.core.crop.CropProperties;
import ic2.neoforge.registration.ModCrops;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The pumpkin stem crop (legacy CropPumpkin): tier one, gains whole pumpkins and seeds back one to
 * three pumpkin seeds at low stats.
 */
public class PumpkinCropCard extends VanillaStemCropCard {
    /** Legacy shares one global random (IC2.random); the seed/product rolls run outside the tile. */
    private static final RandomSource RANDOM = RandomSource.create();

    public PumpkinCropCard() {
        super(
                "pumpkin",
                ModCrops.PUMPKIN_CROP::get,
                3,
                new CropProperties(1, 0, 1, 0, 3, 1),
                () -> new ItemStack(Items.PUMPKIN),
                () -> new ItemStack(Items.PUMPKIN_SEEDS, RANDOM.nextInt(3) + 1),
                new String[] {"Orange", "Decoration", "Stem"});
    }

    @Override
    public int getGrowthDuration(CropBlockEntity crop) {
        return crop.getCurrentAge() == getMaxAge() - 1 ? 600 : 200;
    }
}
