package ic2.neoforge.registration;

import ic2.neoforge.IndustrialCraft;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
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
    public static final Map<String, DeferredBlock<Block>> MATERIALS = blocks();

    private static Map<String, DeferredBlock<Block>> blocks() {
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
        if (event.getTabKey().equals(CreativeModeTabs.BUILDING_BLOCKS))
            MATERIALS.values().forEach(event::accept);
    }

    private ModMaterialBlocks() {}
}
