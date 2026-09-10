package ic2.neoforge.registration;

import ic2.neoforge.IndustrialCraft;
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
        }
    }

    private ModArmor() {}
}
