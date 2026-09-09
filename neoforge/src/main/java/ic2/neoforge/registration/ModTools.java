package ic2.neoforge.registration;

import ic2.core.energy.ElectricItemSpec;
import ic2.neoforge.IndustrialCraft;
import ic2.neoforge.item.*;
import ic2.neoforge.item.ElectricTreetapItem;
import ic2.neoforge.item.TreetapItem;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

public final class ModTools {
    private static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(IndustrialCraft.MOD_ID);
    public static final TagKey<Block> WRENCH_TARGETS =
            TagKey.create(
                    Registries.BLOCK, Identifier.fromNamespaceAndPath("ic2", "mineable/wrench"));
    public static final DeferredItem<WrenchItem> WRENCH =
            ITEMS.registerItem("wrench", p -> new WrenchItem(tool(p.durability(120), 1)));
    public static final DeferredItem<ElectricWrenchItem> ELECTRIC_WRENCH =
            ITEMS.registerItem(
                    "electric_wrench", p -> new ElectricWrenchItem(tool(p.stacksTo(1), 0)));
    public static final DeferredItem<CraftingToolItem> FORGE_HAMMER =
            ITEMS.registerItem("forge_hammer", p -> new CraftingToolItem(p.durability(80)));
    public static final DeferredItem<CutterItem> CUTTER =
            ITEMS.registerItem("cutter", p -> new CutterItem(p.durability(60)));

    public static final DeferredItem<TreetapItem> TREETAP =
            ITEMS.registerItem("treetap", p -> new TreetapItem(p.durability(16)));
    public static final DeferredItem<ElectricTreetapItem> ELECTRIC_TREETAP =
            ITEMS.registerItem("electric_treetap", p -> new ElectricTreetapItem(p.stacksTo(1)));

    // Drill miner constants keep the legacy TileEntityMiner pacing: EU per tick, ticks per
    // block, then the per-harvest drill wear energy.
    public static final DeferredItem<DrillItem> DRILL =
            ITEMS.registerItem(
                    "drill",
                    p ->
                            new DrillItem(
                                    p.stacksTo(1),
                                    new ElectricItemSpec(30000, 100, 1, false),
                                    BlockTags.INCORRECT_FOR_IRON_TOOL,
                                    8.0F,
                                    50,
                                    50,
                                    6,
                                    200,
                                    0));
    public static final DeferredItem<DrillItem> DIAMOND_DRILL =
            ITEMS.registerItem(
                    "diamond_drill",
                    p ->
                            new DrillItem(
                                    p.stacksTo(1),
                                    new ElectricItemSpec(30000, 100, 1, false),
                                    BlockTags.INCORRECT_FOR_DIAMOND_TOOL,
                                    16.0F,
                                    80,
                                    80,
                                    20,
                                    50,
                                    0));
    public static final DeferredItem<DrillItem> IRIDIUM_DRILL =
            ITEMS.registerItem(
                    "iridium_drill",
                    p ->
                            new DrillItem(
                                    p.stacksTo(1),
                                    new ElectricItemSpec(300000, 1000, 3, false),
                                    BlockTags.INCORRECT_FOR_NETHERITE_TOOL,
                                    24.0F,
                                    800,
                                    800,
                                    200,
                                    20,
                                    3));
    public static final DeferredItem<ScannerItem> SCANNER =
            ITEMS.registerItem(
                    "scanner",
                    p ->
                            new ScannerItem(
                                    p.stacksTo(1),
                                    new ElectricItemSpec(100000, 128, 1, false),
                                    6,
                                    50));
    public static final DeferredItem<FrequencyTransmitterItem> FREQUENCY_TRANSMITTER =
            ITEMS.registerItem("frequency_transmitter", FrequencyTransmitterItem::new);
    public static final DeferredItem<ScannerItem> ADVANCED_SCANNER =
            ITEMS.registerItem(
                    "advanced_scanner",
                    p ->
                            new ScannerItem(
                                    p.stacksTo(1),
                                    new ElectricItemSpec(1000000, 512, 2, false),
                                    12,
                                    250));

    private static Item.Properties tool(Item.Properties properties, int damage) {
        var blocks =
                BuiltInRegistries.acquireBootstrapRegistrationLookup(BuiltInRegistries.BLOCK)
                        .getOrThrow(WRENCH_TARGETS);
        return properties.component(
                DataComponents.TOOL,
                new Tool(List.of(Tool.Rule.minesAndDrops(blocks, 6)), 1, damage, true));
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        bus.addListener(ModTools::creativeContents);
    }

    private static void creativeContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES)) {
            event.accept(WRENCH);
            event.accept(ELECTRIC_WRENCH);
            event.accept(FORGE_HAMMER);
            event.accept(CUTTER);
            event.accept(TREETAP);
            event.accept(ELECTRIC_TREETAP);
            event.accept(DRILL);
            event.accept(DIAMOND_DRILL);
            event.accept(IRIDIUM_DRILL);
            event.accept(SCANNER);
            event.accept(ADVANCED_SCANNER);
            event.accept(FREQUENCY_TRANSMITTER);
        }
    }

    private ModTools() {}
}
