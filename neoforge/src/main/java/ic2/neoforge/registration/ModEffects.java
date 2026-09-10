package ic2.neoforge.registration;

import ic2.neoforge.IndustrialCraft;
import ic2.neoforge.effect.RadiationEffect;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Mob effects ported from legacy Ic2Potion. */
public final class ModEffects {
    private static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, IndustrialCraft.MOD_ID);

    public static final DeferredHolder<MobEffect, RadiationEffect> RADIATION =
            EFFECTS.register("radiation", RadiationEffect::new);

    public static void register(IEventBus modBus) {
        EFFECTS.register(modBus);
    }

    private ModEffects() {}
}
