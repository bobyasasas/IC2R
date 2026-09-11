package ic2.neoforge.registration;

import ic2.core.energy.ElectricItemSpec;
import ic2.neoforge.IndustrialCraft;
import ic2.neoforge.item.ElectricItem;
import ic2.neoforge.item.HazmatArmorItem;

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
        }
    }

    private ModArmor() {}
}
