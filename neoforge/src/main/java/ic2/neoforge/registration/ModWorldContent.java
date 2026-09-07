package ic2.neoforge.registration;

import ic2.neoforge.IndustrialCraft;
import ic2.neoforge.world.RubberFoliagePlacer;
import ic2.neoforge.world.RubberLeavesBlock;
import ic2.neoforge.world.RubberLogBlock;
import ic2.neoforge.world.RubberWoodBlock;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacerType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class ModWorldContent {
    private static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(IndustrialCraft.MOD_ID);
    private static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(IndustrialCraft.MOD_ID);
    private static final DeferredRegister<FoliagePlacerType<?>> FOLIAGE =
            DeferredRegister.create(Registries.FOLIAGE_PLACER_TYPE, IndustrialCraft.MOD_ID);
    public static final DeferredHolder<FoliagePlacerType<?>, FoliagePlacerType<RubberFoliagePlacer>>
            RUBBER_FOLIAGE =
                    FOLIAGE.register(
                            "rubber_tree",
                            () -> new FoliagePlacerType<>(RubberFoliagePlacer.CODEC));
    public static final ResourceKey<ConfiguredFeature<?, ?>> RUBBER_TREE =
            ResourceKey.create(
                    Registries.CONFIGURED_FEATURE,
                    Identifier.fromNamespaceAndPath("ic2", "rubber_tree"));
    public static final Map<String, DeferredBlock<Block>> ORES = ores();
    public static final DeferredBlock<RubberLogBlock> RUBBER_LOG =
            BLOCKS.registerBlock("rubber_log", RubberLogBlock::new, () -> wood().randomTicks());
    public static final DeferredBlock<RotatedPillarBlock> STRIPPED_RUBBER_LOG =
            BLOCKS.registerBlock(
                    "stripped_rubber_log", RotatedPillarBlock::new, ModWorldContent::wood);
    public static final DeferredBlock<RubberWoodBlock> RUBBER_WOOD =
            BLOCKS.registerBlock("rubber_wood", RubberWoodBlock::new, ModWorldContent::wood);
    public static final DeferredBlock<Block> STRIPPED_RUBBER_WOOD =
            BLOCKS.registerBlock("stripped_rubber_wood", Block::new, ModWorldContent::wood);
    public static final DeferredBlock<Block> RUBBER_PLANKS =
            BLOCKS.registerBlock("rubber_planks", Block::new, ModWorldContent::wood);
    public static final DeferredBlock<RubberLeavesBlock> RUBBER_LEAVES =
            BLOCKS.registerBlock(
                    "rubber_leaves",
                    RubberLeavesBlock::new,
                    () ->
                            BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LEAVES)
                                    .sound(SoundType.GRASS));
    public static final DeferredBlock<SaplingBlock> RUBBER_SAPLING =
            BLOCKS.registerBlock(
                    "rubber_sapling",
                    p ->
                            new SaplingBlock(
                                    new TreeGrower(
                                            "ic2:rubber",
                                            Optional.empty(),
                                            Optional.of(RUBBER_TREE),
                                            Optional.empty()),
                                    p),
                    () ->
                            BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SAPLING)
                                    .sound(SoundType.GRASS));

    private static BlockBehaviour.Properties wood() {
        return BlockBehaviour.Properties.of()
                .strength(2, 3)
                .sound(SoundType.WOOD)
                .mapColor(MapColor.PODZOL);
    }

    private static Map<String, DeferredBlock<Block>> ores() {
        var result = new LinkedHashMap<String, DeferredBlock<Block>>();
        for (int index = 0; index < 3; index++)
            for (boolean deep : new boolean[] {false, true}) {
                String name =
                        (deep ? "deepslate_" : "")
                                + new String[] {"lead", "tin", "uranium"}[index]
                                + "_ore";
                int hardness = index + (deep ? 3 : 2), resistance = index + (deep ? 6 : 4);
                result.put(
                        name,
                        BLOCKS.registerSimpleBlock(
                                name,
                                p ->
                                        p.strength(hardness, resistance)
                                                .requiresCorrectToolForDrops()
                                                .mapColor(deep ? MapColor.DEEPSLATE : MapColor.NONE)
                                                .sound(
                                                        deep
                                                                ? SoundType.DEEPSLATE
                                                                : SoundType.STONE)));
            }
        return Collections.unmodifiableMap(result);
    }

    public static void register(IEventBus bus) {
        BLOCKS.getEntries().forEach(holder -> ITEMS.registerSimpleBlockItem(holder));
        BLOCKS.register(bus);
        ITEMS.register(bus);
        FOLIAGE.register(bus);
        bus.addListener(ModWorldContent::creativeContents);
    }

    private static void creativeContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.NATURAL_BLOCKS))
            BLOCKS.getEntries().forEach(holder -> event.accept(holder.get()));
    }

    private ModWorldContent() {}
}
