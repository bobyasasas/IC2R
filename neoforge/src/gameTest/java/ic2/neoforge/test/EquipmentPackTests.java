package ic2.neoforge.test;

import com.mojang.authlib.GameProfile;

import ic2.core.energy.ElectricItemSpec;
import ic2.core.machine.SolarGeneration;
import ic2.neoforge.component.ModDataComponents;
import ic2.neoforge.item.ElectricItemEnergy;
import ic2.neoforge.item.JetpackItem;
import ic2.neoforge.item.JetpackLogic;
import ic2.neoforge.item.JetpackLike;
import ic2.neoforge.item.UtilityArmorHelper;
import ic2.neoforge.registration.ModArmor;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LightLayer;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.UUID;

/**
 * The remaining legacy wearable equipment: Lappack/Energypack specs with external discharge, the
 * solar helmet and static boots charging whatever is worn on the chest, and both jetpacks flying
 * on the shared JetpackLogic physics with per-item thrust parameters.
 */
final class EquipmentPackTests {
    private static double armorAmount(ItemAttributeModifiers modifiers) {
        double sum = 0;
        for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
            if (entry.attribute().value() == Attributes.ARMOR.value()) {
                sum += entry.modifier().amount();
            }
        }
        return sum;
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

    static void equipmentSpecs(GameTestHelper helper) {
        helper.assertTrue(
                ModArmor.LAPACK.get()
                        .specification()
                        .equals(new ElectricItemSpec(20000000, 2500, 4, true)),
                "Lappack must keep the legacy 20M/2500/t4 spec with external output");
        helper.assertTrue(
                ModArmor.ENERGY_PACK.get()
                        .specification()
                        .equals(new ElectricItemSpec(2000000, 1000, 3, true)),
                "Energypack must keep the legacy 2M/1000/t3 spec with external output");
        helper.assertTrue(
                ModArmor.JETPACK_ELECTRIC.get()
                        .specification()
                        .equals(new ElectricItemSpec(30000, 60, 1, false)),
                "Electric jetpack must keep the legacy 30000/60/t1 spec");

        var lap = ModArmor.LAPACK.toStack();
        var statics = lap.getOrDefault(
                net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS,
                ItemAttributeModifiers.EMPTY);
        helper.assertTrue(
                armorAmount(statics) == 8.0,
                "Lappack must carry the legacy 8-point chest defence");
        var helmet = ModArmor.SOLAR_HELMET_ITEM.toStack();
        helper.assertTrue(
                armorAmount(helmet.getOrDefault(
                        net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS,
                        ItemAttributeModifiers.EMPTY))
                        == 3.0,
                "Solar helmet must carry the legacy 3-point defence");
        var boots = ModArmor.STATIC_BOOTS_ITEM.toStack();
        helper.assertTrue(
                armorAmount(boots.getOrDefault(
                        net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS,
                        ItemAttributeModifiers.EMPTY))
                        == 3.0,
                "Static boots must carry the legacy 3-point defence");

        // Electric jetpack charge level is a 0..1 EU ratio; the biogas jetpack's is millibuckets.
        var jetpackElectric = ModArmor.JETPACK_ELECTRIC.toStack();
        ElectricItemEnergy.charge(jetpackElectric, 15000, 1, true, false);
        JetpackLike like = (JetpackLike) jetpackElectric.getItem();
        helper.assertTrue(
                Math.abs(like.chargeLevel(jetpackElectric) - 0.5) < 1e-9,
                "Half tank must report charge level 0.5");

        var classic = ModArmor.JETPACK.toStack();
        JetpackItem.fillMb(classic, 30000);
        helper.assertTrue(
                JetpackItem.getContentsMb(classic) == 30000,
                "The biogas tank must hold 30000 mB");
        helper.assertTrue(
                JetpackItem.drainMb(classic, 40000) == 0,
                "Draining beyond the tank contents must be refused entirely");
        helper.assertTrue(
                JetpackItem.drainMb(classic, 2) == 2
                        && JetpackItem.getContentsMb(classic) == 29998,
                "A covered drain must pay exactly the amount");
        helper.succeed();
    }

