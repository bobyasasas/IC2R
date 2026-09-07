package ic2.core.world;

import ic2.core.IC2;
import ic2.core.block.misc.RubberLogBlock;
import ic2.core.block.misc.RubberLogBlock.RubberWoodState;
import ic2.core.proxy.EnvProxy.BiomeSelector;
import ic2.core.ref.Ic2Blocks;

import net.minecraft.core.Holder;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.GenerationStep.Decoration;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration.TreeConfigurationBuilder;
import net.minecraft.world.level.levelgen.feature.featuresize.TwoLayersFeatureSize;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.WeightedStateProvider;
import net.minecraft.world.level.levelgen.feature.trunkplacers.StraightTrunkPlacer;

import java.util.concurrent.CompletableFuture;

public class Ic2WorldGen {
    private static final WeightedStateProvider RUBBER_LOG_PROVIDER =
            new WeightedStateProvider(
                    SimpleWeightedRandomList.<BlockState>builder()
                            .add(Ic2Blocks.RUBBER_LOG.defaultBlockState(), 16)
                            .add(
                                    Ic2Blocks.RUBBER_LOG
                                            .defaultBlockState()
                                            .setValue(
                                                    RubberLogBlock.stateProperty,
                                                    RubberWoodState.wet_north),
                                    1)
                            .add(
                                    Ic2Blocks.RUBBER_LOG
                                            .defaultBlockState()
                                            .setValue(
                                                    RubberLogBlock.stateProperty,
                                                    RubberWoodState.wet_east),
                                    1)
                            .add(
                                    Ic2Blocks.RUBBER_LOG
                                            .defaultBlockState()
                                            .setValue(
                                                    RubberLogBlock.stateProperty,
                                                    RubberWoodState.wet_south),
                                    1)
                            .add(
                                    Ic2Blocks.RUBBER_LOG
                                            .defaultBlockState()
                                            .setValue(
                                                    RubberLogBlock.stateProperty,
                                                    RubberWoodState.wet_west),
                                    1));
    public static final CompletableFuture<Holder<ConfiguredFeature<TreeConfiguration, ?>>>
            RUBBER_TREE =
                    register(
                            new TreeConfigurationBuilder(
                                            RUBBER_LOG_PROVIDER,
                                            new StraightTrunkPlacer(4, 4, 0),
                                            BlockStateProvider.simple(Ic2Blocks.RUBBER_LEAVES),
                                            RubberTreeFoliagePlacer.INSTANCE,
                                            new TwoLayersFeatureSize(1, 0, 1))
                                    .ignoreVines()
                                    .build());

    public static void init() {
        attachOreFeatureToBiome("lead_ore");
        attachOreFeatureToBiome("lead_ore_lower");
        attachOreFeatureToBiome("tin_ore_small");
        attachOreFeatureToBiome("tin_ore_upper");
        attachOreFeatureToBiome("uranium_ore");
        attachOreFeatureToBiome("uranium_ore_buried");
        attachOreFeatureToBiome("uranium_ore_large");
        attachRubberTreeFeatureToBiome("trees_rubber_jungle", BiomeSelector.JUNGLE);
        attachRubberTreeFeatureToBiome("trees_rubber_forest", BiomeSelector.FOREST);
        attachRubberTreeFeatureToBiome("trees_rubber_swamp", BiomeSelector.SWAMP);
        RubberTreeFoliagePlacer.init();
    }

    private static void attachOreFeatureToBiome(String id) {
        IC2.envProxy.attachPlacedFeatureToBiome(
                IC2.getIdentifier(id), BiomeSelector.OVERWORLD, Decoration.UNDERGROUND_ORES);
    }

    private static void attachRubberTreeFeatureToBiome(String id, BiomeSelector selector) {
        IC2.envProxy.attachPlacedFeatureToBiome(
                IC2.getIdentifier(id), selector, Decoration.VEGETAL_DECORATION);
    }

    private static <FC extends FeatureConfiguration, F extends Feature<FC>>
            CompletableFuture<Holder<ConfiguredFeature<FC, ?>>> register(FC config) {
        return IC2.envProxy.registerConfiguredFeature(
                IC2.getIdentifier("rubber_tree"), (F) Feature.TREE, config);
    }
}
