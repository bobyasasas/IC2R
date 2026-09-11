package ic2.neoforge.item;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Legacy ItemArmorElectric.damageArmor and the EventHandler fall hook: each worn electric armour
 * piece eats its slot's share of incoming damage for energyPerDamage EU per point, and charged
 * nano/quantum boots cancel falls the suit can pay for.
 */
public final class ElectricArmorHelper {
    /** Legacy iteration order: feet first, matching EquipmentSlot.values() order. */
    private static final EquipmentSlot[] ARMOR_SLOTS = {
        EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD
    };

    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        float amount = event.getAmount();
        if (amount <= 0.0F) return;
        if (event.getSource().is(DamageTypeTags.BYPASSES_ENCHANTMENTS)) return;
        if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;

        float remainingDamage = amount;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!(stack.getItem() instanceof ElectricArmorItem armor)) continue;

            double absorptionRatio =
                    ElectricArmorItem.baseAbsorptionRatio(slot) * armor.damageAbsorptionRatio();
            if (absorptionRatio <= 0.0) continue;
            int energyPerDamage = armor.energyPerDamage();
            if (energyPerDamage <= 0) continue;

            double availableEnergy = ElectricItemEnergy.charge(stack);
            double maxAbsorbDamage = availableEnergy / energyPerDamage;
            double absorbedDamage = Math.min(remainingDamage * absorptionRatio, maxAbsorbDamage);
            if (absorbedDamage <= 0.0) continue;

            ElectricItemEnergy.discharge(
                    stack,
                    absorbedDamage * energyPerDamage,
                    Integer.MAX_VALUE,
                    true,
                    false,
                    false);
            remainingDamage -= (float) absorbedDamage;
            if (remainingDamage <= 0.0F) break;
        }

        event.setAmount(remainingDamage);
    }

    /** Legacy EventHandler.onLivingFall nano branch (the rubber boots one is dead legacy code). */
    public static void onLivingFall(LivingFallEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) return;

        ItemStack boots = entity.getItemBySlot(EquipmentSlot.FEET);
        if (boots.getItem() instanceof NanoSuitItem nanoBoots
                && nanoBoots.absorbFall(boots, (float) event.getDistance())) {
            event.setCanceled(true);
        }
    }

    private ElectricArmorHelper() {}
}