    static void solarHelmetChargesChest(GameTestHelper helper) {
        helper.setTime(6000);
        // Open-sky corner of the test footprint (the machine room roof keeps (2,2,2) dark).
        var open = helper.absolutePos(new BlockPos(8, 2, 8));
        Player player = fakePlayer(helper, "ic2-solar");
        player.setPos(open.getX() + 0.5, open.getY(), open.getZ() + 0.5);

        var chest = ModArmor.BATPACK.toStack();
        wear(player, chest, EquipmentSlot.CHEST);
        wear(player, ModArmor.SOLAR_HELMET_ITEM.toStack(), EquipmentSlot.HEAD);

        helper.startSequence()
                .thenWaitUntil(
                        () -> {
                            UtilityArmorHelper.onPlayerTick(new PlayerTickEvent.Post(player));
                            helper.assertTrue(
                                    ElectricItemEnergy.charge(player.getItemBySlot(EquipmentSlot.CHEST))
                                            > 0,
                                    "Daylight on the helmet must charge the worn chest piece");
                        })
                .thenExecute(
                        () -> {
                            var level = (ServerLevel) helper.getLevel();
                            // The exact per-tick amount is the generator brightness at the
                            // player's position, evaluated on the same tick.
                            double expected =
                                    SolarGeneration.brightness(
                                            level.dimensionType().hasSkyLight(),
                                            level.getBrightness(
                                                    LightLayer.SKY,
                                                    BlockPos.containing(player.position())),
                                            level.environmentAttributes()
                                                    .getValue(
                                                            EnvironmentAttributes.SUN_ANGLE,
                                                            BlockPos.containing(player.position())),
                                            false,
                                            level.getRainLevel(1),
                                            level.getThunderLevel(1));
                            double before =
                                    ElectricItemEnergy.charge(player.getItemBySlot(EquipmentSlot.CHEST));
                            UtilityArmorHelper.onPlayerTick(new PlayerTickEvent.Post(player));
                            double after =
                                    ElectricItemEnergy.charge(player.getItemBySlot(EquipmentSlot.CHEST));
                            helper.assertTrue(
                                    Math.abs((after - before) - expected) < 1e-9,
                                    "The tick must charge exactly the brightness " + expected
                                            + " (got " + (after - before) + ")");
                        })
                .thenSucceed();
    }

    static void staticBootsChargesChest(GameTestHelper helper) {
        Player player = fakePlayer(helper, "ic2-static");
        var base = helper.absolutePos(new BlockPos(2, 2, 2));
        player.setPos(base.getX() + 0.5, base.getY(), base.getZ() + 0.5);
        var chest = ModArmor.BATPACK.toStack();
        wear(player, chest, EquipmentSlot.CHEST);
        var boots = ModArmor.STATIC_BOOTS_ITEM.toStack();
        wear(player, boots, EquipmentSlot.FEET);

        // First tick plants the walk markers without charging.
        UtilityArmorHelper.onPlayerTick(new PlayerTickEvent.Post(player));
        helper.assertTrue(
                ElectricItemEnergy.charge(player.getItemBySlot(EquipmentSlot.CHEST)) == 0,
                "Marker initialization must not charge");
        helper.assertTrue(
                boots.has(ModDataComponents.STATIC_BOOTS_X.get())
                        && boots.has(ModDataComponents.STATIC_BOOTS_Z.get()),
                "The first tick must plant the walk markers");

        // Five blocks of travel pay min(3, 5/5) = 1 EU.
        player.setPos(base.getX() + 5.5, base.getY(), base.getZ() + 0.5);
        UtilityArmorHelper.onPlayerTick(new PlayerTickEvent.Post(player));
        double charge = ElectricItemEnergy.charge(player.getItemBySlot(EquipmentSlot.CHEST));
        helper.assertTrue(
                Math.abs(charge - 1.0) < 1e-9,
                "Five blocks must charge exactly 1 EU (got " + charge + ")");

        // Another ten blocks pay 2 more; the markers moved with the last charge.
        player.setPos(base.getX() + 15.5, base.getY(), base.getZ() + 0.5);
        UtilityArmorHelper.onPlayerTick(new PlayerTickEvent.Post(player));
        charge = ElectricItemEnergy.charge(player.getItemBySlot(EquipmentSlot.CHEST));
        helper.assertTrue(
                Math.abs(charge - 3.0) < 1e-9,
                "Ten more blocks must charge 2 more EU (got " + charge + ")");

        // Riding freezes the markers: no travel, no charge.
        var stand = new net.minecraft.world.entity.decoration.ArmorStand(
                helper.getLevel(), base.getX() + 25.5, base.getY(), base.getZ() + 0.5);
        helper.getLevel().addFreshEntity(stand);
        player.startRiding(stand, true, true);
        UtilityArmorHelper.onPlayerTick(new PlayerTickEvent.Post(player));
        charge = ElectricItemEnergy.charge(player.getItemBySlot(EquipmentSlot.CHEST));
        helper.assertTrue(
                Math.abs(charge - 3.0) < 1e-9,
                "Riding must freeze the walk markers (got " + charge + ")");
        player.stopRiding();
        stand.discard();
        helper.succeed();
    }

