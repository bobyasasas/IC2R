package ic2.neoforge.registration;

import ic2.core.energy.ElectricItemSpec;
import ic2.neoforge.IndustrialCraft;
import ic2.neoforge.item.ElectricItem;
import ic2.neoforge.item.HazmatArmorItem;
import ic2.neoforge.item.NanoSuitItem;
import ic2.neoforge.item.NightVisionGogglesItem;

import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Map;
import java.util.function.UnaryOperator;

/**
 * The legacy hazmat suit: hazmat helmet, chestplate and leggings plus the rubber boots (legacy
 * registers the boots with the same ItemArmorHazmat class). Legacy Ic2ArmorMaterials.HAZMAT:
 * defence 3/6/8/3, toughness 2.0, zero durability and enchantability — reproduced by hand-assembled
 * item components because 26.1.2 rejects zero values there.
 */
public final class ModArmor {
    private static final TagKey<Item> NO_REPAIR =
            ItemTags.create(
                    Identifier.fromNamespaceAndPath(IndustrialCraft.MOD_ID, "hazmat_no_repair"));

    private static final ArmorMaterial HAZMAT =
            new ArmorMaterial(
                    0,
                    Map.of(
                            ArmorType.BOOTS,
                            3,
                            ArmorType.LEGGINGS,
                            6,
                            ArmorType.CHESTPLATE,
                            8,
                            ArmorType.HELMET,
                            3),
                    0,
                    SoundEvents.ARMOR_EQUIP_LEATHER,
                    2.0F,
                    0.0F,
                    NO_REPAIR,
                    ResourceKey.create(
                            EquipmentAssets.ROOT_ID,
                            Identifier.fromNamespaceAndPath(IndustrialCraft.MOD_ID, "ic2_hazmat")));

    private static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(IndustrialCraft.MOD_ID);

    public static final DeferredItem<HazmatArmorItem> HAZMAT_HELMET =
            ITEMS.registerItem(
                    "hazmat_helmet",
                    HazmatArmorItem::new,
                    hazmat(ArmorType.HELMET, EquipmentSlot.HEAD));
    public static final DeferredItem<HazmatArmorItem> HAZMAT_CHESTPLATE =
            ITEMS.registerItem(
                    "hazmat_chestplate",
                    HazmatArmorItem::new,
                    hazmat(ArmorType.CHESTPLATE, EquipmentSlot.CHEST));
    public static final DeferredItem<HazmatArmorItem> HAZMAT_LEGGINGS =
            ITEMS.registerItem(
                    "hazmat_leggings",
                    HazmatArmorItem::new,
                    hazmat(ArmorType.LEGGINGS, EquipmentSlot.LEGS));
    public static final DeferredItem<HazmatArmorItem> RUBBER_BOOTS =
            ITEMS.registerItem(
                    "rubber_boots",
                    HazmatArmorItem::new,
                    hazmat(ArmorType.BOOTS, EquipmentSlot.FEET));

