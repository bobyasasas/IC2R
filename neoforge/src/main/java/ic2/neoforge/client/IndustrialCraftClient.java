package ic2.neoforge.client;

import ic2.neoforge.IndustrialCraft;
import ic2.neoforge.item.ElectricItem;
import ic2.neoforge.item.ElectricItemEnergy;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterRangeSelectItemModelPropertyEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@Mod(value = IndustrialCraft.MOD_ID, dist = Dist.CLIENT)
public final class IndustrialCraftClient {
    public IndustrialCraftClient(IEventBus modBus) {
        modBus.addListener(IndustrialCraftClient::registerProperties);
        modBus.addListener(IndustrialCraftClient::registerScreens);
        NeoForge.EVENT_BUS.addListener(IndustrialCraftClient::addTooltip);
    }

    private static void registerScreens(
            net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) {
        event.register(
                ic2.neoforge.registration.ModMachines.GENERATOR_MENU.get(), MachineScreen::new);
        event.register(
                ic2.neoforge.registration.ModMachines.ELECTRIC_FURNACE_MENU.get(),
                MachineScreen::new);
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