    static void jetpackElectricFlight(GameTestHelper helper) {
        var level = helper.getLevel();
        Player player = fakePlayer(helper, "ic2-jet-e");
        var inside = helper.absolutePos(new BlockPos(2, 2, 2));
        player.setPos(inside.getX() + 0.5, inside.getY(), inside.getZ() + 0.5);

        var jetpack = ModArmor.JETPACK_ELECTRIC.toStack();
        ElectricItemEnergy.charge(jetpack, 30000, 1, true, false);
        // Legacy flipped hover mode from client keys; the port arms flight with sneak + use.
        player.setShiftKeyDown(true);
        player.setItemInHand(InteractionHand.MAIN_HAND, jetpack);
        helper.assertTrue(
                ModArmor.JETPACK_ELECTRIC.get().use(level, player, InteractionHand.MAIN_HAND)
                        == InteractionResult.SUCCESS,
                "Sneak + use must arm the electric jetpack");
        player.setShiftKeyDown(false);
        helper.assertTrue(
                jetpack.getOrDefault(ModDataComponents.JETPACK_ACTIVE.get(), false),
                "The toggle must set the active flag");

        wear(player, jetpack, EquipmentSlot.CHEST);
        player.setOnGround(false);
        player.fallDistance = 3.0;
        JetpackLogic.onPlayerTick(new PlayerTickEvent.Post(player));
        // Replicate the shared physics: thrust 0.7, no ramp above the drop fraction, and this
        // test dimension's low ceiling under the 1.28 divisor.
        float power = 0.7F;
        int maxFlightHeight = (int) (level.getMaxY() / 1.28F);
        double py = player.getY();
        if (py > maxFlightHeight - 25) {
            power = (float) (power * ((maxFlightHeight - Math.min(py, maxFlightHeight)) / 25.0));
        }
        double expectedVy = Math.min(power * 0.2F, 0.6F);
        double vy = player.getDeltaMovement().y;
        helper.assertTrue(
                Math.abs(vy - expectedVy) < 1e-6,
                "Electric thrust must add power * 0.2 (" + expectedVy + ") (got " + vy + ")");
        helper.assertTrue(
                ElectricItemEnergy.charge(jetpack) == 29992,
                "Airborne thrust must pay 2 EU plus the legacy +6 quirk (got "
                        + ElectricItemEnergy.charge(jetpack) + ")");

        // Sneaking is the hover stand-in: descend at the hover multiplier for 1 EU.
        player.setShiftKeyDown(true);
        JetpackLogic.onPlayerTick(new PlayerTickEvent.Post(player));
        double hoverVy = player.getDeltaMovement().y;
        helper.assertTrue(
                Math.abs(hoverVy - -0.1) < 1e-6,
                "Sneaking must clamp the descent to -0.1 (got " + hoverVy + ")");
        helper.assertTrue(
                ElectricItemEnergy.charge(jetpack) == 29985,
                "Hovering must pay 1 EU plus the legacy +6 quirk (got "
                        + ElectricItemEnergy.charge(jetpack) + ")");

        // The 5% tank ramp scales the thrust with the remaining fraction.
        player.setOnGround(true);
        player.setShiftKeyDown(false);
        ElectricItemEnergy.discharge(jetpack, 29000, Integer.MAX_VALUE, true, false, false);
        double tank = ElectricItemEnergy.charge(jetpack);
        JetpackLogic.onPlayerTick(new PlayerTickEvent.Post(player));
        helper.assertTrue(
                ElectricItemEnergy.charge(jetpack) == tank,
                "Grounded thrust must not drain");
        player.setDeltaMovement(0, 0, 0);
        player.setOnGround(false);
        JetpackLogic.onPlayerTick(new PlayerTickEvent.Post(player));
        double rampedVy = player.getDeltaMovement().y;
        double expectedRamped =
                Math.min((float) (0.7F * (tank / 30000.0 / 0.05)) * 0.2F, 0.6F);
        helper.assertTrue(
                Math.abs(rampedVy - expectedRamped) < 1e-6,
                "The sub-5% ramp must scale thrust (" + expectedRamped + ") (got " + rampedVy
                        + ")");
        helper.succeed();
    }

