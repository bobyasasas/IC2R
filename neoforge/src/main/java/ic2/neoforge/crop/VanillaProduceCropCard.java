package ic2.neoforge.crop;

import ic2.core.crop.CropProperties;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.function.Supplier;

/**
 * Legacy CropVanilla produce crops (carrots, beetroots): light-gated growth; the produce is the
 * gain and the plain seed drop at low stats (beetroots seed back beetroot seeds instead).
 */
public class VanillaProduceCropCard implements CropCard {
    private final String id;
    private final Supplier<Block> block;
    private final int maxAge;
    private final CropProperties properties;
    private final Supplier<ItemStack> produce;
    private final Supplier<ItemStack> seeds;

    public VanillaProduceCropCard(
            String id, Supplier<Block> block, int maxAge, CropProperties properties,
            Supplier<ItemStack> produce, Supplier<ItemStack> seeds) {
        this.id = id;
        this.block = block;
        this.maxAge = maxAge;
        this.properties = properties;
        this.produce = produce;
        this.seeds = seeds;
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
        return properties;
    }

    @Override
    public int getMaxAge() {
        return maxAge;
    }

    @Override
    public boolean canGrow(CropBlockEntity crop) {
        return crop.getCurrentAge() < getMaxAge() && crop.getLightLevel() >= 9;
    }

    @Override
    public List<ItemStack> getGains(CropBlockEntity crop) {
        return List.of(produce.get());
    }

    @Override
    public ItemStack getSeedsItem(CropBlockEntity crop) {
        if (crop.getStatGain() <= 1 && crop.getStatGrowth() <= 1
                && crop.getStatResistance() <= 1) {
            return seeds.get();
        }
        return crop.generateSeeds(this, crop.getStatGrowth(), crop.getStatGain(),
                crop.getStatResistance(), crop.getScanLevel());
    }
}
