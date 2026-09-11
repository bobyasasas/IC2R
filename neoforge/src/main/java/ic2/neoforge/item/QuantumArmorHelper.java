package ic2.neoforge.item;

import ic2.neoforge.component.ModDataComponents;
import ic2.neoforge.registration.ModEffects;

import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Legacy ItemArmorQuantumSuit inventoryTick plus JetpackLogic: helmet air/food/potion
 * life-support, chest fire clearing and flight, legs sprint boost, feet super jump. The legacy
 * followed client keys (jump/boost/forward); the port engages each feature through its piece
 * flag (sneak + use) and uses sprinting as the forward/boost stand-in.
 */
public final class QuantumArmorHelper {
    /** Legacy potionRemovalCost: EU to purge one effect (radiation adds amplifier*100). */
    private static final Map<net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>, Integer>
            POTION_REMOVAL_COST = new IdentityHashMap<>();

    static {
        POTION_REMOVAL_COST.put(MobEffects.POISON, 10000);
        POTION_REMOVAL_COST.put(ModEffects.RADIATION, 10000);
        POTION_REMOVAL_COST.put(MobEffects.WITHER, 25000);
    }

    // Legacy jumpCharges; the on_ground flag lives here too (legacy kept it in item NBT).
    private static final Map<Player, Float> JUMP_CHARGES = new WeakHashMap<>();
    private static final Map<Player, Boolean> WAS_ON_GROUND = new WeakHashMap<>();

    private static final int AIR_REFILL_COST = 1000;
    private static final int AIR_REFILL_AMOUNT = 200;
    private static final int AUTO_FEED_COST = 1000;
    private static final int SPEED_COST = 1000;
    private static final int SPEED_TICKS = 10;
    private static final int JUMP_COST = 4000;

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        if (helmet.getItem() instanceof QuantumSuitItem) {
            lifeSupport(player, helmet);
        }

        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (chest.getItem() instanceof QuantumSuitItem) {
            chestTick(player, chest);
        }

