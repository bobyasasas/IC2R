package ic2.neoforge.registration;

import ic2.neoforge.item.ReactorHeatItem;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModReactorItems {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("ic2");
    public static final DeferredItem<ReactorHeatItem> HEAT_VENT =
            ITEMS.registerItem("heat_vent", properties -> new ReactorHeatItem(properties, 1000, 6));

    /**
     * RTG fuel: one pellet per slot across six machine slots. The recovered item also irradiates an
     * unprotected carrier; that behavior needs the hazmat armor set and lands with P16.
     */
    public static final DeferredItem<Item> RTG_PELLET =
            ITEMS.registerItem("rtg_pellet", properties -> new Item(properties.stacksTo(1)));

    // Nuclear materials (P09): stackable refinement outputs of the ore and reactor chains.
    // The recovered resources also irradiate an unprotected carrier; that behavior needs the
    // hazmat armor set and lands with P16.
    public static final DeferredItem<Item> URANIUM = nuclear("uranium");
    public static final DeferredItem<Item> URANIUM_235 = nuclear("uranium_235");
    public static final DeferredItem<Item> URANIUM_238 = nuclear("uranium_238");
    public static final DeferredItem<Item> PLUTONIUM = nuclear("plutonium");
    public static final DeferredItem<Item> SMALL_PLUTONIUM = nuclear("small_plutonium");
    public static final DeferredItem<Item> SMALL_URANIUM_235 = nuclear("small_uranium_235");
    public static final DeferredItem<Item> SMALL_URANIUM_238 = nuclear("small_uranium_238");
    public static final DeferredItem<Item> MOX = nuclear("mox");
    public static final DeferredItem<Item> URANIUM_PELLET = nuclear("uranium_pellet");

    private static DeferredItem<Item> nuclear(String name) {
        return ITEMS.registerItem(name, properties -> new Item(properties));
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        bus.addListener(ModReactorItems::creative);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(ModReactorItems::tooltip);
    }

    private static void creative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.FUNCTIONAL_BLOCKS)) event.accept(HEAT_VENT);
        if (event.getTabKey().equals(CreativeModeTabs.INGREDIENTS)) {
            event.accept(RTG_PELLET);
            event.accept(URANIUM);
            event.accept(URANIUM_235);
            event.accept(URANIUM_238);
            event.accept(PLUTONIUM);
            event.accept(SMALL_PLUTONIUM);
            event.accept(SMALL_URANIUM_235);
            event.accept(SMALL_URANIUM_238);
            event.accept(MOX);
            event.accept(URANIUM_PELLET);
        }
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
