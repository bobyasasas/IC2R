package ic2.neoforge.test;

import com.mojang.authlib.GameProfile;

import ic2.core.energy.ElectricItemSpec;
import ic2.neoforge.component.ModDataComponents;
import ic2.neoforge.item.ElectricItemEnergy;
import ic2.neoforge.item.QuantumArmorHelper;
import ic2.neoforge.item.QuantumSuitItem;
import ic2.neoforge.registration.ModArmor;
import ic2.neoforge.registration.ModEffects;
import ic2.neoforge.registration.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Legacy quantum suit: spec and charged protection per slot, 20000-EU-per-point absorption with
 * the chest 1.2 ratio, helmet life support (air, tin cans, paid poison/radiation/wither purging),
 * chest jetpack flight, legs sprint boost and feet super jump plus uncapped fall absorption.
 */
final class QuantumArmorTests {
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

    static void specAndAttributes(GameTestHelper helper) {
        var legacySpec = new ElectricItemSpec(10000000, 12000, 4, false);
        helper.assertTrue(
                ModArmor.QUANTUM_HELMET.get().specification().equals(legacySpec)
                        && ModArmor.QUANTUM_CHESTPLATE.get().specification().equals(legacySpec)
                        && ModArmor.QUANTUM_LEGGINGS.get().specification().equals(legacySpec)
                        && ModArmor.QUANTUM_BOOTS.get().specification().equals(legacySpec),
                "All four quantum pieces must keep the 10000000/12000/t4 spec");
        helper.assertTrue(
                QuantumSuitItem.ENERGY_PER_DAMAGE == 20000,
                "The suit must spend 20000 EU per absorbed damage point");

        var helmet = ModArmor.QUANTUM_HELMET.toStack();
        helper.assertTrue(
                armorAmount(ModArmor.QUANTUM_HELMET.get().getDefaultAttributeModifiers(helmet))
                        == 0.0,
                "Uncharged quantum armour must fall back to the material's zero defence");
        ElectricItemEnergy.charge(helmet, 25000, 4, true, false);
        var charged = ModArmor.QUANTUM_HELMET.get().getDefaultAttributeModifiers(helmet);
        helper.assertTrue(
                armorAmount(charged) == 3.0
                        && toughnessAmounts(charged).equals(List.of(2.0)),
                "Charged helmet must show CHARGED_PROTECTION 3 with 2.0 toughness");

        var chest = ModArmor.QUANTUM_CHESTPLATE.toStack();
        ElectricItemEnergy.charge(chest, 25000, 4, true, false);
        helper.assertTrue(
                armorAmount(ModArmor.QUANTUM_CHESTPLATE.get().getDefaultAttributeModifiers(chest))
                        == 8.0,
                "Charged chestplate must show CHARGED_PROTECTION 8");
        var legs = ModArmor.QUANTUM_LEGGINGS.toStack();
        ElectricItemEnergy.charge(legs, 25000, 4, true, false);
        helper.assertTrue(
                armorAmount(ModArmor.QUANTUM_LEGGINGS.get().getDefaultAttributeModifiers(legs))
                        == 6.0,
                "Charged leggings must show CHARGED_PROTECTION 6");
        var boots = ModArmor.QUANTUM_BOOTS.toStack();
        ElectricItemEnergy.charge(boots, 25000, 4, true, false);
        helper.assertTrue(
                armorAmount(ModArmor.QUANTUM_BOOTS.get().getDefaultAttributeModifiers(boots))
                        == 3.0,
                "Charged boots must show CHARGED_PROTECTION 3");

        helper.assertTrue(
                ModArmor.QUANTUM_CHESTPLATE.get().damageAbsorptionRatio() == 1.2
                        && ModArmor.QUANTUM_LEGGINGS.get().damageAbsorptionRatio() == 1.0,
                "Legacy ratio: chest 1.2, every other piece 1.0");

        var dyed = ModArmor.QUANTUM_HELMET.toStack();
        helper.assertTrue(
                !ModArmor.QUANTUM_HELMET.get().hasCustomColor(dyed),
                "A fresh piece must report no dye");
        ModArmor.QUANTUM_HELMET.get().setColor(dyed, 0x00FF00);
        helper.assertTrue(
                ModArmor.QUANTUM_HELMET.get().hasCustomColor(dyed)
                        && ModArmor.QUANTUM_HELMET.get().getColor(dyed)
                                == net.minecraft.util.ARGB.opaque(0x00FF00),
                "The dye must round-trip through DYED_COLOR");
        helper.assertTrue(
                ModArmor.QUANTUM_HELMET.get().addsProtection(null, EquipmentSlot.HEAD, helmet)
                        && !ModArmor.QUANTUM_HELMET.get()
                                .addsProtection(null, EquipmentSlot.HEAD, dyed),
                "Hazmat protection requires charge");
        helper.succeed();
    }

