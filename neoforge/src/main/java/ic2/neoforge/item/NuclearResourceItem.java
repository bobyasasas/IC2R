package ic2.neoforge.item;

import ic2.neoforge.registration.ModEffects;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * Legacy ItemNuclearResource: a nuclear-cycle resource that irradiates any carrier without the
 * complete hazmat suit (near-depleted 15s, re-enriched 30s, amplifier 100) and refuses the
 * reactor grid outright.
 */
public class NuclearResourceItem extends Item implements ReactorComponent {
    private final int radiationDuration;
    private final int radiationAmplifier;

    public NuclearResourceItem(Properties properties, int radiationDuration, int radiationAmplifier) {
        super(properties);
        this.radiationDuration = radiationDuration;
        this.radiationAmplifier = radiationAmplifier;
    }

    @Override
    public boolean canBePlacedIn(ItemStack stack, ReactorHost reactor) {
        return false;
    }

    @Override
    public void inventoryTick(
            ItemStack stack,
            net.minecraft.server.level.ServerLevel level,
            Entity owner,
            @Nullable EquipmentSlot slot) {
        super.inventoryTick(stack, level, owner, slot);
        if (owner instanceof LivingEntity living && !HazmatLike.hasCompleteHazmat(living)) {
            living.addEffect(new MobEffectInstance(
                    ModEffects.RADIATION, radiationDuration * 20, radiationAmplifier));
        }
    }
}
