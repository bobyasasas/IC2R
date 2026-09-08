package ic2.neoforge.registration;

import ic2.neoforge.item.ReactorHeatItem;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModReactorItems {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("ic2");
    public static final DeferredItem<ReactorHeatItem> HEAT_VENT =
            ITEMS.registerItem("heat_vent", properties -> new ReactorHeatItem(properties, 1000, 6));

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        bus.addListener(ModReactorItems::creative);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(ModReactorItems::tooltip);
    }

    private static void creative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.FUNCTIONAL_BLOCKS)) event.accept(HEAT_VENT);
    }

    private static void tooltip(net.neoforged.neoforge.event.entity.player.ItemTooltipEvent event) {
        if (event.getItemStack().getItem() instanceof ReactorHeatItem item) {
            var heat = item.heat(event.getItemStack());
            event.getToolTip()
                    .add(
                            Component.translatable(
                                    "ic2.tooltip.processing_heat", heat.stored(), heat.capacity()));
        }
    }

    private ModReactorItems() {}
}