    static void damageAbsorption(GameTestHelper helper) {
        var level = helper.getLevel();
        Player player = fakePlayer(helper, "ic2-quantum-abs");
        var chest = ModArmor.QUANTUM_CHESTPLATE.toStack();
        ElectricItemEnergy.charge(chest, 200000, 4, true, false);
        wear(player, chest, EquipmentSlot.CHEST);

        player.hurt(level.damageSources().generic(), 10.0F);
        // Legacy damageArmor: chest share 0.4 * ratio 1.2 = 4.8 of the 10 damage absorbed
        // for 4.8 * 20000 = 96000 EU; minecraft:generic ignores armour, so the player takes
        // exactly the remaining 5.2.
        double charge = ElectricItemEnergy.charge(player.getItemBySlot(EquipmentSlot.CHEST));
        helper.assertTrue(
                Math.abs(charge - 104000) < 1e-6,
                "The chest must absorb 4.8 damage for 96000 EU (got " + charge + ")");
        float taken = player.getMaxHealth() - player.getHealth();
        helper.assertTrue(
                Math.abs(taken - 5.2F) < 1e-3,
                "Absorption must remove exactly 4.8 of 10 damage (player took " + taken + ")");

        // An empty tank caps the absorption at what the charge can pay for.
        Player poor = fakePlayer(helper, "ic2-quantum-poor");
        var poorLegs = ModArmor.QUANTUM_LEGGINGS.toStack();
        ElectricItemEnergy.charge(poorLegs, 40000, 4, true, false);
        wear(poor, poorLegs, EquipmentSlot.LEGS);
        poor.hurt(level.damageSources().generic(), 10.0F);
        // 40000 EU pays for 2 points; the legs' share of 10 is only 3, so the tank empties.
        double poorCharge =
                ElectricItemEnergy.charge(poor.getItemBySlot(EquipmentSlot.LEGS));
        helper.assertTrue(
                Math.abs(poorCharge) < 1e-6,
                "A nearly empty tank must drain to zero (got " + poorCharge + ")");
        float poorTaken = poor.getMaxHealth() - poor.getHealth();
        helper.assertTrue(
                Math.abs(poorTaken - 8.0F) < 1e-3,
                "Charge-limited absorption must leave 8 of 10 damage (player took " + poorTaken
                        + ")");

        // A bypassing source must skip the energy absorption entirely (legacy gate).
        Player plain = fakePlayer(helper, "ic2-quantum-byp");
        var plainChest = ModArmor.QUANTUM_CHESTPLATE.toStack();
        ElectricItemEnergy.charge(plainChest, 200000, 4, true, false);
        wear(plain, plainChest, EquipmentSlot.CHEST);
        plain.hurt(level.damageSources().genericKill(), 10.0F);
        helper.assertTrue(
                ElectricItemEnergy.charge(plain.getItemBySlot(EquipmentSlot.CHEST)) == 200000,
                "BYPASSES_ENCHANTMENTS sources must not drain the quantum suit");
        helper.succeed();
    }

