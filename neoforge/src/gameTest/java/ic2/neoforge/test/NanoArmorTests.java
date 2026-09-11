package ic2.neoforge.test;

import com.mojang.authlib.GameProfile;

import ic2.core.energy.ElectricItemSpec;
import ic2.neoforge.component.ModDataComponents;
import ic2.neoforge.item.ElectricItemEnergy;
import ic2.neoforge.item.NightVisionHelper;
import ic2.neoforge.registration.ModArmor;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Legacy nano suit and night vision goggles: charged protection attributes, energy-based damage
 * absorption, boot fall absorption and the helmet night vision toggle (ported to sneak + use).
 */
final class NanoArmorTests {
    private static double armorAmount(ItemAttributeModifiers modifiers) {
        double sum = 0;
        for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
            if (entry.attribute().value() == Attributes.ARMOR.value()) {
                sum += entry.modifier().amount();
            }
        }
        return sum;
    }

    private static List<Double> toughnessAmounts(ItemAttributeModifiers modifiers) {
        List<Double> amounts = new ArrayList<>();
        for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
            if (entry.attribute().value() == Attributes.ARMOR_TOUGHNESS.value()) {
                amounts.add(entry.modifier().amount());
            }
        }
        return amounts;
    }

    private static Player fakePlayer(GameTestHelper helper, String name) {
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), name));
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        player.setInvulnerable(false);
        try {
            // Fresh ServerPlayers start "awaiting respawn" and FakePlayer never ticks the
            // client-load timeout, so ServerPlayer.isInvulnerableTo would swallow all damage.
            var respawn =
                    net.minecraft.server.network.ServerGamePacketListenerImpl.class
                            .getDeclaredField("waitingForRespawn");
            respawn.setAccessible(true);
            respawn.setBoolean(player.connection, false);
            player.connection.markClientLoaded();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot enable damage on the fake player", e);
        }
        return player;
    }

    /** FakePlayer never ticks, so vanilla's transient equipment-modifier sync never runs. */
    private static void wear(Player player, ItemStack stack, EquipmentSlot slot) {
        player.setItemSlot(slot, stack);
        stack.forEachModifier(
                slot,
                (attribute, modifier) -> {
                    var instance = player.getAttribute(attribute);
                    if (instance != null) {
                        instance.removeModifier(modifier.id());
                        instance.addTransientModifier(modifier);
                    }
                });
    }

    static void specAndChargedAttributes(GameTestHelper helper) {
        helper.assertTrue(
                ModArmor.NANO_HELMET.get()
                        .specification()
                        .equals(new ElectricItemSpec(1000000, 1600, 3, false)),
                "Nano suit must keep legacy 1000000/1600/t3 spec without external output");
        helper.assertTrue(
                ModArmor.NIGHT_VISION_GOGGLES.get()
                        .specification()
                        .equals(new ElectricItemSpec(200000, 200, 1, false)),
                "Goggles must keep legacy 200000/200/t1 spec without external output");

        var helmet = ModArmor.NANO_HELMET.toStack();
        double uncharged =
                armorAmount(ModArmor.NANO_HELMET.get().getDefaultAttributeModifiers(helmet));
        helper.assertTrue(
                uncharged == 0.0,
                "Uncharged nano helmet must carry the material's zero armour");

        ElectricItemEnergy.charge(helmet, 5000, 3, true, false);
        var charged = ModArmor.NANO_HELMET.get().getDefaultAttributeModifiers(helmet);
        helper.assertTrue(
                armorAmount(charged) == 3.0
                        && toughnessAmounts(charged).equals(List.of(2.0)),
                "Charged nano helmet must show CHARGED_PROTECTION 3 with legacy 2.0 toughness");

        var chest = ModArmor.NANO_CHESTPLATE.toStack();
        ElectricItemEnergy.charge(chest, 5000, 3, true, false);
        helper.assertTrue(
                armorAmount(ModArmor.NANO_CHESTPLATE.get().getDefaultAttributeModifiers(chest))
                        == 8.0,
                "Charged nano chestplate must show CHARGED_PROTECTION 8");
        var legs = ModArmor.NANO_LEGGINGS.toStack();
        ElectricItemEnergy.charge(legs, 5000, 3, true, false);
        helper.assertTrue(
                armorAmount(ModArmor.NANO_LEGGINGS.get().getDefaultAttributeModifiers(legs)) == 6.0,
                "Charged nano leggings must show CHARGED_PROTECTION 6");
        var boots = ModArmor.NANO_BOOTS.toStack();
        ElectricItemEnergy.charge(boots, 4999, 3, true, false);
        helper.assertTrue(
                armorAmount(ModArmor.NANO_BOOTS.get().getDefaultAttributeModifiers(boots)) == 0.0,
                "Nano boots below one damage charge must fall back to the material's zero armour");

        var goggles = ModArmor.NIGHT_VISION_GOGGLES.toStack();
        helper.assertTrue(
                armorAmount(goggles.getOrDefault(
                        net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS,
                        ItemAttributeModifiers.EMPTY))
                        == 3.0,
                "Goggles must carry the legacy 3-point helmet material defence");
        helper.succeed();
    }

    static void energyAbsorption(GameTestHelper helper) {
        var level = helper.getLevel();
        Player player = fakePlayer(helper, "ic2-nano-abs");
        var chest = ModArmor.NANO_CHESTPLATE.toStack();
        ElectricItemEnergy.charge(chest, 40000, 3, true, false);
        wear(player, chest, EquipmentSlot.CHEST);

        player.hurt(level.damageSources().generic(), 10.0F);
        // Legacy damageArmor: chest share 0.4 * ratio 0.9 = 3.6 of the 10 damage absorbed
        // for 3.6 * 5000 = 18000 EU; the rest stays for vanilla armour to reduce.
        double charge = ElectricItemEnergy.charge(player.getItemBySlot(EquipmentSlot.CHEST));
        helper.assertTrue(
                Math.abs(charge - 22000) < 1e-6,
                "Charged chestplate must absorb 3.6 damage for 18000 EU (got " + charge + ")");
        // minecraft:generic ignores armour, so the player must take exactly the
        // unabsorbed 6.4 damage of the 10 dealt.
        float taken = player.getMaxHealth() - player.getHealth();
        helper.assertTrue(
                Math.abs(taken - 6.4F) < 1e-3,
                "Absorption must remove exactly 3.6 of 10 damage (player took " + taken + ")");
        // Legacy damageArmor: chest share 0.4 * ratio 0.9 = 3.6 of the 10 damage absorbed

        // A bypassing source must skip the energy absorption entirely (legacy gate).
        Player plain = fakePlayer(helper, "ic2-nano-byp");
        var plainChest = ModArmor.NANO_CHESTPLATE.toStack();
        ElectricItemEnergy.charge(plainChest, 40000, 3, true, false);
        wear(plain, plainChest, EquipmentSlot.CHEST);
        plain.hurt(level.damageSources().genericKill(), 10.0F);
        helper.assertTrue(
                ElectricItemEnergy.charge(plain.getItemBySlot(EquipmentSlot.CHEST)) == 40000,
                "BYPASSES_ENCHANTMENTS sources must not drain electric armour");

        // An uncharged piece must absorb nothing.
        Player empty = fakePlayer(helper, "ic2-nano-empty");
        var emptyChest = ModArmor.NANO_CHESTPLATE.toStack();
        wear(player, emptyChest, EquipmentSlot.CHEST);
        empty.hurt(level.damageSources().generic(), 10.0F);
        helper.assertTrue(
                ElectricItemEnergy.charge(emptyChest) == 0,
                "Empty nano chestplate must not drain");
        float emptyTaken = empty.getMaxHealth() - empty.getHealth();
        helper.assertTrue(
                Math.abs(emptyTaken - 10.0F) < 1e-3,
                "Empty nano chestplate must protect nothing (player took " + emptyTaken + ")");
        helper.succeed();
    }

    static void fallAbsorption(GameTestHelper helper) {
        var boots = ModArmor.NANO_BOOTS.toStack();
        ElectricItemEnergy.charge(boots, 15000, 3, true, false);
        helper.assertTrue(
                ModArmor.NANO_BOOTS.get().absorbFall(boots, 5.0F),
                "Nano boots must absorb a five-block fall (2 damage for 10000 EU)");
        helper.assertTrue(
                ElectricItemEnergy.charge(boots) == 5000,
                "Fall absorption must spend exactly energyPerDamage * fallDamage");
        helper.assertTrue(
                !ModArmor.NANO_BOOTS.get().absorbFall(boots, 11.0F),
                "Falls of eight damage or more must overwhelm the boots like legacy");
        helper.assertTrue(
                ElectricItemEnergy.charge(boots) == 5000,
                "A failed absorption must not spend energy");

        Player player = fakePlayer(helper, "ic2-nano-fall");
        var worn = ModArmor.NANO_BOOTS.toStack();
        ElectricItemEnergy.charge(worn, 15000, 3, true, false);
        wear(player, worn, EquipmentSlot.FEET);
        boolean damaged = player.causeFallDamage(5.0, 1.0F, helper.getLevel().damageSources().fall());
        helper.assertTrue(
                !damaged && player.getHealth() == player.getMaxHealth(),
                "LivingFallEvent wiring must cancel an affordable fall");
        helper.assertTrue(
                ElectricItemEnergy.charge(player.getItemBySlot(EquipmentSlot.FEET)) == 5000,
                "The cancelled fall must still drain the boots");
        helper.succeed();
    }

    static void nightVisionToggleAndTick(GameTestHelper helper) {
        var level = helper.getLevel();
        Player player = fakePlayer(helper, "ic2-nano-nv");
        // Stand inside the machine room so skylight stays low and the effect is night vision.
        var inside = helper.absolutePos(new BlockPos(2, 2, 2));
        player.setPos(inside.getX() + 0.5, inside.getY(), inside.getZ() + 0.5);

        var goggles = ModArmor.NIGHT_VISION_GOGGLES.toStack();
        ElectricItemEnergy.charge(goggles, 100, 1, true, false);
        player.setItemInHand(InteractionHand.MAIN_HAND, goggles);
        player.setShiftKeyDown(true);
        helper.assertTrue(
                ModArmor.NIGHT_VISION_GOGGLES.get().use(level, player, InteractionHand.MAIN_HAND)
                        == InteractionResult.SUCCESS,
                "Sneak + use must toggle the goggles");
        helper.assertTrue(
                goggles.getOrDefault(ModDataComponents.NIGHT_VISION_ACTIVE.get(), false),
                "The toggle must set the night vision component");

        wear(player, goggles, EquipmentSlot.HEAD);
        NightVisionHelper.onPlayerTick(new PlayerTickEvent.Post(player));
        double afterTick = ElectricItemEnergy.charge(player.getItemBySlot(EquipmentSlot.HEAD));
        helper.assertTrue(
                afterTick == 99,
                "Night vision must drain one EU per tick (got " + afterTick + ")");
        // Legacy affectPlayer: bright skylight blinds, otherwise night vision.
        int skylight =
                level.getMaxLocalRawBrightness(BlockPos.containing(player.position()));
        if (skylight > 8) {
            helper.assertTrue(
                    player.hasEffect(MobEffects.BLINDNESS),
                    "In bright skylight the active helmet must blind the player");
        } else {
            helper.assertTrue(
                    player.hasEffect(MobEffects.NIGHT_VISION),
                    "An active helmet must grant night vision in the dark (skylight " + skylight
                            + ")");
        }

        // Toggling off (sneak + use with the same stack in hand) must stop the drain.
        player.setItemInHand(InteractionHand.MAIN_HAND, player.getItemBySlot(EquipmentSlot.HEAD));
        ModArmor.NIGHT_VISION_GOGGLES.get().use(level, player, InteractionHand.MAIN_HAND);
        helper.assertTrue(
                !player.getItemBySlot(EquipmentSlot.HEAD)
                        .getOrDefault(ModDataComponents.NIGHT_VISION_ACTIVE.get(), true),
                "The second toggle must clear the night vision component");
        NightVisionHelper.onPlayerTick(new PlayerTickEvent.Post(player));
        helper.assertTrue(
                ElectricItemEnergy.charge(player.getItemBySlot(EquipmentSlot.HEAD)) == 99,
                "A disabled helmet must stop draining");
        if (skylight <= 8) {
            helper.assertTrue(
                    !player.hasEffect(MobEffects.BLINDNESS),
                    "Indoors the effect must be night vision, never blindness");
        }
        helper.succeed();
    }

    private NanoArmorTests() {}
}
