package ic2.neoforge.registration;

import ic2.neoforge.IndustrialCraft;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Comparator;

/** IC2's complete creative inventory, sorted by registry ID without a manual item list. */
public final class ModCreativeTabs {
    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, IndustrialCraft.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> GENERAL =
            TABS.register(
                    "general",
                    () ->
                            CreativeModeTab.builder()
                                    .title(Component.translatable("itemGroup.ic2.general"))
                                    .icon(() -> new ItemStack(ModMachines.GENERATOR.get()))
                                    .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
                                    .withSearchBar()
                                    .displayItems(ModCreativeTabs::displayItems)
                                    .build());

    private static void displayItems(
            CreativeModeTab.ItemDisplayParameters parameters, CreativeModeTab.Output output) {
        BuiltInRegistries.ITEM.stream()
                .filter(ModCreativeTabs::isIc2Item)
                .filter(item -> item.isEnabled(parameters.enabledFeatures()))
                .sorted(Comparator.comparing(item -> BuiltInRegistries.ITEM.getKey(item).toString()))
                .forEach(output::accept);
    }

    private static boolean isIc2Item(Item item) {
        return IndustrialCraft.MOD_ID.equals(
                BuiltInRegistries.ITEM.getKey(item).getNamespace());
    }

    public static void register(IEventBus bus) {
        TABS.register(bus);
    }

    private ModCreativeTabs() {}
}
