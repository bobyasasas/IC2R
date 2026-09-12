package ic2.neoforge.test;

import ic2.neoforge.component.ModDataComponents;
import ic2.neoforge.item.ElectricItemEnergy;
import ic2.neoforge.item.NanoSaberItem;
import ic2.neoforge.registration.ModArmor;
import ic2.neoforge.registration.ModTools;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.GameType;

/**
 * Legacy nano saber: right-click toggles the blade free of charge but only with 16 EU stored, a
 * held blade bills 64 EU per 16 ticks and switches itself off when the tank runs dry, the attack
 * profile follows the activation (20 damage at speed 0 vs 4 at speed -3, gated on 400 EU), and a
 * strike on IC2 armour bills 2000 EU and dumps 48000 EU out of a worn nano suit piece, removing
 * it once fully drained.
 */
final class NanoSaberTests {
    static void useTogglesActiveFree(GameTestHelper helper) {
        ItemStack saber = charged(1000.0, false);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, saber);
        var item = ModTools.NANO_SABER.get();

        item.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(NanoSaberItem.isActive(saber), "Right-click must switch the blade on");
        helper.assertTrue(
                ElectricItemEnergy.charge(saber) == 1000.0, "Toggling the blade must stay free");

        item.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(
                !NanoSaberItem.isActive(saber), "A second right-click must switch it off");

        ItemStack empty = charged(15.0, false);
        player.setItemInHand(InteractionHand.MAIN_HAND, empty);
        item.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(
                !NanoSaberItem.isActive(empty),
                "A blade below the 16 EU activation minimum must refuse to switch on");
        helper.succeed();
    }

    static void heldBillingDepletesAndShutsOff(GameTestHelper helper) {
        ItemStack saber = charged(96.0, true);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, saber);
        var item = ModTools.NANO_SABER.get();

        for (int tick = 1; tick <= 16; tick++) {
            item.inventoryTick(
                    saber,
                    helper.getLevel(),
                    player,
                    tick % 2 == 0 ? EquipmentSlot.MAINHAND : null);
        }
        helper.assertTrue(
                ElectricItemEnergy.charge(saber) == 32.0,
                "A held blade must bill 64 EU per 16 ticks");

        for (int tick = 17; tick <= 32; tick++) {
            item.inventoryTick(saber, helper.getLevel(), player, EquipmentSlot.MAINHAND);
        }
        helper.assertTrue(
                ElectricItemEnergy.charge(saber) == 32.0,
                "A failed bill must not drain below the remaining charge");
        helper.assertTrue(
                !NanoSaberItem.isActive(saber),
                "A blade that cannot pay its bill must switch itself off");
        helper.succeed();
    }

    static void attributesFollowActivation(GameTestHelper helper) {
        ItemStack saber = charged(1000.0, false);
        ItemAttributeModifiers idle =
                ModTools.NANO_SABER.get().getDefaultAttributeModifiers(saber);
        helper.assertTrue(
                damageOf(idle) == 4.0 && speedOf(idle) == -3.0,
                "The idle blade must hit for 4 at speed -3");

        saber.set(ModDataComponents.SABER_ACTIVE, true);
        ItemAttributeModifiers active =
                ModTools.NANO_SABER.get().getDefaultAttributeModifiers(saber);
        helper.assertTrue(
                damageOf(active) == 20.0 && speedOf(active) == 0.0,
                "The active blade must hit for 20 at full speed");

        ItemStack weak = charged(100.0, true);
        ItemAttributeModifiers underPowered =
                ModTools.NANO_SABER.get().getDefaultAttributeModifiers(weak);
        helper.assertTrue(
                damageOf(underPowered) == 4.0 && speedOf(underPowered) == -3.0,
                "Below 400 EU the active blade must fall back to idle stats");
        helper.succeed();
    }

    static void strikeDrainsNanoArmor(GameTestHelper helper) {
        ItemStack saber = charged(20000.0, true);
        Player attacker = helper.makeMockPlayer(GameType.SURVIVAL);
        attacker.setItemInHand(InteractionHand.MAIN_HAND, saber);

        Zombie target = helper.spawn(EntityType.ZOMBIE, new BlockPos(3, 2, 3));
        // The strike is a direct method call; a wandering mob must never leave this plot.
        target.setNoAi(true);
        ItemStack chest = ModArmor.NANO_CHESTPLATE.toStack();
        ElectricItemEnergy.charge(chest, 48000.0, 3, true, false);
        target.setItemSlot(EquipmentSlot.CHEST, chest);

        ModTools.NANO_SABER.get().postHurtEnemy(saber, target, attacker);

        helper.assertTrue(
                ElectricItemEnergy.charge(saber) == 17600.0,
                "The strike must bill 400 EU plus one 2000 EU disable attempt");
        helper.assertTrue(
                target.getItemBySlot(EquipmentSlot.CHEST).isEmpty(),
                "A fully drained nano chestplate must be removed from its slot");
        helper.succeed();
    }

    private static ItemStack charged(double amount, boolean active) {
        ItemStack saber = new ItemStack(ModTools.NANO_SABER.get());
        ElectricItemEnergy.charge(saber, amount, 3, true, false);
        if (active) {
            saber.set(ModDataComponents.SABER_ACTIVE, true);
        }
        return saber;
    }

    private static double damageOf(ItemAttributeModifiers modifiers) {
        return amountOf(modifiers, Attributes.ATTACK_DAMAGE);
    }

    private static double speedOf(ItemAttributeModifiers modifiers) {
        return amountOf(modifiers, Attributes.ATTACK_SPEED);
    }

    private static double amountOf(
            ItemAttributeModifiers modifiers, Holder<Attribute> attribute) {
        return modifiers
                .modifiers()
                .stream()
                .filter(entry -> entry.attribute().equals(attribute))
                .mapToDouble(entry -> entry.modifier().amount())
                .sum();
    }
}
