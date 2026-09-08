package ic2.neoforge.machine;

import ic2.core.machine.RotorMaterial;
import ic2.core.machine.RotorOperation;
import ic2.core.machine.WaterFlow;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;

public final class WaterTurbineBlockEntity extends TurbineBlockEntity {
    private record Shore(int distance, boolean forward) {}

    public WaterTurbineBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state);
    }

    @Override
    protected int sampleInterval() {
        return 20;
    }

    @Override
    protected boolean acceptsRotor(RotorMaterial material) {
        return material.supportsWater();
    }

    @Override
    protected RotorOperation sample(
            ServerLevel level, RotorMaterial material, Direction facing, RotorSpace space) {
        double multiplier = GenerationConfig.WATER_KINETIC.get();
        if (multiplier == 0)
            return RotorOperation.stopped(material.diameter(), RotorOperation.Status.DISABLED);
        var body = body(level.getBiome(worldPosition));
        int diameter = body.diameter(material);
        if (body == WaterFlow.Body.INVALID)
            return RotorOperation.stopped(diameter, RotorOperation.Status.INVALID_BIOME);
        int plane = space.obstructions(worldPosition, facing, diameter, true, true, kind());
        if (plane != 0)
            return new RotorOperation(0, diameter, 0, RotorOperation.Status.NO_SPACE, 0, plane, 0);
        int obstructions = space.obstructions(worldPosition, facing, diameter, false, true, kind());
        var shore = shore(level, facing);
        boolean reverse =
                body == WaterFlow.Body.RIVER
                        ? facing == Direction.EAST || facing == Direction.NORTH
                        : shore.forward();
        return new WaterFlow(
                        body,
                        level.getDefaultClockTime(),
                        shore.distance(),
                        reverse,
                        obstructions,
                        body == WaterFlow.Body.RIVER ? turbulence(level) : 0)
                .operation(material, multiplier);
    }

    private float turbulence(ServerLevel level) {
        // A stable sample epoch prevents reloads or rotor swaps from rerolling this interval's
        // output. Xoroshiro mixes nearby epochs without advancing the world's shared RNG.
        long phase = Math.floorMod(worldPosition.hashCode(), sampleInterval());
        long epoch = Math.floorDiv(level.getGameTime() - phase, sampleInterval());
        return new XoroshiroRandomSource(level.getSeed() ^ worldPosition.asLong() ^ epoch)
                .nextFloat();
    }

    private static WaterFlow.Body body(Holder<Biome> biome) {
        // Deep oceans also belong to IS_OCEAN, so the more specific tag must be checked first.
        if (biome.is(BiomeTags.IS_DEEP_OCEAN)) return WaterFlow.Body.DEEP_OCEAN;
        if (biome.is(BiomeTags.IS_OCEAN)) return WaterFlow.Body.OCEAN;
        return biome.is(BiomeTags.IS_RIVER) ? WaterFlow.Body.RIVER : WaterFlow.Body.INVALID;
    }

    private Shore shore(ServerLevel level, Direction facing) {
        // Native biome lookup requests BIOMES with loadOrGenerate=false and falls back to the
        // biome source for absent chunks. Shore sampling never generates terrain or adds tickets.
        for (int distance = 1; distance < 200; distance++) {
            if (body(level.getBiome(worldPosition.relative(facing, distance)))
                    == WaterFlow.Body.INVALID) return new Shore(distance, true);
            if (body(level.getBiome(worldPosition.relative(facing, -distance)))
                    == WaterFlow.Body.INVALID) return new Shore(distance, false);
        }
        return new Shore(200, true);
    }
}
