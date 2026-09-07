package ic2.neoforge.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import ic2.neoforge.registration.ModWorldContent;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacerType;

/** Classic layered crown and narrow top, registered through a MapCodec without global instances. */
public final class RubberFoliagePlacer extends FoliagePlacer {
    public static final MapCodec<RubberFoliagePlacer> CODEC =
            RecordCodecBuilder.mapCodec(
                    i -> foliagePlacerParts(i).apply(i, RubberFoliagePlacer::new));

    public RubberFoliagePlacer(IntProvider radius, IntProvider offset) {
        super(radius, offset);
    }

    @Override
    protected FoliagePlacerType<?> type() {
        return ModWorldContent.RUBBER_FOLIAGE.get();
    }

    @Override
    protected void createFoliage(
            WorldGenLevel level,
            FoliageSetter setter,
            RandomSource random,
            TreeConfiguration config,
            int trunkHeight,
            FoliageAttachment attachment,
            int foliageHeight,
            int radius,
            int offset) {
        int start = trunkHeight < 4 ? 0 : trunkHeight < 7 ? 2 : 3;
        BlockPos center = attachment.pos();
        var pos = new BlockPos.MutableBlockPos();
        for (int y = start; y < trunkHeight; y++)
            for (int x = -radius; x <= radius; x++)
                for (int z = -radius; z <= radius; z++) {
                    int chance = y + 4 - trunkHeight, dx = Math.abs(x), dz = Math.abs(z);
                    if (dx <= 1 && dz <= 1
                            || dx <= 1 && (chance <= 1 || random.nextInt(chance) == 0)
                            || dz <= 1 && (chance <= 1 || random.nextInt(chance) == 0)) {
                        pos.setWithOffset(center, x, y - trunkHeight, z);
                        tryPlaceLeaf(level, setter, random, config, pos);
                    }
                }
        for (int x = -1; x <= 1; x++)
            for (int z = -1; z <= 1; z++)
                if (x == 0 || z == 0) {
                    pos.setWithOffset(center, x, 0, z);
                    tryPlaceLeaf(level, setter, random, config, pos);
                }
        for (int y = 1; y <= foliageHeight; y++) {
            pos.setWithOffset(center, 0, y, 0);
            tryPlaceLeaf(level, setter, random, config, pos);
        }
    }

    @Override
    public int foliageHeight(RandomSource random, int trunkHeight, TreeConfiguration config) {
        return 1 + trunkHeight / 4 + random.nextInt(2);
    }

    @Override
    protected boolean shouldSkipLocation(
            RandomSource random, int x, int y, int z, int radius, boolean doubleTrunk) {
        return false;
    }
}