    static void jetpackClassicFlight(GameTestHelper helper) {
        var level = helper.getLevel();
        Player player = fakePlayer(helper, "ic2-jet-b");
        var inside = helper.absolutePos(new BlockPos(2, 2, 2));
        player.setPos(inside.getX() + 0.5, inside.getY(), inside.getZ() + 0.5);

        var jetpack = ModArmor.JETPACK.toStack();
        JetpackItem.fillMb(jetpack, JetpackItem.CAPACITY_MB);
        player.setShiftKeyDown(true);
        player.setItemInHand(InteractionHand.MAIN_HAND, jetpack);
        ModArmor.JETPACK.get().use(level, player, InteractionHand.MAIN_HAND);
        player.setShiftKeyDown(false);
        helper.assertTrue(
                jetpack.getOrDefault(ModDataComponents.JETPACK_ACTIVE.get(), false),
                "Sneak + use must arm the biogas jetpack");

        wear(player, jetpack, EquipmentSlot.CHEST);
        player.setOnGround(false);
        JetpackLogic.onPlayerTick(new PlayerTickEvent.Post(player));
        float power = 1.0F;
        int maxFlightHeight = (int) (level.getMaxY() / 1.0F);
        double py = player.getY();
        if (py > maxFlightHeight - 25) {
            power = (float) (power * ((maxFlightHeight - Math.min(py, maxFlightHeight)) / 25.0));
        }
        double expectedVy = Math.min(power * 0.2F, 0.6F);
        double vy = player.getDeltaMovement().y;
        helper.assertTrue(
                Math.abs(vy - expectedVy) < 1e-6,
                "Biogas thrust must add power * 0.2 (" + expectedVy + ") (got " + vy + ")");
        helper.assertTrue(
                JetpackItem.getContentsMb(jetpack) == 29998,
                "Airborne thrust must drain 2 mB (got " + JetpackItem.getContentsMb(jetpack) + ")");

        // Legacy drainEnergy refuses the whole tick unless the tank covers the drain: with
        // 1 mB left an airborne non-hover tick wants 2 and pays nothing.
        JetpackItem.drainMb(jetpack, 29997);
        JetpackLogic.onPlayerTick(new PlayerTickEvent.Post(player));
        helper.assertTrue(
                JetpackItem.getContentsMb(jetpack) == 1,
                "A tank below the drain must pay nothing (got "
                        + JetpackItem.getContentsMb(jetpack) + ")");

        // Empty tank: charge level 0 aborts before any motion or drain.
        JetpackItem.drainMb(jetpack, 1);
        double stalled = player.getDeltaMovement().y;
        JetpackLogic.onPlayerTick(new PlayerTickEvent.Post(player));
        helper.assertTrue(
                player.getDeltaMovement().y == stalled,
                "An empty tank must not thrust or move (motion y "
                        + player.getDeltaMovement().y + ")");
        helper.succeed();
    }

    private EquipmentPackTests() {}
}
