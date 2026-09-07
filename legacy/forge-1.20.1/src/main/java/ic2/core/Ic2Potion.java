package ic2.core;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

import org.jetbrains.annotations.NotNull;

public class Ic2Potion extends MobEffect {
    public static Ic2Potion radiation;

    public Ic2Potion(MobEffectCategory type, int liquidColor) {
        super(type, liquidColor);
    }

    @Override
    public void applyEffectTick(@NotNull LivingEntity entity, int amplifier) {
        if (this == radiation) {
            if (Ic2DamageSource.radiation == null) {
                Ic2DamageSource.init(entity.level().registryAccess());
            }

            entity.hurt(Ic2DamageSource.radiation, amplifier / 100.0F + 0.5F);
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        if (this != radiation) {
            return false;
        }

        int rate = 25 >> amplifier;
        return rate == 0 || duration % rate == 0;
    }

    public void applyTo(LivingEntity entity, int duration, int amplifier) {
        MobEffectInstance effect = new MobEffectInstance(radiation, duration, amplifier);
        entity.addEffect(effect);
    }
}