    static void helmetLifeSupport(GameTestHelper helper) {
        // Air refill: 100 -> 300 for 1000 EU.
        Player swimmer = fakePlayer(helper, "ic2-quantum-air");
        var airHelmet = ModArmor.QUANTUM_HELMET.toStack();
        ElectricItemEnergy.charge(airHelmet, 5000, 4, true, false);
        wear(swimmer, airHelmet, EquipmentSlot.HEAD);
        swimmer.setAirSupply(100);
        swimmer.getFoodData().setFoodLevel(20);
        QuantumArmorHelper.onPlayerTick(new PlayerTickEvent.Post(swimmer));
        helper.assertTrue(
                swimmer.getAirSupply() == swimmer.getMaxAirSupply(),
                "The helmet must refill air toward the 300-tick maximum");
        helper.assertTrue(
                ElectricItemEnergy.charge(swimmer.getItemBySlot(EquipmentSlot.HEAD)) == 4000,
                "Air refill must cost exactly 1000 EU");

        // Tin-can auto feed: three cans lift hunger 10 -> 13 for 1000 EU.
        Player eater = fakePlayer(helper, "ic2-quantum-food");
        var foodHelmet = ModArmor.QUANTUM_HELMET.toStack();
        ElectricItemEnergy.charge(foodHelmet, 5000, 4, true, false);
        eater.getFoodData().setFoodLevel(10);
        eater.getInventory().add(new ItemStack(ModItems.FILLED_TIN_CAN.get(), 3));
        wear(eater, foodHelmet, EquipmentSlot.HEAD);
        QuantumArmorHelper.onPlayerTick(new PlayerTickEvent.Post(eater));
        FoodData food = eater.getFoodData();
        helper.assertTrue(
                food.getFoodLevel() == 13,
                "The helmet must auto-eat filled tin cans (food level " + food.getFoodLevel()
                        + ")");
        helper.assertTrue(
                ElectricItemEnergy.charge(eater.getItemBySlot(EquipmentSlot.HEAD)) == 4000,
                "Auto feed must cost exactly 1000 EU");

        // Paid purging: poison amp 2 = 30000, radiation amp 1 = 10100, wither amp 1 = 50000.
        Player sick = fakePlayer(helper, "ic2-quantum-sick");
        var sickHelmet = ModArmor.QUANTUM_HELMET.toStack();
        ElectricItemEnergy.charge(sickHelmet, 100000, 4, true, false);
        sick.addEffect(new MobEffectInstance(MobEffects.POISON, 400, 2));
        sick.addEffect(new MobEffectInstance(ModEffects.RADIATION, 400, 1));
        sick.addEffect(new MobEffectInstance(MobEffects.WITHER, 400, 1));
        sick.addEffect(new MobEffectInstance(MobEffects.SPEED, 400, 0));
        wear(sick, sickHelmet, EquipmentSlot.HEAD);
        QuantumArmorHelper.onPlayerTick(new PlayerTickEvent.Post(sick));
        helper.assertTrue(
                !sick.hasEffect(MobEffects.POISON) && !sick.hasEffect(ModEffects.RADIATION)
                        && !sick.hasEffect(MobEffects.WITHER),
                "The helmet must purge poison, radiation and wither in one tick");
        helper.assertTrue(
                sick.hasEffect(MobEffects.SPEED),
                "Effects outside legacy potionRemovalCost must survive");
        helper.assertTrue(
                ElectricItemEnergy.charge(sick.getItemBySlot(EquipmentSlot.HEAD)) == 9900,
                "Purging must cost exactly 30000 + 10100 + 50000 EU (got "
                        + ElectricItemEnergy.charge(sick.getItemBySlot(EquipmentSlot.HEAD)) + ")");
        helper.succeed();
    }

