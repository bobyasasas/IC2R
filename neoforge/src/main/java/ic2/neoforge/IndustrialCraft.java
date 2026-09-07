package ic2.neoforge;

import ic2.neoforge.component.ModDataComponents;
import ic2.neoforge.energy.EnergyConfig;
import ic2.neoforge.energy.WorldEnergyNetworks;
import ic2.neoforge.registration.ModCannerRecipes;
import ic2.neoforge.registration.ModCells;
import ic2.neoforge.registration.ModCraftingRecipes;
import ic2.neoforge.registration.ModFluids;
import ic2.neoforge.registration.ModItems;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.registration.ModMaterialBlocks;
import ic2.neoforge.registration.ModProcessingRecipes;
import ic2.neoforge.registration.ModSounds;
import ic2.neoforge.registration.ModTools;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

/** Composition root: platform registration belongs here, machine rules belong in core. */
@Mod(IndustrialCraft.MOD_ID)
public final class IndustrialCraft {
    public static final String MOD_ID = "ic2";

    public IndustrialCraft(IEventBus modBus, ModContainer container) {
        ModDataComponents.register(modBus);
        ModItems.register(modBus);
        ModTools.register(modBus);
        ModSounds.register(modBus);
        ModCannerRecipes.register(modBus);
        ModCells.register(modBus);
        ModFluids.register(modBus);
        ModMaterialBlocks.register(modBus);
        ModCraftingRecipes.register(modBus);
        ModProcessingRecipes.register(modBus);
        ModMachines.register(modBus);
        container.registerConfig(ModConfig.Type.SERVER, EnergyConfig.SPEC);
        var gameBus = NeoForge.EVENT_BUS;
        gameBus.addListener(WorldEnergyNetworks::onUnload);
        gameBus.addListener(WorldEnergyNetworks::onChunkLoad);
        gameBus.addListener(WorldEnergyNetworks::onChunkUnload);
        gameBus.addListener(WorldEnergyNetworks::tick);
    }
}