        ItemStack legs = player.getItemBySlot(EquipmentSlot.LEGS);
        if (legs.getItem() instanceof QuantumSuitItem) {
            speedTick(player, legs);
        }

        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
        if (boots.getItem() instanceof QuantumSuitItem) {
            jumpTick(player, boots);
        }
    }

    /** Legacy helmet tick: air refill, tin-can auto feed and paid effect purging. */
    private static void lifeSupport(Player player, ItemStack helmet) {
        int maxAir = player.getMaxAirSupply();
        if (player.getAirSupply() < maxAir && canUse(helmet, AIR_REFILL_COST)) {
            player.setAirSupply(Math.min(maxAir, player.getAirSupply() + AIR_REFILL_AMOUNT));
            ElectricItemEnergy.use(helmet, AIR_REFILL_COST, player);
        }

        if (player.getFoodData().needsFood() && canUse(helmet, AUTO_FEED_COST)) {
            if (TinCanItem.eatFromInventory(player)) {
                ElectricItemEnergy.use(helmet, AUTO_FEED_COST, player);
            }
        }

        for (MobEffectInstance effect : List.copyOf(player.getActiveEffects())) {
            Integer baseCost = POTION_REMOVAL_COST.get(effect.getEffect());
            if (baseCost == null) continue;
            int cost = removalCost(effect.getEffect(), effect, baseCost);
            if (canUse(helmet, cost)) {
                ElectricItemEnergy.use(helmet, cost, player);
                player.removeEffect(effect.getEffect());
            }
        }
    }

    /** Legacy getPotionRemovalCost: radiation scales by +100 per amplifier, others multiply. */
    private static int removalCost(
            net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> potion,
            MobEffectInstance effect,
            int baseCost) {
        if (potion == ModEffects.RADIATION) {
            return baseCost + effect.getAmplifier() * 100;
        }

        return baseCost * (effect.getAmplifier() + 1);
    }

    /** Legacy chest tick: the suit clears fire for free, flight drains the JetpackLogic budget. */
    private static void chestTick(Player player, ItemStack chest) {
        player.clearFire();
        if (!chest.getOrDefault(ModDataComponents.JETPACK_ACTIVE.get(), false)) return;
        useJetpack(player, chest);
    }

    /**
     * Legacy JetpackLogic.useJetpack with the quantum values (power 1.0, drop 5%, height
     * divisor 0.9). Hover mode is replaced by sneaking (slow descent); sprinting stands in for
     * the legacy forward thruster.
     */
    private static void useJetpack(Player player, ItemStack stack) {
        double chargeLevel = ElectricItemEnergy.charge(stack) / 10000000.0;
        if (chargeLevel <= 0.0) return;

        float power = 1.0F;
        if (chargeLevel <= 0.05) {
            power = (float) (power * (chargeLevel / 0.05));
        }

        Vec3 motion = player.getDeltaMovement();
        double motionX = motion.x;
        double motionY = motion.y;
        double motionZ = motion.z;
        if (player.isSprinting()) {
            float forwarder = power * 0.15F * 2.0F;
            float yaw = player.getYRot() * (float) Math.PI / 180.0F;
            float thrust = 0.4F * forwarder * 0.02F;
            motionX += -Math.sin(yaw) * thrust;
            motionZ += Math.cos(yaw) * thrust;
        }

        int maxFlightHeight = (int) (player.level().getMaxY() / 0.9F);
        double y = player.getY();
        if (y > maxFlightHeight - 25) {
            if (y > maxFlightHeight) {
                y = maxFlightHeight;
            }

            power = (float) (power * ((maxFlightHeight - y) / 25.0));
        }

        motionY = Math.min(motionY + power * 0.2F, 0.6F);
        if (player.isShiftKeyDown() && motionY > -0.1F) {
            motionY = -0.1F;
        }

        player.setDeltaMovement(motionX, motionY, motionZ);
        int consume = player.isShiftKeyDown() ? 1 : 2;
        if (!player.onGround()) {
            drainEnergy(stack, consume);
        }

        player.resetFallDistance();
    }

    /** Legacy IJetpack.drainEnergy: one extra EU on every discharge (the legacy +6 quirk). */
    private static void drainEnergy(ItemStack stack, int amount) {
        ElectricItemEnergy.discharge(stack, amount + 6, Integer.MAX_VALUE, true, false, false);
    }

    /** Legacy legs tick: sprinting forward on ground or in water pushes the player for EU. */
    private static void speedTick(Player player, ItemStack legs) {
        if (!legs.getOrDefault(ModDataComponents.SPEED_ENABLED.get(), true)) return;
        if (!player.onGround() && !player.isInWater()) return;
        if (!player.isSprinting()) return;
        if (!canUse(legs, SPEED_COST)) return;

        int ticker = legs.getOrDefault(ModDataComponents.SPEED_TICKER.get(), 0) + 1;
        if (ticker >= SPEED_TICKS) {
            ticker = 0;
            ElectricItemEnergy.use(legs, SPEED_COST, player);
        }

        legs.set(ModDataComponents.SPEED_TICKER.get(), ticker);
        float speed = player.isInWater() ? 0.1F : 0.22F;
        float yawRad = player.getYRot() * Mth.DEG_TO_RAD;
        Vec3 motion = player.getDeltaMovement();
        player.setDeltaMovement(
                motion.x + (-Mth.sin(yawRad) * speed),
                motion.y,
                motion.z + (Mth.cos(yawRad) * speed));
    }

    /** Legacy feet tick: charged takeoffs super-jump upward for 4000 EU, fading per tick. */
    private static void jumpTick(Player player, ItemStack boots) {
        if (!boots.getOrDefault(ModDataComponents.JUMP_ENABLED.get(), true)) return;

        boolean wasOnGround = WAS_ON_GROUND.getOrDefault(player, true);
        if (wasOnGround && !player.onGround() && JUMP_CHARGES.getOrDefault(player, 0.0F) > 0.0F) {
            ElectricItemEnergy.use(boots, JUMP_COST, player);
        }

        if (player.onGround() != wasOnGround) {
            WAS_ON_GROUND.put(player, player.onGround());
        }

        if (canUse(boots, JUMP_COST) && player.onGround()) {
            JUMP_CHARGES.put(player, 1.0F);
        }

        float jumpCharge = JUMP_CHARGES.getOrDefault(player, 0.0F);
        Vec3 motion = player.getDeltaMovement();
        if (motion.y >= 0.0 && jumpCharge > 0.0F && !player.isInWater()) {
            double x = motion.x;
            double z = motion.z;
            if (jumpCharge == 1.0F && player.isSprinting()) {
                x *= 3.5;
                z *= 3.5;
            }

            player.setDeltaMovement(x, motion.y + jumpCharge * 0.3F, z);
            JUMP_CHARGES.put(player, jumpCharge * 0.75F);
        } else if (motion.y < 0.0 && jumpCharge < 1.0F) {
            JUMP_CHARGES.put(player, 0.0F);
        }
    }

    private static boolean canUse(ItemStack stack, double amount) {
        return ElectricItemEnergy.charge(stack) >= amount;
    }

    private QuantumArmorHelper() {}
}
