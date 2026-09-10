package ic2.neoforge.registration;

import ic2.neoforge.IndustrialCraft;
import ic2.neoforge.fluid.FoamTankHandler;
import ic2.neoforge.item.CFPackItem;
import ic2.neoforge.item.FoamSprayerItem;
import ic2.neoforge.world.FoamBlock;

import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Construction foam: the foam block, foam walls, the sprayer and the wearable foam pack. */
public final class ModFoam {
    private static final List<String> WALL_COLORS =
            List.of(
                    "white",
                    "orange",
                    "magenta",
                    "light_blue",
                    "yellow",
                    "lime",
                    "pink",
                    "gray",
                    "light_gray",
                    "cyan",
                    "purple",
                    "blue",
                    "brown",
                    "green",
                    "red",
                    "black");

    // Legacy Ic2ArmorMaterials.CF_PACK: eight chest armor, zero durability loss, iron equip sound.
    // 26.1.2 rejects zero enchantable/durability components, so the pack item is assembled by hand:
    // no MAX_DAMAGE and no ENCHANTABLE component reproduces the legacy zero values exactly.
    private static final TagKey<Item> NO_REPAIR =
            ItemTags.create(
                    Identifier.fromNamespaceAndPath(IndustrialCraft.MOD_ID, "foam_pack_no_repair"));
    private static final ArmorMaterial CF_PACK_MATERIAL =
            new ArmorMaterial(
                    0,
                    Map.of(ArmorType.CHESTPLATE, 8),
                    0,
                    SoundEvents.ARMOR_EQUIP_IRON,
                    2.0F,
                    0.0F,
                    NO_REPAIR,
                    ResourceKey.create(
                            EquipmentAssets.ROOT_ID,
                            Identifier.fromNamespaceAndPath(IndustrialCraft.MOD_ID, "cf_pack")));

    private static Item.Properties cfPackProperties() {
        return new Item.Properties()
                .attributes(CF_PACK_MATERIAL.createAttributes(ArmorType.CHESTPLATE))
                .component(
                        DataComponents.EQUIPPABLE,
                        Equippable.builder(EquipmentSlot.CHEST)
                                .setEquipSound(CF_PACK_MATERIAL.equipSound())
                                .setAsset(CF_PACK_MATERIAL.assetId())
                                .build());
    }

    private static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(IndustrialCraft.MOD_ID);
    private static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(IndustrialCraft.MOD_ID);

    public static final DeferredBlock<FoamBlock> FOAM =
            BLOCKS.registerBlock(
                    "foam",
                    properties ->
                            new FoamBlock(
                                    properties
                                            .noOcclusion()
                                            .strength(0.01F, 10.0F)
                                            .randomTicks()
                                            .sound(SoundType.WOOL)
                                            .noLootTable()));

    public static final Map<String, DeferredBlock<Block>> WALLS = walls();

    public static final DeferredItem<FoamSprayerItem> FOAM_SPRAYER =
            ITEMS.registerItem(
                    "foam_sprayer", properties -> new FoamSprayerItem(properties.stacksTo(1)));
    public static final DeferredItem<CFPackItem> CF_PACK =
            ITEMS.registerItem("cf_pack", CFPackItem::new, ModFoam::cfPackProperties);

    private static Map<String, DeferredBlock<Block>> walls() {
        var result = new LinkedHashMap<String, DeferredBlock<Block>>();
        for (String color : WALL_COLORS) {
            DeferredBlock<Block> wall =
                    BLOCKS.registerBlock(
                            color + "_wall",
                            properties ->
                                    new Block(
                                            properties
                                                    .strength(3.0F, 30.0F)
                                                    .requiresCorrectToolForDrops()
                                                    .sound(SoundType.STONE)));
            ITEMS.registerSimpleBlockItem(wall);
            result.put(color, wall);
        }
        return Collections.unmodifiableMap(result);
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        bus.addListener(ModFoam::capabilities);
        bus.addListener(ModFoam::creativeContents);
    }

    private static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerItem(
                Capabilities.Fluid.ITEM,
                (stack, access) -> new FoamTankHandler(access, FoamSprayerItem.CAPACITY_MB),
                FOAM_SPRAYER.get());
        event.registerItem(
                Capabilities.Fluid.ITEM,
                (stack, access) -> new FoamTankHandler(access, CFPackItem.CAPACITY_MB),
                CF_PACK.get());
    }

    private static void creativeContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.BUILDING_BLOCKS)) {
            // Foam has no BlockItem (sprayer-placed only); accepting the bare block
            // would hand the tab an ItemStack.EMPTY and crash the tab build.
            WALLS.values().forEach(event::accept);
        } else if (event.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES)) {
            event.accept(FOAM_SPRAYER);
        } else if (event.getTabKey().equals(CreativeModeTabs.COMBAT)) {
            event.accept(CF_PACK);
        }
    }

    private ModFoam() {}
}
