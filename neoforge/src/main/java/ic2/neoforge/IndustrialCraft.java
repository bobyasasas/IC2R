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

    public IndustrialCraft(IEventBus modBus, net.neoforged.fml.ModContainer container) {
        ModDataComponents.register(modBus);
        ModItems.register(modBus);
        ModSounds.register(modBus);
        ic2.neoforge.registration.ModMachines.register(modBus);
        container.registerConfig(
                net.neoforged.fml.config.ModConfig.Type.SERVER,
                ic2.neoforge.energy.EnergyConfig.SPEC);
        var gameBus = net.neoforged.neoforge.common.NeoForge.EVENT_BUS;
        gameBus.addListener(ic2.neoforge.energy.WorldEnergyNetworks::onUnload);
        gameBus.addListener(ic2.neoforge.energy.WorldEnergyNetworks::onChunkLoad);
        gameBus.addListener(ic2.neoforge.energy.WorldEnergyNetworks::onChunkUnload);
        gameBus.addListener(ic2.neoforge.energy.WorldEnergyNetworks::tick);
    }
}
