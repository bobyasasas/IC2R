package ic2.neoforge;

import ic2.neoforge.component.ModDataComponents;
import ic2.neoforge.registration.ModItems;
import ic2.neoforge.registration.ModSounds;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

/** Composition root: platform registration belongs here, machine rules belong in core. */
@Mod(IndustrialCraft.MOD_ID)
public final class IndustrialCraft {
    public static final String MOD_ID = "ic2";

    public IndustrialCraft(IEventBus modBus) {
        ModDataComponents.register(modBus);
        ModItems.register(modBus);
        ModSounds.register(modBus);
    }
}
