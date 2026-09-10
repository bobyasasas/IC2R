package ic2.neoforge.effect;

import ic2.neoforge.registration.ModEffects;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/**
 * Legacy Ic2Potion.radiation: radiation sickness deals a fraction of a heart every {@code 25 >>
 * amplifier} ticks through the ic2:radiation damage type.
 */
public class RadiationEffect extends MobEffect {
    public static final ResourceKey<DamageType> RADIATION_TYPE =
            ResourceKey.create(
                    Registries.DAMAGE_TYPE, Identifier.fromNamespaceAndPath("ic2", "radiation"));

    public RadiationEffect() {
        super(MobEffectCategory.HARMFUL, 5149489);
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        DamageSource source =
                new DamageSource(level.damageSources().damageTypes.getOrThrow(RADIATION_TYPE));
        return entity.hurtServer(level, source, amplifier / 100.0F + 0.5F);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        int rate = 25 >> amplifier;
        return rate == 0 || duration % rate == 0;
    }

    /** Legacy applyTo: replaces the effect with the given duration and amplifier. */
    public static void applyTo(LivingEntity entity, int duration, int amplifier) {
        entity.addEffect(new MobEffectInstance(ModEffects.RADIATION, duration, amplifier));
    }

    /** The ic2:radiation damage source for direct radiation hurts (reactor heat branch). */
    public static DamageSource radiationSource(ServerLevel level) {
        return new DamageSource(level.damageSources().damageTypes.getOrThrow(RADIATION_TYPE));
    }
}
