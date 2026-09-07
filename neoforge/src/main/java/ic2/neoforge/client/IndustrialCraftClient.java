package ic2.neoforge.client;

import ic2.neoforge.IndustrialCraft;
import ic2.neoforge.item.ElectricItem;
import ic2.neoforge.item.ElectricItemEnergy;
import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.registration.ModMachines;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterRangeSelectItemModelPropertyEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@Mod(value = IndustrialCraft.MOD_ID, dist = Dist.CLIENT)
public final class IndustrialCraftClient {
    public IndustrialCraftClient(IEventBus modBus) {
        modBus.addListener(IndustrialCraftClient::registerProperties);
        modBus.addListener(FluidModels::register);
        modBus.addListener(MachineSounds::reloaded);
        modBus.addListener(IndustrialCraftClient::registerScreens);
        NeoForge.EVENT_BUS.addListener(IndustrialCraftClient::addTooltip);
        NeoForge.EVENT_BUS.addListener(MachineSounds::loaded);
        NeoForge.EVENT_BUS.addListener(MachineSounds::tick);
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        ModMachines.MACHINES.forEach(
                (kind, registration) -> {
                    if (kind == MachineKind.CANNER)
                        event.register(registration.menu().get(), CannerScreen::new);
                    else event.register(registration.menu().get(), MachineScreen::new);
                });
    }

    private static void addTooltip(ItemTooltipEvent event) {
        if (event.getItemStack().getItem() instanceof ElectricItem item) {
            event.getToolTip()
                    .add(
                            Component.translatable(
                                    "ic2.tooltip.energy",
                                    ElectricItemEnergy.charge(event.getItemStack()),
                                    item.specification().capacity()));
        }
    }

    private static void registerProperties(RegisterRangeSelectItemModelPropertyEvent event) {
        event.register(
                Identifier.fromNamespaceAndPath(IndustrialCraft.MOD_ID, "charge"),
                ChargeProperty.CODEC);
    }
}
