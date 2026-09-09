package ic2.neoforge.registration;

import ic2.neoforge.component.ModDataComponents;
import ic2.neoforge.item.CondensatorItem;
import ic2.neoforge.item.FuelRodItem;
import ic2.neoforge.item.HeatStorageComponent;
import ic2.neoforge.item.HeatSwitchItem;
import ic2.neoforge.item.MoxFuelRodItem;
import ic2.neoforge.item.ReactorHeatItem;
import ic2.neoforge.item.ReactorPlatingItem;
import ic2.neoforge.item.ReactorVentItem;
import ic2.neoforge.item.ReflectorItem;
import ic2.neoforge.item.VentSpreadItem;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModReactorItems {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("ic2");
    public static final DeferredItem<ReactorVentItem> HEAT_VENT =
            ITEMS.registerItem(
                    "heat_vent", properties -> new ReactorVentItem(properties, 1000, 6, 0));
    public static final DeferredItem<ReactorVentItem> REACTOR_HEAT_VENT =
            ITEMS.registerItem(
                    "reactor_heat_vent", properties -> new ReactorVentItem(properties, 1000, 5, 5));
    public static final DeferredItem<ReactorVentItem> OVERCLOCKED_HEAT_VENT =
            ITEMS.registerItem(
                    "overclocked_heat_vent",
                    properties -> new ReactorVentItem(properties, 1000, 20, 36));
    public static final DeferredItem<ReactorVentItem> ADVANCED_HEAT_VENT =
            ITEMS.registerItem(
                    "advanced_heat_vent",
                    properties -> new ReactorVentItem(properties, 1000, 12, 0));
    public static final DeferredItem<VentSpreadItem> COMPONENT_HEAT_VENT =
            ITEMS.registerItem(
                    "component_heat_vent", properties -> new VentSpreadItem(properties, 4));

    public static final DeferredItem<HeatStorageComponent> REACTOR_COOLANT_CELL =
            coolantCell("reactor_coolant_cell", 10000);
    public static final DeferredItem<HeatStorageComponent> TRIPLE_REACTOR_COOLANT_CELL =
            coolantCell("triple_reactor_coolant_cell", 30000);
    public static final DeferredItem<HeatStorageComponent> SEXTUPLE_REACTOR_COOLANT_CELL =
            coolantCell("sextuple_reactor_coolant_cell", 60000);

    public static final DeferredItem<ReactorPlatingItem> REACTOR_PLATING =
            plating("reactor_plating", 1000, 0.95F);
    public static final DeferredItem<ReactorPlatingItem> REACTOR_HEAT_PLATING =
            plating("reactor_heat_plating", 2000, 0.99F);
    public static final DeferredItem<ReactorPlatingItem> CONTAINMENT_REACTOR_PLATING =
            plating("containment_reactor_plating", 500, 0.9F);

    public static final DeferredItem<HeatSwitchItem> HEAT_EXCHANGER =
            heatSwitch("heat_exchanger", 2500, 12, 4);
    public static final DeferredItem<HeatSwitchItem> REACTOR_HEAT_EXCHANGER =
            heatSwitch("reactor_heat_exchanger", 5000, 0, 72);
    public static final DeferredItem<HeatSwitchItem> COMPONENT_HEAT_EXCHANGER =
            heatSwitch("component_heat_exchanger", 5000, 36, 0);
    public static final DeferredItem<HeatSwitchItem> ADVANCED_HEAT_EXCHANGER =
            heatSwitch("advanced_heat_exchanger", 10000, 24, 8);

    public static final DeferredItem<ReflectorItem> NEUTRON_REFLECTOR =
            ITEMS.registerItem(
                    "neutron_reflector", properties -> new ReflectorItem(properties, 30000, true));
    public static final DeferredItem<ReflectorItem> THICK_NEUTRON_REFLECTOR =
            ITEMS.registerItem(
                    "thick_neutron_reflector",
                    properties -> new ReflectorItem(properties, 120000, true));
    public static final DeferredItem<ReflectorItem> IRIDIUM_NEUTRON_REFLECTOR =
            ITEMS.registerItem(
                    "iridium_neutron_reflector",
                    properties -> new ReflectorItem(properties.stacksTo(1), 0, false));

    // P11 replication chain: the pattern-recording crystal memory (the blank raw crystal is a
    // material-block pipeline item).
    public static final DeferredItem<ic2.neoforge.item.CrystalMemoryItem> CRYSTAL_MEMORY =
            ITEMS.registerItem(
                    "crystal_memory",
                    properties -> new ic2.neoforge.item.CrystalMemoryItem(properties.stacksTo(1)));

    /** Tops up adjacent component heat storage straight from the core while it is below 1000. */
    public static final DeferredItem<ic2.neoforge.item.HeatpackItem> HEATPACK =
            ITEMS.registerItem(
                    "heatpack",
                    properties -> new ic2.neoforge.item.HeatpackItem(properties, 1000, 1));

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
    public static final DeferredItem<FuelRodItem> MOX_FUEL_ROD =
            ITEMS.registerItem(
                    "mox_fuel_rod", properties -> new MoxFuelRodItem(properties, 1, 10000));
    public static final DeferredItem<FuelRodItem> DUAL_MOX_FUEL_ROD =
            ITEMS.registerItem(
                    "dual_mox_fuel_rod", properties -> new MoxFuelRodItem(properties, 2, 10000));
    public static final DeferredItem<FuelRodItem> QUAD_MOX_FUEL_ROD =
            ITEMS.registerItem(
                    "quad_mox_fuel_rod", properties -> new MoxFuelRodItem(properties, 4, 10000));
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

    private static DeferredItem<HeatStorageComponent> coolantCell(String name, int capacity) {
        return ITEMS.registerItem(
                name, properties -> new HeatStorageComponent(properties, capacity));
    }

    private static DeferredItem<ReactorPlatingItem> plating(
            String name, int maxHeatAdd, float effectModifier) {
        return ITEMS.registerItem(
                name, properties -> new ReactorPlatingItem(properties, maxHeatAdd, effectModifier));
    }

    private static DeferredItem<HeatSwitchItem> heatSwitch(
            String name, int capacity, int switchSide, int switchReactor) {
        return ITEMS.registerItem(
                name,
                properties -> new HeatSwitchItem(properties, capacity, switchSide, switchReactor));
    }

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
            event.accept(REACTOR_HEAT_VENT);
            event.accept(OVERCLOCKED_HEAT_VENT);
            event.accept(ADVANCED_HEAT_VENT);
            event.accept(COMPONENT_HEAT_VENT);
            event.accept(REACTOR_COOLANT_CELL);
            event.accept(TRIPLE_REACTOR_COOLANT_CELL);
            event.accept(SEXTUPLE_REACTOR_COOLANT_CELL);
            event.accept(REACTOR_PLATING);
            event.accept(REACTOR_HEAT_PLATING);
            event.accept(CONTAINMENT_REACTOR_PLATING);
            event.accept(HEAT_EXCHANGER);
            event.accept(REACTOR_HEAT_EXCHANGER);
            event.accept(COMPONENT_HEAT_EXCHANGER);
            event.accept(ADVANCED_HEAT_EXCHANGER);
            event.accept(NEUTRON_REFLECTOR);
            event.accept(THICK_NEUTRON_REFLECTOR);
            event.accept(IRIDIUM_NEUTRON_REFLECTOR);
            event.accept(HEATPACK);
            event.accept(CRYSTAL_MEMORY);
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
