package ic2.neoforge.crop;

import ic2.core.crop.CropProperties;
import ic2.neoforge.registration.ModCrops;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The melon stem crop (legacy CropMelon): a third of the harvests yield a whole melon, the rest
 * two to five slices.
 */
public class MelonCropCard extends VanillaStemCropCard {
    /** Legacy shares one global random (IC2.random); the seed/product rolls run outside the tile. */
    private static final RandomSource RANDOM = RandomSource.create();

    public MelonCropCard() {
        super(
                "melon",
                ModCrops.MELON_CROP::get,
                3,
                new CropProperties(2, 0, 4, 0, 2, 0),
                MelonCropCard::randomMelon,
                () -> new ItemStack(Items.MELON_SEEDS, RANDOM.nextInt(2) + 1),
                new String[] {"Green", "Food", "Stem"});
    }

    private static ItemStack randomMelon() {
        if (RANDOM.nextInt(3) == 0) return new ItemStack(Items.MELON);
        return new ItemStack(Items.MELON_SLICE, RANDOM.nextInt(4) + 2);
    }

    @Override
    public int getGrowthDuration(CropBlockEntity crop) {
        return crop.getCurrentAge() == getMaxAge() - 1 ? 700 : 250;
    }
}
