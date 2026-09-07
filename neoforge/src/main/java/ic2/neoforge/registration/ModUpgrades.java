package ic2.neoforge.registration;

import ic2.neoforge.IndustrialCraft;
import ic2.neoforge.item.UpgradeItem;

import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

public final class ModUpgrades {
    private static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(IndustrialCraft.MOD_ID);
    public static final Map<UpgradeItem.Kind, DeferredItem<UpgradeItem>> ALL = create();

    private static Map<UpgradeItem.Kind, DeferredItem<UpgradeItem>> create() {
        var result =
                new EnumMap<UpgradeItem.Kind, DeferredItem<UpgradeItem>>(UpgradeItem.Kind.class);
        for (var kind : UpgradeItem.Kind.values())
            result.put(
                    kind,
                    ITEMS.registerItem(
                            kind.name().toLowerCase(Locale.ROOT) + "_upgrade",
                            p -> new UpgradeItem(kind, p)));
        return Collections.unmodifiableMap(result);
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        bus.addListener(
                (BuildCreativeModeTabContentsEvent event) -> {
                    if (event.getTabKey().equals(CreativeModeTabs.FUNCTIONAL_BLOCKS))
                        ALL.values().forEach(event::accept);
                });
    }

    private ModUpgrades() {}
}