    static void jetpackFlight(GameTestHelper helper) {
        var level = helper.getLevel();
        Player player = fakePlayer(helper, "ic2-quantum-fly");
        var inside = helper.absolutePos(new BlockPos(2, 2, 2));
        player.setPos(inside.getX() + 0.5, inside.getY(), inside.getZ() + 0.5);

        var chest = ModArmor.QUANTUM_CHESTPLATE.toStack();
        ElectricItemEnergy.charge(chest, 100000, 4, true, false);
        // Legacy flipped hover mode from the client keyboard; the port uses sneak + use.
        player.setShiftKeyDown(true);
        player.setItemInHand(InteractionHand.MAIN_HAND, chest);
        helper.assertTrue(
                ModArmor.QUANTUM_CHESTPLATE.get().use(level, player, InteractionHand.MAIN_HAND)
                        == InteractionResult.SUCCESS,
                "Sneak + use must toggle the jetpack");
        player.setShiftKeyDown(false);
        helper.assertTrue(
                chest.getOrDefault(ModDataComponents.JETPACK_ACTIVE.get(), false),
                "The toggle must arm the jetpack flag");

        wear(player, chest, EquipmentSlot.CHEST);
        player.setOnGround(false);
        player.fallDistance = 3.0;
        QuantumArmorHelper.onPlayerTick(new PlayerTickEvent.Post(player));
        double vy = player.getDeltaMovement().y;
        // Derive the expected thrust exactly like JetpackLogic: the sub-5% charge ramp
        // scales power down, and this test dimension's low ceiling engages the height clamp.
        double chargeLevel = 100000 / 10000000.0;
        float power = 1.0F;
        if (chargeLevel <= 0.05) {
            power = (float) (power * (chargeLevel / 0.05));
        }

        int maxFlightHeight = (int) (level.getMaxY() / 0.9F);
        double py = player.getY();
        if (py > maxFlightHeight - 25) {
            power = (float) (power * ((maxFlightHeight - Math.min(py, maxFlightHeight)) / 25.0));
        }
        double expectedVy = Math.min(power * 0.2F, 0.6F);
        helper.assertTrue(
                Math.abs(vy - expectedVy) < 1e-6,
                "Flight must add power * 0.2 (" + expectedVy + ") to the vertical motion (got "
                        + vy + ")");
        helper.assertTrue(
                player.fallDistance == 0.0,
                "Thrusting must reset the fall distance");
        helper.assertTrue(
                ElectricItemEnergy.charge(player.getItemBySlot(EquipmentSlot.CHEST)) == 99992,
                "Airborne flight must drain 2 EU plus the legacy +6 quirk (got "
                        + ElectricItemEnergy.charge(player.getItemBySlot(EquipmentSlot.CHEST))
                        + ")");

        // Hover mode stand-in: sneaking clamps the descent and halves the burn.
        player.setShiftKeyDown(true);
        QuantumArmorHelper.onPlayerTick(new PlayerTickEvent.Post(player));
        double hoverVy = player.getDeltaMovement().y;
        helper.assertTrue(
                Math.abs(hoverVy - -0.1) < 1e-6,
                "Sneaking in flight must clamp the descent to -0.1 (got " + hoverVy + ")");
        helper.assertTrue(
                ElectricItemEnergy.charge(player.getItemBySlot(EquipmentSlot.CHEST)) == 99985,
                "Hovering must drain 1 EU plus the legacy +6 quirk (got "
                        + ElectricItemEnergy.charge(player.getItemBySlot(EquipmentSlot.CHEST))
                        + ")");

        // Grounded thrust is free until the player leaves the ground.
        player.setOnGround(true);
        player.setShiftKeyDown(false);
        QuantumArmorHelper.onPlayerTick(new PlayerTickEvent.Post(player));
        helper.assertTrue(
                ElectricItemEnergy.charge(player.getItemBySlot(EquipmentSlot.CHEST)) == 99985,
                "Grounded jetpack must not drain");
        helper.succeed();
    }

