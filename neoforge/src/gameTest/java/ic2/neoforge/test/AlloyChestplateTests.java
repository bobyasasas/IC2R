package ic2.neoforge.test;

import ic2.neoforge.item.MetalArmorLike;
import ic2.neoforge.registration.ModArmor;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.enchantment.Repairable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.GameType;

import java.util.List;

/** Legacy ItemArmorIC2 alloy chestplate: material stats, wear semantics and alloy repair. */
final class AlloyChestplateTests {
    static void materialStatsAndRepair(GameTestHelper helper) {
        var chestplate = ModArmor.ALLOY_CHESTPLATE.toStack();
        var modifiers =
                (ItemAttributeModifiers) chestplate.get(DataComponents.ATTRIBUTE_MODIFIERS);
        helper.assertTrue(modifiers != null, "The chestplate must carry attribute modifiers");
        double armor = 0;
        double toughness = 0;
        for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
            if (entry.attribute().value() == Attributes.ARMOR.value()) armor += entry.modifier().amount();
            if (entry.attribute().value() == Attributes.ARMOR_TOUGHNESS.value())
                toughness += entry.modifier().amount();
        }
        helper.assertTrue(
                armor == 9 && toughness == 2.0,
                "Legacy ALLOY material: 9 chest defence and 2.0 toughness, got " + armor + "/" + toughness);
        helper.assertTrue(
                chestplate.getMaxDamage() == 800,
                "Legacy 50× durability multiplier must yield 800 for a chestplate, got "
                        + chestplate.getMaxDamage());
        var repairable = (Repairable) chestplate.get(DataComponents.REPAIRABLE);
        helper.assertTrue(repairable != null, "The chestplate must be repairable");
        boolean repairsAlloy = false;
        for (Holder<net.minecraft.world.item.Item> holder : repairable.items()) {
            if (holder.getRegisteredName().equals("ic2:alloy")) repairsAlloy = true;
        }
        helper.assertTrue(
                repairsAlloy,
                "The repair ingredient set must contain the alloy ingot (legacy Ic2ArmorMaterials.ALLOY)");
        helper.assertTrue(
                chestplate.getItem() instanceof MetalArmorLike,
                "Legacy ItemArmorIC2 implements IMetalArmor; the port marks it MetalArmorLike");
        helper.succeed();
    }

    static void wearAddsArmor(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        AttributeInstance attribute = player.getAttribute(Attributes.ARMOR);
        double before = attribute == null ? 0 : attribute.getValue();
        var chestplate = ModArmor.ALLOY_CHESTPLATE.toStack();
        player.setItemSlot(EquipmentSlot.CHEST, chestplate);
        // Mock players never tick, so vanilla's transient equipment-modifier sync never runs.
        chestplate.forEachModifier(
                EquipmentSlot.CHEST,
                (attrib, modifier) -> {
                    var instance = player.getAttribute(attrib);
                    if (instance != null) instance.addTransientModifier(modifier);
                });
        double after = attribute == null ? 0 : attribute.getValue();
        helper.assertTrue(
                after - before == 9,
                "Wearing the alloy chestplate must add its 9 defence points, got " + (after - before));
        helper.assertTrue(
                player.getItemBySlot(EquipmentSlot.CHEST).is(ModArmor.ALLOY_CHESTPLATE.get()),
                "The chestplate must stay equipped in the chest slot");
        helper.succeed();
    }

    private AlloyChestplateTests() {}
}