    /**
     * The legacy wearable batteries: chest slot, no protection outside the material's 8-point
     * chestplate defence, and externally dischargeable so worn packs can feed held tools through
     * ElectricItemEnergy.use. Ic2ArmorMaterials.BAT_PACK: defence 0/0/8/0, toughness 2.0, zero
     * durability and enchantability.
     */
    private static final ArmorMaterial BAT_PACK =
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
                            Identifier.fromNamespaceAndPath(IndustrialCraft.MOD_ID, "ic2_bat_pack")));

    public static final DeferredItem<ElectricItem> BATPACK =
            ITEMS.registerItem(
                    "batpack",
                    p -> new ElectricItem(batpack(p), new ElectricItemSpec(60000, 100, 1, true)));
    public static final DeferredItem<ElectricItem> ADVANCED_BATPACK =
            ITEMS.registerItem(
                    "advanced_batpack",
                    p ->
                            new ElectricItem(
                                    batpack(p), new ElectricItemSpec(600000, 1000, 2, true)));

    private static Item.Properties batpack(Item.Properties properties) {
        return properties
                .attributes(BAT_PACK.createAttributes(ArmorType.CHESTPLATE))
                .component(
                        DataComponents.EQUIPPABLE,
                        Equippable.builder(EquipmentSlot.CHEST)
                                .setEquipSound(BAT_PACK.equipSound())
                                .setAsset(BAT_PACK.assetId())
                                .build());
    }

    /**
     * Legacy Ic2ArmorMaterials.NANO_SUIT: zero protection while uncharged — the charged
     * protection values (3/6/8/3 per slot) come from ElectricArmorItem instead. Toughness 2.0,
     * zero durability and enchantability.
     */
    private static final ArmorMaterial NANO_SUIT =
            new ArmorMaterial(
                    0,
                    Map.of(
                            ArmorType.BOOTS,
                            0,
                            ArmorType.LEGGINGS,
                            0,
                            ArmorType.CHESTPLATE,
                            0,
                            ArmorType.HELMET,
                            0),
                    0,
                    SoundEvents.ARMOR_EQUIP_IRON,
                    2.0F,
                    0.0F,
                    NO_REPAIR,
                    ResourceKey.create(
                            EquipmentAssets.ROOT_ID,
                            Identifier.fromNamespaceAndPath(IndustrialCraft.MOD_ID, "ic2_nano")));

    /**
     * Legacy Ic2ArmorMaterials.NIGHT_VISION_GOGGLES: 3-point helmet, toughness 2.0 (the legacy
     * protection array {0,0,0,3} indexes [boots,legs,chest,helmet]).
     */
    private static final ArmorMaterial NIGHT_VISION =
            new ArmorMaterial(
                    0,
                    Map.of(
                            ArmorType.BOOTS,
                            0,
                            ArmorType.LEGGINGS,
                            0,
                            ArmorType.CHESTPLATE,
                            0,
                            ArmorType.HELMET,
                            3),
                    0,
                    SoundEvents.ARMOR_EQUIP_IRON,
                    2.0F,
                    0.0F,
                    NO_REPAIR,
                    ResourceKey.create(
                            EquipmentAssets.ROOT_ID,
                            Identifier.fromNamespaceAndPath(
                                    IndustrialCraft.MOD_ID, "ic2_night_vision")));

    /** Legacy ItemArmorNanoSuit: 1M EU, tier 3, 1600 EU/t. */
    public static final DeferredItem<NanoSuitItem> NANO_HELMET =
            ITEMS.registerItem(
                    "nano_helmet",
                    p ->
                            new NanoSuitItem(
                                    nano(p, ArmorType.HELMET, EquipmentSlot.HEAD),
                                    nanoSpec(),
                                    ArmorType.HELMET,
                                    NANO_SUIT,
                                    3));
    public static final DeferredItem<NanoSuitItem> NANO_CHESTPLATE =
            ITEMS.registerItem(
                    "nano_chestplate",
                    p ->
                            new NanoSuitItem(
                                    nano(p, ArmorType.CHESTPLATE, EquipmentSlot.CHEST),
                                    nanoSpec(),
                                    ArmorType.CHESTPLATE,
                                    NANO_SUIT,
                                    8));
    public static final DeferredItem<NanoSuitItem> NANO_LEGGINGS =
            ITEMS.registerItem(
                    "nano_leggings",
                    p ->
                            new NanoSuitItem(
                                    nano(p, ArmorType.LEGGINGS, EquipmentSlot.LEGS),
                                    nanoSpec(),
                                    ArmorType.LEGGINGS,
                                    NANO_SUIT,
                                    6));
    public static final DeferredItem<NanoSuitItem> NANO_BOOTS =
            ITEMS.registerItem(
                    "nano_boots",
                    p ->
                            new NanoSuitItem(
                                    nano(p, ArmorType.BOOTS, EquipmentSlot.FEET),
                                    nanoSpec(),
                                    ArmorType.BOOTS,
                                    NANO_SUIT,
                                    3));

    private static ElectricItemSpec nanoSpec() {
        return new ElectricItemSpec(1000000, 1600, 3, false);
    }

    private static Item.Properties nano(
            Item.Properties properties, ArmorType type, EquipmentSlot slot) {
        // No static ATTRIBUTE_MODIFIERS component: the charged/uncharged switch lives in
        // ElectricArmorItem.getDefaultAttributeModifiers, which the stack only consults
        // while the component stays empty.
        return properties
                .rarity(Rarity.UNCOMMON)
                .component(
                        DataComponents.EQUIPPABLE,
                        Equippable.builder(slot)
                                .setEquipSound(NANO_SUIT.equipSound())
                                .setAsset(NANO_SUIT.assetId())
                                .build());
    }

    /** Legacy ItemArmorNightVisionGoggles: 200k EU at tier 1 and a 27-use durability bar. */
    public static final DeferredItem<NightVisionGogglesItem> NIGHT_VISION_GOGGLES =
            ITEMS.registerItem(
                    "night_vision_goggles",
                    p ->
                            new NightVisionGogglesItem(
                                    p.attributes(NIGHT_VISION.createAttributes(ArmorType.HELMET))
                                            .durability(27)
                                            .component(
                                                    DataComponents.EQUIPPABLE,
                                                    Equippable.builder(EquipmentSlot.HEAD)
                                                            .setEquipSound(
                                                                    NIGHT_VISION.equipSound())
                                                            .setAsset(NIGHT_VISION.assetId())
                                                            .build()),
                                    new ElectricItemSpec(200000, 200, 1, false)));

    private static UnaryOperator<Item.Properties> hazmat(ArmorType type, EquipmentSlot slot) {
        return properties ->
                properties
                        .attributes(HAZMAT.createAttributes(type))
                        .component(
                                DataComponents.EQUIPPABLE,
                                Equippable.builder(slot)
                                        .setEquipSound(HAZMAT.equipSound())
                                        .setAsset(HAZMAT.assetId())
                                        .build());
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        modBus.addListener(ModArmor::creativeTab);
    }

    private static void creativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(ModArmor.HAZMAT_HELMET);
            event.accept(ModArmor.HAZMAT_CHESTPLATE);
            event.accept(ModArmor.HAZMAT_LEGGINGS);
            event.accept(ModArmor.RUBBER_BOOTS);
            event.accept(ModArmor.BATPACK);
            event.accept(ModArmor.ADVANCED_BATPACK);
            event.accept(ModArmor.NANO_HELMET);
            event.accept(ModArmor.NANO_CHESTPLATE);
            event.accept(ModArmor.NANO_LEGGINGS);
            event.accept(ModArmor.NANO_BOOTS);
            event.accept(ModArmor.NIGHT_VISION_GOGGLES);
        }
    }

    private ModArmor() {}
}
