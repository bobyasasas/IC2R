package ic2.neoforge.registration;

import ic2.neoforge.IndustrialCraft;
import ic2.neoforge.block.ReinforcedGlassBlock;
import ic2.neoforge.block.SheetBlock;
import ic2.neoforge.world.MiningPipeBlock;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Non-ticking material blocks and casings. Ore generation remains a separate subsystem. */
public final class ModMaterialBlocks {
    private static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(IndustrialCraft.MOD_ID);
    private static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(IndustrialCraft.MOD_ID);
    public static final DeferredBlock<Block> IRON_FENCE =
            BLOCKS.registerBlock(
                    "iron_fence",
                    properties ->
                            new ic2.neoforge.world.IronFenceBlock(
                                    properties.strength(5.0F, 10.0F).sound(SoundType.METAL)));
    public static final DeferredBlock<ReinforcedGlassBlock> REINFORCED_GLASS =
            BLOCKS.registerBlock(
                    "reinforced_glass",
                    properties ->
                            new ReinforcedGlassBlock(
                                    properties
                                            .noOcclusion()
                                            .strength(5.0F, 180.0F)
                                            .sound(SoundType.GLASS)
                                            .isValidSpawn((state, world, pos, type) -> false)));
    public static final DeferredBlock<MiningPipeBlock> MINING_PIPE =
            BLOCKS.registerBlock(
                    "mining_pipe",
                    properties ->
                            new MiningPipeBlock(
                                    properties
                                            .strength(6.0F, 10.0F)
                                            .requiresCorrectToolForDrops()
                                            .sound(SoundType.METAL)));
    // The tip keeps no item form, matching legacy: only the miner places it.
    public static final DeferredBlock<Block> MINING_PIPE_TIP =
            BLOCKS.registerBlock(
                    "mining_pipe_tip",
                    properties ->
                            new Block(
                                    properties
                                            .strength(6.0F, 10.0F)
                                            .requiresCorrectToolForDrops()
                                            .sound(SoundType.METAL)));
    // Sheets keep the bare legacy properties (no sound/mapColor overrides) for a 1:1 port.
    public static final DeferredBlock<SheetBlock> RESIN_SHEET =
            BLOCKS.registerBlock(
                    "resin_sheet",
                    properties -> new SheetBlock(properties.strength(1.6F, 0.5F)));
    public static final DeferredBlock<SheetBlock> RUBBER_SHEET =
            BLOCKS.registerBlock(
                    "rubber_sheet",
                    properties -> new SheetBlock(properties.strength(0.8F, 2.0F)));
    public static final DeferredBlock<SheetBlock> WOOL_SHEET =
            BLOCKS.registerBlock(
                    "wool_sheet",
                    properties -> new SheetBlock(properties.strength(0.8F, 0.8F)));
    // Bare legacy BlockRefractoryBricks properties, no tool binding beyond correct-tool drops.
    public static final DeferredBlock<Block> REFRACTORY_BRICKS =
            BLOCKS.registerBlock(
                    "refractory_bricks",
                    properties ->
                            new Block(
                                    properties
                                            .strength(2.0F, 10.0F)
                                            .requiresCorrectToolForDrops()
                                            .sound(SoundType.STONE)));
    // Legacy uses the vanilla iron DoorBlock set; the anonymous subclass carries no overrides.
    public static final DeferredBlock<DoorBlock> REINFORCED_DOOR =
            BLOCKS.registerBlock(
                    "reinforced_door",
                    properties ->
                            new DoorBlock(
                                    BlockSetType.IRON,
                                    properties.strength(50.0F, 150.0F).sound(SoundType.METAL)) {});
    public static final Map<String, DeferredBlock<Block>> MATERIALS = blocks();

    private static Map<String, DeferredBlock<Block>> blocks() {
        ITEMS.registerSimpleBlockItem(IRON_FENCE);
        ITEMS.registerSimpleBlockItem(MINING_PIPE);
        ITEMS.registerSimpleBlockItem(REINFORCED_GLASS);
        ITEMS.registerSimpleBlockItem(RESIN_SHEET);
        ITEMS.registerSimpleBlockItem(RUBBER_SHEET);
        ITEMS.registerSimpleBlockItem(WOOL_SHEET);
        ITEMS.registerSimpleBlockItem(REFRACTORY_BRICKS);
        ITEMS.registerSimpleBlockItem(REINFORCED_DOOR);
        var result = new LinkedHashMap<String, DeferredBlock<Block>>();
        add(result, "bronze_block", 5, 10, SoundType.METAL, MapColor.NONE);
        add(result, "lead_block", 4, 10, SoundType.METAL, MapColor.NONE);
        add(result, "steel_block", 8, 10, SoundType.METAL, MapColor.NONE);
        add(result, "tin_block", 4, 10, SoundType.METAL, MapColor.NONE);
        add(result, "uranium_block", 6, 10, SoundType.METAL, MapColor.NONE);
        add(result, "silver_block", 4, 10, SoundType.METAL, MapColor.NONE);
        add(result, "machine", 5, 10, SoundType.METAL, MapColor.NONE);
        add(result, "advanced_machine", 8, 10, SoundType.METAL, MapColor.NONE);
        add(result, "raw_lead_block", 3, 11, SoundType.STONE, MapColor.COLOR_LIGHT_GRAY);
        add(result, "raw_tin_block", 4, 12, SoundType.STONE, MapColor.SNOW);
        add(result, "raw_uranium_block", 5, 13, SoundType.STONE, MapColor.COLOR_GREEN);
        add(result, "reinforced_stone", 80, 180, SoundType.STONE, MapColor.NONE);
        add(result, "reactor_vessel", 40, 90, SoundType.STONE, MapColor.NONE);
        return Collections.unmodifiableMap(result);
    }

    private static void add(
            Map<String, DeferredBlock<Block>> result,
            String id,
            float hardness,
            float resistance,
            SoundType sound,
            MapColor color) {
        var block =
                BLOCKS.registerSimpleBlock(
                        id,
                        properties ->
                                properties
                                        .mapColor(color)
                                        .strength(hardness, resistance)
                                        .sound(sound)
                                        .requiresCorrectToolForDrops());
        ITEMS.registerSimpleBlockItem(block);
        result.put(id, block);
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        bus.addListener(ModMaterialBlocks::creativeContents);
    }

    private static void creativeContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.BUILDING_BLOCKS)) {
            MATERIALS.values().forEach(event::accept);
            event.accept(IRON_FENCE);
            event.accept(REINFORCED_GLASS);
            event.accept(RESIN_SHEET);
            event.accept(RUBBER_SHEET);
            event.accept(WOOL_SHEET);
            event.accept(REFRACTORY_BRICKS);
        } else if (event.getTabKey().equals(CreativeModeTabs.INGREDIENTS)) {
            event.accept(MINING_PIPE);
            event.accept(REINFORCED_DOOR);
        }
    }

    private ModMaterialBlocks() {}
}
