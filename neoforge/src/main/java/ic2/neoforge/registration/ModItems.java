package ic2.neoforge.registration;

import ic2.core.energy.ElectricItemSpec;
import ic2.neoforge.IndustrialCraft;
import ic2.neoforge.item.BatteryItem;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public final class ModItems {
    private static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(IndustrialCraft.MOD_ID);

    public static final Map<MaterialDefinition, DeferredItem<Item>> MATERIALS = registerMaterials();
    public static final DeferredItem<Item> COPPER_PLATE =
            MATERIALS.get(MaterialDefinition.COPPER_PLATE);

    private static Map<MaterialDefinition, DeferredItem<Item>> registerMaterials() {
        var materials =
                new EnumMap<MaterialDefinition, DeferredItem<Item>>(MaterialDefinition.class);
        for (var definition : MaterialDefinition.values()) {
            materials.put(
                    definition, ITEMS.registerSimpleItem(definition.id(), definition::properties));
        }
        return Collections.unmodifiableMap(materials);
    }

    public static final DeferredItem<BatteryItem> RE_BATTERY =
            battery("re_battery", 10000, 100, 1, 64, Rarity.COMMON);
    public static final DeferredItem<BatteryItem> ADVANCED_RE_BATTERY =
            battery("advanced_re_battery", 100000, 256, 2, 16, Rarity.COMMON);
    public static final DeferredItem<BatteryItem> ENERGY_CRYSTAL =
            battery("energy_crystal", 1000000, 2048, 3, 16, Rarity.COMMON);
    public static final DeferredItem<BatteryItem> LAPOTRON_CRYSTAL =
            battery("lapotron_crystal", 10000000, 8092, 4, 16, Rarity.UNCOMMON);

    private static DeferredItem<BatteryItem> battery(
            String id, double capacity, double limit, int tier, int stackSize, Rarity rarity) {
        return ITEMS.registerItem(
                id,
                properties ->
                        new BatteryItem(
                                properties.stacksTo(stackSize).rarity(rarity),
                                new ElectricItemSpec(capacity, limit, tier, true)));
    }

    private ModItems() {}

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        modBus.addListener(ModItems::addCreativeContents);
    }

    private static void addCreativeContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.INGREDIENTS)) {
            MATERIALS.values().forEach(event::accept);
            event.accept(RE_BATTERY);
            event.accept(ADVANCED_RE_BATTERY);
            event.accept(ENERGY_CRYSTAL);
            event.accept(LAPOTRON_CRYSTAL);
        }
    }
}
