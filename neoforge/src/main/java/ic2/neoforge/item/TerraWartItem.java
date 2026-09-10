package ic2.neoforge.item;

import ic2.neoforge.registration.ModEffects;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Legacy ItemTerraWart: eating a terra wart strips the ailments (including the IC2 radiation
 * effect, shortened by 600 ticks instead of fully removed when it ran long).
 */
public class TerraWartItem extends Item {
    // Legacy 1.20 names: CONFUSION/DIG_SLOWDOWN/MOVEMENT_SLOWDOWN are nausea/mining fatigue/
    // slowness by registry id.
    private static final List<Holder<MobEffect>> CURED_EFFECTS =
            List.of(
                    MobEffects.NAUSEA,
                    MobEffects.MINING_FATIGUE,
                    MobEffects.HUNGER,
                    MobEffects.SLOWNESS,
                    MobEffects.WEAKNESS,
                    MobEffects.BLINDNESS,
                    MobEffects.POISON,
                    MobEffects.WITHER);

    public TerraWartItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        for (Holder<MobEffect> effect : CURED_EFFECTS) {
            entity.removeEffect(effect);
        }
        MobEffectInstance radiation = entity.getEffect(ModEffects.RADIATION);
        if (radiation != null) {
            entity.removeEffect(ModEffects.RADIATION);
            if (radiation.getDuration() > 600) {
                entity.addEffect(
                        new MobEffectInstance(
                                ModEffects.RADIATION,
                                radiation.getDuration() - 600,
                                radiation.getAmplifier()));
            }
        }
        return super.finishUsingItem(stack, level, entity);
    }
}
