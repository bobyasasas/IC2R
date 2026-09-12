package ic2.neoforge.registration;

import ic2.core.energy.ElectricItemSpec;
import ic2.neoforge.IndustrialCraft;
import ic2.neoforge.item.*;
import ic2.neoforge.item.ElectricTreetapItem;
import ic2.neoforge.item.TreetapItem;
import ic2.neoforge.menu.CropAnalyzerMenu;
import ic2.neoforge.menu.MiningFilterMenu;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class ModTools {
    private static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(IndustrialCraft.MOD_ID);
    public static final TagKey<Block> WRENCH_TARGETS =
            TagKey.create(
                    Registries.BLOCK, Identifier.fromNamespaceAndPath("ic2", "mineable/wrench"));
    private static final TagKey<Item> TOOL_REPAIRS =
            ItemTags.create(
                    Identifier.fromNamespaceAndPath(IndustrialCraft.MOD_ID, "repairs_bronze_tool"));
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

    /** Legacy Ic2ToolMaterials.BRONZE: level 2, 350 uses, 6.0 speed, 2.0 damage bonus, enchantability 14. */
    public static final ToolMaterial BRONZE_TOOL_MATERIAL = new ToolMaterial(
            BlockTags.INCORRECT_FOR_IRON_TOOL, 350, 6.0F, 2.0F, 14, TOOL_REPAIRS);
    public static final DeferredItem<Item> BRONZE_PICKAXE =
            ITEMS.registerItem(
                    "bronze_pickaxe", p -> new Item(p.pickaxe(BRONZE_TOOL_MATERIAL, 1.0F, -2.8F)));
    public static final DeferredItem<Item> BRONZE_AXE =
            ITEMS.registerItem(
                    "bronze_axe", p -> new AxeItem(BRONZE_TOOL_MATERIAL, 6.0F, -3.1F, p));
    public static final DeferredItem<Item> BRONZE_SHOVEL =
            ITEMS.registerItem(
                    "bronze_shovel", p -> new ShovelItem(BRONZE_TOOL_MATERIAL, 1.5F, -3.0F, p));
    public static final DeferredItem<Item> BRONZE_HOE =
            ITEMS.registerItem(
                    "bronze_hoe", p -> new HoeItem(BRONZE_TOOL_MATERIAL, -2.0F, -1.0F, p));
    public static final DeferredItem<Item> BRONZE_SWORD =
            ITEMS.registerItem(
                    "bronze_sword", p -> new Item(p.sword(BRONZE_TOOL_MATERIAL, 3.0F, -2.4F)));

    public static final DeferredItem<PainterItem> PAINTER =
            ITEMS.registerItem("painter", p -> new PainterItem(p.durability(32), null));
    private static final Map<DyeColor, DeferredItem<PainterItem>> PAINTERS =
            new EnumMap<>(DyeColor.class);

    static {
        for (DyeColor color : DyeColor.values()) {
            PAINTERS.put(
                    color,
                    ITEMS.registerItem(
                            color.getName() + "_painter",
                            p -> new PainterItem(p.durability(32), color)));
        }
    }

    public static DeferredItem<PainterItem> painter(DyeColor color) {
        return PAINTERS.get(color);
    }

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
    public static final DeferredItem<ChainsawItem> CHAINSAW =
            ITEMS.registerItem(
                    "chainsaw",
                    p -> new ChainsawItem(p.stacksTo(1), new ElectricItemSpec(30000, 100, 1, false)));
    public static final DeferredItem<MiningLaserItem> MINING_LASER =
            ITEMS.registerItem(
                    "mining_laser",
                    p ->
                            new MiningLaserItem(
                                    p.stacksTo(1).rarity(Rarity.UNCOMMON),
                                    new ElectricItemSpec(300000, 512, 3, false)));
    public static final DeferredItem<NanoSaberItem> NANO_SABER =
            ITEMS.registerItem(
                    "nano_saber",
                    p ->
                            new NanoSaberItem(
                                    p.stacksTo(1).rarity(Rarity.UNCOMMON),
                                    new ElectricItemSpec(160000, 500, 3, false)));
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
    public static final DeferredItem<CropAnalyzerItem> CROP_ANALYZER =
            ITEMS.registerItem(
                    "crop_analyzer",
                    p ->
                            new CropAnalyzerItem(
                                    p.stacksTo(1).rarity(Rarity.UNCOMMON),
                                    new ElectricItemSpec(100000, 128, 2, false)));
    public static final DeferredItem<ScannerItem> ADVANCED_SCANNER =
            ITEMS.registerItem(
                    "advanced_scanner",
                    p ->
                            new ScannerItem(
                                    p.stacksTo(1),
                                    new ElectricItemSpec(1000000, 512, 2, false),
                                    12,
                                    250));
    public static final DeferredItem<WindMeterItem> WIND_METER =
            ITEMS.registerItem("wind_meter", p -> new WindMeterItem(p.stacksTo(1)));
    public static final DeferredItem<MiningFilterCardItem> MINING_FILTER_CARD =
            ITEMS.registerItem("mining_filter_card", p -> new MiningFilterCardItem(p.stacksTo(1)));
    public static final DeferredItem<CuttingBladeItem> IRON_CUTTING_BLADE =
            ITEMS.registerItem(
                    "iron_cutting_blade",
                    p -> new CuttingBladeItem(p.stacksTo(1), 3, "ic2.iron_cutting_blade.info"));
    public static final DeferredItem<CuttingBladeItem> STEEL_CUTTING_BLADE =
            ITEMS.registerItem(
                    "steel_cutting_blade",
                    p -> new CuttingBladeItem(p.stacksTo(1), 6, "ic2.steel_cutting_blade.info"));
    public static final DeferredItem<CuttingBladeItem> DIAMOND_CUTTING_BLADE =
            ITEMS.registerItem(
                    "diamond_cutting_blade",
                    p -> new CuttingBladeItem(p.stacksTo(1), 9, "ic2.diamond_cutting_blade.info"));

    private static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, IndustrialCraft.MOD_ID);
    public static final DeferredHolder<MenuType<?>, MenuType<MiningFilterMenu>> MINING_FILTER_MENU =
            MENUS.register(
                    "mining_filter",
                    () ->
                            IMenuTypeExtension.create(
                                    (id, inventory, data) ->
                                            new MiningFilterMenu(
                                                    id, inventory, data.readVarInt(), true)));
    public static final DeferredHolder<MenuType<?>, MenuType<CropAnalyzerMenu>> CROP_ANALYZER_MENU =
            MENUS.register(
                    "crop_analyzer",
                    () ->
                            IMenuTypeExtension.create(
                                    (id, inventory, data) ->
                                            new CropAnalyzerMenu(
                                                    id, inventory, data.readVarInt(), true)));

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
        MENUS.register(bus);
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
            event.accept(BRONZE_PICKAXE);
            event.accept(BRONZE_AXE);
            event.accept(BRONZE_SHOVEL);
            event.accept(BRONZE_HOE);
            event.accept(DRILL);
            event.accept(DIAMOND_DRILL);
            event.accept(IRIDIUM_DRILL);
            event.accept(CHAINSAW);
            event.accept(MINING_LASER);
            event.accept(NANO_SABER);
            event.accept(SCANNER);
            event.accept(ADVANCED_SCANNER);
            event.accept(FREQUENCY_TRANSMITTER);
            event.accept(WIND_METER);
            event.accept(CROP_ANALYZER);
            event.accept(MINING_FILTER_CARD);
            event.accept(PAINTER);
            for (DyeColor color : DyeColor.values()) {
                event.accept(PAINTERS.get(color));
            }
            event.accept(IRON_CUTTING_BLADE);
            event.accept(STEEL_CUTTING_BLADE);
            event.accept(DIAMOND_CUTTING_BLADE);
            event.accept(ModItems.BLANK_TFBP);
            event.accept(ModItems.CULTIVATION_TFBP);
            event.accept(ModItems.DESERTIFICATION_TFBP);
            event.accept(ModItems.FLATIFICATION_TFBP);
            event.accept(ModItems.CHILLING_TFBP);
            event.accept(ModItems.IRRIGATION_TFBP);
            event.accept(ModItems.MUSHROOM_TFBP);
            event.accept(ModItems.BLANK_TFBP);
            event.accept(ModItems.CHILLING_TFBP);
            event.accept(ModItems.CULTIVATION_TFBP);
            event.accept(ModItems.DESERTIFICATION_TFBP);
            event.accept(ModItems.FLATIFICATION_TFBP);
            event.accept(ModItems.IRRIGATION_TFBP);
            event.accept(ModItems.MUSHROOM_TFBP);
        }
        if (event.getTabKey().equals(CreativeModeTabs.COMBAT)) {
            event.accept(BRONZE_SWORD);
        }
    }

    private ModTools() {}
}
