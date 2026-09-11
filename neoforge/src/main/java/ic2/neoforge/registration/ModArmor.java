package ic2.neoforge.registration;

import ic2.core.energy.ElectricItemSpec;
import ic2.neoforge.IndustrialCraft;
import ic2.neoforge.item.ElectricItem;
import ic2.neoforge.item.HazmatArmorItem;
import ic2.neoforge.item.NanoSuitItem;
import ic2.neoforge.item.NightVisionGogglesItem;
import ic2.neoforge.item.JetpackElectricItem;
import ic2.neoforge.item.JetpackItem;
import ic2.neoforge.item.QuantumSuitItem;
import ic2.neoforge.item.SolarHelmetItem;
import ic2.neoforge.item.StaticBootsItem;

import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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

    /** The remaining legacy chest/helmet/feet utility packs: JET_PACK, JET_PACK_ELECTRIC,
     * ENERGY_PACK and LAP_PACK share the batpack's 8-point chest defence and 2.0 toughness;
     * SOLAR_HELMET carries a 3-point helmet with no toughness and STATIC_BOOTS a 3-point boot.
     */
    private static ArmorMaterial utilityMaterial(
            String asset, EquipmentSlotGroup slot, int defence, float toughness) {
        return new ArmorMaterial(
                0,
                Map.of(armorTypeOf(slot), defence),
                0,
                SoundEvents.ARMOR_EQUIP_IRON,
                toughness,
                0.0F,
                NO_REPAIR,
                ResourceKey.create(
                        EquipmentAssets.ROOT_ID,
                        Identifier.fromNamespaceAndPath(IndustrialCraft.MOD_ID, asset)));
    }

    private static ArmorType armorTypeOf(EquipmentSlotGroup group) {
        return switch (group) {
            case HEAD -> ArmorType.HELMET;
            case CHEST -> ArmorType.CHESTPLATE;
            case LEGS -> ArmorType.LEGGINGS;
            case FEET -> ArmorType.BOOTS;
            default -> ArmorType.BODY;
        };
    }

    private static final ArmorMaterial LAP_PACK_MATERIAL =
            utilityMaterial("ic2_lap_pack", EquipmentSlotGroup.CHEST, 8, 2.0F);
    private static final ArmorMaterial ENERGY_PACK_MATERIAL =
            utilityMaterial("ic2_energy_pack", EquipmentSlotGroup.CHEST, 8, 2.0F);
    private static final ArmorMaterial JET_PACK_MATERIAL =
            utilityMaterial("ic2_jet_pack", EquipmentSlotGroup.CHEST, 8, 2.0F);
    private static final ArmorMaterial JET_PACK_ELECTRIC_MATERIAL =
            utilityMaterial("ic2_jet_pack_electric", EquipmentSlotGroup.CHEST, 8, 2.0F);
    private static final ArmorMaterial SOLAR_HELMET_MATERIAL =
            utilityMaterial("ic2_solar_helmet", EquipmentSlotGroup.HEAD, 3, 0.0F);
    private static final ArmorMaterial STATIC_BOOTS_MATERIAL =
            utilityMaterial("ic2_static_boots", EquipmentSlotGroup.FEET, 3, 0.0F);

    private static Item.Properties utility(
            Item.Properties properties,
            ArmorMaterial material,
            EquipmentSlotGroup group,
            EquipmentSlot slot) {
        return properties
                .attributes(material.createAttributes(armorTypeOf(group)))
                .component(
                        DataComponents.EQUIPPABLE,
                        Equippable.builder(slot)
                                .setEquipSound(material.equipSound())
                                .setAsset(material.assetId())
                                .build());
    }

    /** Legacy ItemArmorLappack: 20M EU, 2500 transfer, tier 4, externally dischargeable. */
    public static final DeferredItem<ElectricItem> LAPACK =
            ITEMS.registerItem(
                    "lappack",
                    p ->
                            new ElectricItem(
                                    utility(p, LAP_PACK_MATERIAL, EquipmentSlotGroup.CHEST, EquipmentSlot.CHEST)
                                            .rarity(Rarity.UNCOMMON),
                                    new ElectricItemSpec(20000000, 2500, 4, true)));
    /** Legacy ItemArmorEnergypack: 2M EU, 1000 transfer, tier 3, externally dischargeable. */
    public static final DeferredItem<ElectricItem> ENERGY_PACK =
            ITEMS.registerItem(
                    "energy_pack",
                    p ->
                            new ElectricItem(
                                    utility(p, ENERGY_PACK_MATERIAL, EquipmentSlotGroup.CHEST, EquipmentSlot.CHEST),
                                    new ElectricItemSpec(2000000, 1000, 3, true)));
    /** Legacy ItemArmorJetpackElectric: 30000 EU, 60 transfer, tier 1, thrust 0.7. */
    public static final DeferredItem<JetpackElectricItem> JETPACK_ELECTRIC =
            ITEMS.registerItem(
                    "jetpack_electric",
                    p ->
                            new JetpackElectricItem(
                                    utility(p, JET_PACK_ELECTRIC_MATERIAL, EquipmentSlotGroup.CHEST, EquipmentSlot.CHEST),
                                    new ElectricItemSpec(30000, 60, 1, false)));
    /** Legacy ItemArmorJetpack: the 30000 mB biogas tank jetpack, thrust 1.0. */
    public static final DeferredItem<JetpackItem> JETPACK =
            ITEMS.registerItem(
                    "jetpack",
                    p ->
                            new JetpackItem(
                                    utility(p, JET_PACK_MATERIAL, EquipmentSlotGroup.CHEST, EquipmentSlot.CHEST)));
    /** Legacy ItemArmorSolarHelmet: charges the worn chest piece from sunlight. */
    public static final DeferredItem<SolarHelmetItem> SOLAR_HELMET_ITEM =
            ITEMS.registerItem(
                    "solar_helmet",
                    p ->
                            new SolarHelmetItem(
                                    utility(p, SOLAR_HELMET_MATERIAL, EquipmentSlotGroup.HEAD, EquipmentSlot.HEAD)));
    /** Legacy ItemArmorStaticBoots: charges the worn chest piece from walking. */
    public static final DeferredItem<StaticBootsItem> STATIC_BOOTS_ITEM =
            ITEMS.registerItem(
                    "static_boots",
                    p ->
                            new StaticBootsItem(
                                    utility(p, STATIC_BOOTS_MATERIAL, EquipmentSlotGroup.FEET, EquipmentSlot.FEET)));

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
     * Legacy Ic2ArmorMaterials.QUANTUM_SUIT: zero protection while uncharged; the charged
     * protection values (3/6/8/3 per slot) come from ElectricArmorItem. Toughness 2.0.
     */
    private static final ArmorMaterial QUANTUM_SUIT =
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
                            Identifier.fromNamespaceAndPath(
                                    IndustrialCraft.MOD_ID, "ic2_quantum")));

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

    /** Legacy ItemArmorQuantumSuit: 10M EU, tier 4, 12000 EU/t, per-slot charged protection. */
    public static final DeferredItem<QuantumSuitItem> QUANTUM_HELMET =
            ITEMS.registerItem(
                    "quantum_helmet",
                    p ->
                            new QuantumSuitItem(
                                    quantum(p, ArmorType.HELMET, EquipmentSlot.HEAD),
                                    quantumSpec(),
                                    ArmorType.HELMET,
                                    QUANTUM_SUIT,
                                    3));
    public static final DeferredItem<QuantumSuitItem> QUANTUM_CHESTPLATE =
            ITEMS.registerItem(
                    "quantum_chestplate",
                    p ->
                            new QuantumSuitItem(
                                    quantum(p, ArmorType.CHESTPLATE, EquipmentSlot.CHEST),
                                    quantumSpec(),
                                    ArmorType.CHESTPLATE,
                                    QUANTUM_SUIT,
                                    8));
    public static final DeferredItem<QuantumSuitItem> QUANTUM_LEGGINGS =
            ITEMS.registerItem(
                    "quantum_leggings",
                    p ->
                            new QuantumSuitItem(
                                    quantum(p, ArmorType.LEGGINGS, EquipmentSlot.LEGS),
                                    quantumSpec(),
                                    ArmorType.LEGGINGS,
                                    QUANTUM_SUIT,
                                    6));
    public static final DeferredItem<QuantumSuitItem> QUANTUM_BOOTS =
            ITEMS.registerItem(
                    "quantum_boots",
                    p ->
                            new QuantumSuitItem(
                                    quantum(p, ArmorType.BOOTS, EquipmentSlot.FEET),
                                    quantumSpec(),
                                    ArmorType.BOOTS,
                                    QUANTUM_SUIT,
                                    3));

    private static ElectricItemSpec quantumSpec() {
        return new ElectricItemSpec(10000000, 12000, 4, false);
    }

    private static Item.Properties quantum(
            Item.Properties properties, ArmorType type, EquipmentSlot slot) {
        // No static ATTRIBUTE_MODIFIERS component: charged protection resolves dynamically in
        // ElectricArmorItem.getDefaultAttributeModifiers, like the nano suit.
        return properties
                .rarity(Rarity.RARE)
                .component(
                        DataComponents.EQUIPPABLE,
                        Equippable.builder(slot)
                                .setEquipSound(QUANTUM_SUIT.equipSound())
                                .setAsset(QUANTUM_SUIT.assetId())
                                .build());
    }

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
            event.accept(ModArmor.QUANTUM_HELMET);
            event.accept(ModArmor.QUANTUM_CHESTPLATE);
            event.accept(ModArmor.QUANTUM_LEGGINGS);
            event.accept(ModArmor.QUANTUM_BOOTS);
            event.accept(ModArmor.LAPACK);
            event.accept(ModArmor.ENERGY_PACK);
            event.accept(ModArmor.JETPACK_ELECTRIC);
            event.accept(ModArmor.JETPACK);
            event.accept(ModArmor.SOLAR_HELMET_ITEM);
            event.accept(ModArmor.STATIC_BOOTS_ITEM);
            // Legacy fillItemCategory showed the biogas jetpack filled and empty.
            var filledJetpack = new ItemStack(ModArmor.JETPACK.get());
            JetpackItem.fillMb(filledJetpack, JetpackItem.CAPACITY_MB);
            event.accept(filledJetpack);
        }
    }

    private ModArmor() {}
}