    static void jumpAndFall(GameTestHelper helper) {
        var level = helper.getLevel();
        Player player = fakePlayer(helper, "ic2-quantum-jump");
        var boots = ModArmor.QUANTUM_BOOTS.toStack();
        ElectricItemEnergy.charge(boots, 10000, 4, true, false);
        wear(player, boots, EquipmentSlot.FEET);

        player.setOnGround(true);
        QuantumArmorHelper.onPlayerTick(new PlayerTickEvent.Post(player));
        double vy = player.getDeltaMovement().y;
        helper.assertTrue(
                Math.abs(vy - 0.3) < 1e-6,
                "A charged takeoff must add 0.3 to the vertical motion (got " + vy + ")");
        helper.assertTrue(
                ElectricItemEnergy.charge(player.getItemBySlot(EquipmentSlot.FEET)) == 10000,
                "Standing on the ground must not drain");

        player.setOnGround(false);
        QuantumArmorHelper.onPlayerTick(new PlayerTickEvent.Post(player));
        helper.assertTrue(
                ElectricItemEnergy.charge(player.getItemBySlot(EquipmentSlot.FEET)) == 6000,
                "Leaving the ground must pay the 4000 EU takeoff cost");
        double boostedVy = player.getDeltaMovement().y;
        helper.assertTrue(
                Math.abs(boostedVy - 0.525) < 1e-6,
                "The jump charge must fade by 0.75 per tick (got " + boostedVy + ")");

        // Direct absorbFall: a fifteen-block fall deals five damage for 100000 EU.
        var fallBoots = ModArmor.QUANTUM_BOOTS.toStack();
        ElectricItemEnergy.charge(fallBoots, 500000, 4, true, false);
        helper.assertTrue(
                ModArmor.QUANTUM_BOOTS.get().absorbFall(fallBoots, 15.0F),
                "Quantum boots must absorb any fall they can pay for");
        helper.assertTrue(
                ElectricItemEnergy.charge(fallBoots) == 400000,
                "A 15-block fall must cost 100000 EU (got "
                        + ElectricItemEnergy.charge(fallBoots) + ")");
        // The unbounded fallback: a 25-block fall deals fifteen damage, beyond the nano cap.
        helper.assertTrue(
                ModArmor.QUANTUM_BOOTS.get().absorbFall(fallBoots, 25.0F),
                "Quantum boots must not carry the nano seven-point cap");
        helper.assertTrue(
                ElectricItemEnergy.charge(fallBoots) == 100000,
                "A 25-block fall must cost 300000 EU (got "
                        + ElectricItemEnergy.charge(fallBoots) + ")");
        helper.assertTrue(
                !ModArmor.QUANTUM_BOOTS.get().absorbFall(fallBoots, 25.0F),
                "A tank below the fall's cost must fail the absorption");
        helper.assertTrue(
                ElectricItemEnergy.charge(fallBoots) == 100000,
                "A failed absorption must not spend energy");

        // The LivingFallEvent wiring must cancel an affordable fall outright.
        Player faller = fakePlayer(helper, "ic2-quantum-fall");
        var worn = ModArmor.QUANTUM_BOOTS.toStack();
        ElectricItemEnergy.charge(worn, 300000, 4, true, false);
        wear(faller, worn, EquipmentSlot.FEET);
        boolean damaged =
                faller.causeFallDamage(25.0, 1.0F, level.damageSources().fall());
        helper.assertTrue(
                !damaged && faller.getHealth() == faller.getMaxHealth(),
                "The fall hook must cancel a fall the boots can pay for");
        helper.assertTrue(
                ElectricItemEnergy.charge(faller.getItemBySlot(EquipmentSlot.FEET)) == 0,
                "The cancelled fall must still drain the boots");
        helper.succeed();
    }

    static void legsSpeedBoost(GameTestHelper helper) {
        Player player = fakePlayer(helper, "ic2-quantum-speed");
        var legs = ModArmor.QUANTUM_LEGGINGS.toStack();
        ElectricItemEnergy.charge(legs, 100000, 4, true, false);
        wear(player, legs, EquipmentSlot.LEGS);

        player.setOnGround(true);
        player.setSprinting(true);
        player.setYRot(0.0F);
        QuantumArmorHelper.onPlayerTick(new PlayerTickEvent.Post(player));
        double dz = player.getDeltaMovement().z;
        helper.assertTrue(
                Math.abs(dz - 0.22) < 1e-6,
                "Sprint boost on ground must push 0.22 along the facing (got " + dz + ")");
        helper.assertTrue(
                legs.getOrDefault(ModDataComponents.SPEED_TICKER.get(), -1) == 1,
                "The boost must tick its legacy counter");
        helper.assertTrue(
                ElectricItemEnergy.charge(legs) == 100000,
                "The first nine boost ticks must be free");

        // Ticks two through nine also ride free; the tenth pays 1000 EU and resets the ticker.
        for (int i = 0; i < 9; i++) {
            QuantumArmorHelper.onPlayerTick(new PlayerTickEvent.Post(player));
        }
        helper.assertTrue(
                legs.getOrDefault(ModDataComponents.SPEED_TICKER.get(), -1) == 0,
                "The tenth tick must reset the counter");
        helper.assertTrue(
                ElectricItemEnergy.charge(legs) == 99000,
                "The tenth boost tick must cost exactly 1000 EU (got "
                        + ElectricItemEnergy.charge(legs) + ")");

        // Disabling the piece flag (legacy key) must stop both push and drain.
        legs.set(ModDataComponents.SPEED_ENABLED.get(), false);
        player.setDeltaMovement(0, 0, 0);
        QuantumArmorHelper.onPlayerTick(new PlayerTickEvent.Post(player));
        helper.assertTrue(
                player.getDeltaMovement().z == 0.0,
                "A disabled boost must not push the player");
        helper.assertTrue(
                ElectricItemEnergy.charge(legs) == 99000,
                "A disabled boost must not drain");
        helper.succeed();
    }

    private QuantumArmorTests() {}
}
