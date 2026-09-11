package ic2.neoforge.item;

import ic2.core.energy.ElectricItemSpec;
import ic2.neoforge.IndustrialCraft;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;

/**
 * Legacy ItemArmorElectric core: while the stored charge covers one {@link #energyPerDamage},
 * the piece carries its charged protection value in place of the material's zero armour; damage
 * absorption itself runs in {@link ElectricArmorHelper}. Attribute modifiers are resolved per
 * stack like legacy (26.1.2 consults {@link #getDefaultAttributeModifiers(ItemStack)} on equip;
 * later charge changes refresh on the next equip, matching the legacy cache behaviour).
 */
public class ElectricArmorItem extends ElectricItem {
    private final int energyPerDamage;
    private final double damageAbsorptionRatio;
    private final ItemAttributeModifiers chargedModifiers;
    private final ItemAttributeModifiers unchargedModifiers;

    protected ElectricArmorItem(
            Properties properties,
            ElectricItemSpec specification,
            ArmorType type,
            ArmorMaterial material,
            int chargedProtection,
            int energyPerDamage,
            double damageAbsorptionRatio) {
        super(properties, specification);
        this.energyPerDamage = energyPerDamage;
        this.damageAbsorptionRatio = damageAbsorptionRatio;
        var group = groupOf(type);
        this.chargedModifiers =
                ItemAttributeModifiers.builder()
                        .add(
                                Attributes.ARMOR,
                                new AttributeModifier(
                                        Identifier.fromNamespaceAndPath(
                                                IndustrialCraft.MOD_ID, "ic2_charged_armor"),
                                        chargedProtection,
                                        AttributeModifier.Operation.ADD_VALUE),
                                group)
                        .add(
                                Attributes.ARMOR_TOUGHNESS,
                                new AttributeModifier(
                                        Identifier.fromNamespaceAndPath(
                                                IndustrialCraft.MOD_ID, "ic2_armor_toughness"),
                                        material.toughness(),
                                        AttributeModifier.Operation.ADD_VALUE),
                                group)
                        .build();
        this.unchargedModifiers = material.createAttributes(type);
    }

    private static EquipmentSlotGroup groupOf(ArmorType type) {
        return switch (type) {
            case HELMET -> EquipmentSlotGroup.HEAD;
            case CHESTPLATE -> EquipmentSlotGroup.CHEST;
            case LEGGINGS -> EquipmentSlotGroup.LEGS;
            case BOOTS -> EquipmentSlotGroup.FEET;
            case BODY -> EquipmentSlotGroup.BODY;
        };
    }

    /** Legacy getEnergyPerDamage: EU spent per point of absorbed damage. */
    public int energyPerDamage() {
        return this.energyPerDamage;
    }

    /** Legacy getDamageAbsorptionRatio. */
    public double damageAbsorptionRatio() {
        return this.damageAbsorptionRatio;
    }

    /** Legacy getBaseAbsorptionRatio: the per-slot share of incoming damage. */
    public static double baseAbsorptionRatio(EquipmentSlot slot) {
        return switch (slot) {
            case HEAD, FEET -> 0.15;
            case CHEST -> 0.4;
            case LEGS -> 0.3;
            default -> 0.0;
        };
    }

    public boolean isCharged(ItemStack stack) {
        return ElectricItemEnergy.charge(stack) >= this.energyPerDamage;
    }

    /** Legacy fall absorption; only the nano and quantum boots implement it. */
    public boolean absorbFall(ItemStack stack, float distance) {
        return false;
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        return this.isCharged(stack) ? this.chargedModifiers : this.unchargedModifiers;
    }
}
