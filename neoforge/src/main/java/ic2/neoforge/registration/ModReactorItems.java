package ic2.neoforge.registration;

import ic2.neoforge.component.ModDataComponents;
import ic2.neoforge.item.CondensatorItem;
import ic2.neoforge.item.FuelRodItem;
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
     * Reactor heat storage: absorbs heat once the reactor itself migrates (P12) and is recharged
     * through gradual crafting recipes with redstone or lapis.
     */
    public static final DeferredItem<CondensatorItem> RSH_CONDENSATOR =
            ITEMS.registerItem(
                    "rsh_condensator", properties -> new CondensatorItem(properties, 20000));

    public static final DeferredItem<CondensatorItem> LZH_CONDENSATOR =
            ITEMS.registerItem(
                    "lzh_condensator", properties -> new CondensatorItem(properties, 100000));

    // Nuclear fuel rods (P12 item chain): cells and duration keep the legacy reactor pacing; the
    // depletion component and in-reactor pulses activate with the reactor itself.
    public static final DeferredItem<FuelRodItem> URANIUM_FUEL_ROD =
            fuelRod("uranium_fuel_rod", 1, 20000);
    public static final DeferredItem<FuelRodItem> DUAL_URANIUM_FUEL_ROD =
            fuelRod("dual_uranium_fuel_rod", 2, 20000);
    public static final DeferredItem<FuelRodItem> QUAD_URANIUM_FUEL_ROD =
            fuelRod("quad_uranium_fuel_rod", 4, 20000);
    public static final DeferredItem<FuelRodItem> MOX_FUEL_ROD = fuelRod("mox_fuel_rod", 1, 10000);
    public static final DeferredItem<FuelRodItem> DUAL_MOX_FUEL_ROD =
            fuelRod("dual_mox_fuel_rod", 2, 10000);
    public static final DeferredItem<FuelRodItem> QUAD_MOX_FUEL_ROD =
            fuelRod("quad_mox_fuel_rod", 4, 10000);
    public static final DeferredItem<Item> DEPLETED_URANIUM_FUEL_ROD =
            nuclear("depleted_uranium_fuel_rod");
    public static final DeferredItem<Item> DEPLETED_DUAL_URANIUM_FUEL_ROD =
            nuclear("depleted_dual_uranium_fuel_rod");
    public static final DeferredItem<Item> DEPLETED_QUAD_URANIUM_FUEL_ROD =
            nuclear("depleted_quad_uranium_fuel_rod");
    public static final DeferredItem<Item> DEPLETED_MOX_FUEL_ROD = nuclear("depleted_mox_fuel_rod");
    public static final DeferredItem<Item> DEPLETED_DUAL_MOX_FUEL_ROD =
            nuclear("depleted_dual_mox_fuel_rod");
    public static final DeferredItem<Item> DEPLETED_QUAD_MOX_FUEL_ROD =
            nuclear("depleted_quad_mox_fuel_rod");

    private static DeferredItem<FuelRodItem> fuelRod(String name, int cells, int duration) {
        return ITEMS.registerItem(
                name,
                properties ->
                        new FuelRodItem(
                                properties.component(ModDataComponents.REACTOR_USE.get(), 0),
                                cells,
                                duration));
    }

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
            event.accept(RSH_CONDENSATOR);
            event.accept(LZH_CONDENSATOR);
            event.accept(URANIUM_FUEL_ROD);
            event.accept(DUAL_URANIUM_FUEL_ROD);
            event.accept(QUAD_URANIUM_FUEL_ROD);
            event.accept(MOX_FUEL_ROD);
            event.accept(DUAL_MOX_FUEL_ROD);
            event.accept(QUAD_MOX_FUEL_ROD);
            event.accept(DEPLETED_URANIUM_FUEL_ROD);
            event.accept(DEPLETED_DUAL_URANIUM_FUEL_ROD);
            event.accept(DEPLETED_QUAD_URANIUM_FUEL_ROD);
            event.accept(DEPLETED_MOX_FUEL_ROD);
            event.accept(DEPLETED_DUAL_MOX_FUEL_ROD);
            event.accept(DEPLETED_QUAD_MOX_FUEL_ROD);
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
