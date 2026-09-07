package ic2.neoforge.registration;

import ic2.neoforge.IndustrialCraft;
import ic2.neoforge.fluid.CellFluidHandler;
import ic2.neoforge.item.FluidCellItem;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public final class ModCells {
    private static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(IndustrialCraft.MOD_ID);
    public static final DeferredItem<FluidCellItem> EMPTY = cell("facade_cell", () -> Fluids.EMPTY);
    public static final DeferredItem<FluidCellItem> WATER = cell("water_cell", () -> Fluids.WATER);
    public static final DeferredItem<FluidCellItem> LAVA = cell("lava_cell", () -> Fluids.LAVA);
    public static final Map<String, DeferredItem<FluidCellItem>> CELLS = cells();

    private static Map<String, DeferredItem<FluidCellItem>> cells() {
        var result = new LinkedHashMap<String, DeferredItem<FluidCellItem>>();
        result.put("facade_cell", EMPTY);
        result.put("water_cell", WATER);
        result.put("lava_cell", LAVA);
        ModFluids.FAMILIES.forEach(
                (definition, family) ->
                        result.put(
                                definition.id() + "_cell",
                                cell(definition.id() + "_cell", family.source())));
        return Collections.unmodifiableMap(result);
    }

    private static DeferredItem<FluidCellItem> cell(String id, Supplier<? extends Fluid> fluid) {
        return ITEMS.registerItem(id, properties -> new FluidCellItem(properties, fluid));
    }

    public static FluidCellItem forFluid(Fluid fluid) {
        return CELLS.values().stream()
                .map(DeferredItem::get)
                .filter(cell -> cell.fixedFluid() == fluid)
                .findFirst()
                .orElse(EMPTY.get());
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        bus.addListener(ModCells::capabilities);
        bus.addListener(ModCells::creativeContents);
    }

    private static void capabilities(RegisterCapabilitiesEvent event) {
        CELLS.values()
                .forEach(
                        cell ->
                                event.registerItem(
                                        Capabilities.Fluid.ITEM,
                                        (stack, access) -> new CellFluidHandler(access),
                                        cell.get()));
    }

    private static void creativeContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES))
            CELLS.values().forEach(event::accept);
    }

    private ModCells() {}
}
