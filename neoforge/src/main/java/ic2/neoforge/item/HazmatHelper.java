package ic2.neoforge.item;

import ic2.neoforge.effect.RadiationEffect;
import ic2.neoforge.energy.WorldEnergyNetworks;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Legacy EventHandler protection hooks: a player wearing a complete hazmat suit ignores fire,
 * electricity and radiation damage (the fire case also extinguishes the player).
 */
public final class HazmatHelper {
    /** Legacy ItemArmorHazmat.hazmatAbsorbs: damage sources a complete suit cancels. */
    public static boolean hazmatAbsorbs(DamageSource source) {
        return source.is(DamageTypeTags.IS_FIRE)
                || source.is(RadiationEffect.RADIATION_TYPE)
                || source.is(WorldEnergyNetworks.ELECTRICITY_TYPE);
    }

    /** Legacy EventHandler.onEntityAttacked protection branch. */
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (!hazmatAbsorbs(event.getSource())) return;
        if (!HazmatLike.hasCompleteHazmat(event.getEntity())) return;
        if (event.getSource().is(DamageTypeTags.IS_FIRE)) {
            event.getEntity().setRemainingFireTicks(0);
        }
        event.setAmount(0);
    }

    private HazmatHelper() {}
}
