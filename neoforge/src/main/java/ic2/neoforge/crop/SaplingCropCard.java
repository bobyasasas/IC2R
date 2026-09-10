package ic2.neoforge.crop;

import ic2.core.crop.CropProperties;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Legacy CropBaseSapling: tree saplings yield their log, a chance at the sapling back and, for
 * oak, a chance at an apple.
 */
public class SaplingCropCard implements CropCard {
    private final String id;
    private final Supplier<Block> block;
    private final Supplier<ItemStack> log;
    private final Supplier<ItemStack> sapling;
    private final boolean oak;

    public SaplingCropCard(
            String id, Supplier<Block> block, Supplier<ItemStack> log,
            Supplier<ItemStack> sapling, boolean oak) {
        this.id = id;
        this.block = block;
        this.log = log;
        this.sapling = sapling;
        this.oak = oak;
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
        return new CropProperties(3, 1, 0, 4, 4, 0);
    }

    @Override
    public int getMaxAge() {
        return 4;
    }

    @Override
    public boolean canGrow(CropBlockEntity crop) {
        return crop.getCurrentAge() < getMaxAge() && crop.getLightLevel() >= 9;
    }

    @Override
    public List<ItemStack> getGains(CropBlockEntity crop) {
        List<ItemStack> drops = new ArrayList<>();
        drops.add(log.get());
        // The level RNG mirrors the shared legacy IC2.random rolls (25% sapling, oak apple).
        if (crop.getLevel() != null) {
            var random = crop.getLevel().getRandom();
            if (random.nextInt(100) >= 75) drops.add(sapling.get());
            if (oak && random.nextInt(100) >= 75) {
                drops.add(new ItemStack(net.minecraft.world.item.Items.APPLE));
            }
        }
        return drops;
    }

    @Override
    public int getGrowthDuration(CropBlockEntity crop) {
        return crop.getCurrentAge() >= getMaxAge() - 1 ? 150 : 600;
    }

    @Override
    public int getAgeAfterHarvest(CropBlockEntity crop) {
        return getMaxAge() - 1;
    }

    @Override
    public String[] getAttributes() {
        return new String[] { "Leaves", "Sapling", "Green" };
    }
}
