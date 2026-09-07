package ic2.neoforge.registration;

import ic2.neoforge.IndustrialCraft;
import ic2.neoforge.component.ModDataComponents;
import ic2.neoforge.item.ToolboxItem;
import ic2.neoforge.menu.ToolboxMenu;
import ic2.neoforge.transfer.ToolboxHandler;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModToolbox {
    private static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(IndustrialCraft.MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, IndustrialCraft.MOD_ID);
    public static final DeferredItem<ToolboxItem> ITEM =
            ITEMS.registerItem(
                    "tool_box",
                    p ->
                            new ToolboxItem(
                                    p.stacksTo(1)
                                            .rarity(Rarity.UNCOMMON)
                                            .component(
                                                    ModDataComponents.TOOLBOX_CONTENTS.get(),
                                                    ItemContainerContents.EMPTY)));
    public static final DeferredHolder<MenuType<?>, MenuType<ToolboxMenu>> MENU =
            MENUS.register(
                    "tool_box",
                    () ->
                            IMenuTypeExtension.create(
                                    (id, inventory, data) ->
                                            new ToolboxMenu(
                                                    id,
                                                    inventory,
                                                    data.readVarInt(),
                                                    data.readUUID(),
                                                    true)));

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        MENUS.register(bus);
        bus.addListener(
                (RegisterCapabilitiesEvent event) ->
                        event.registerItem(
                                Capabilities.Item.ITEM,
                                (stack, access) -> new ToolboxHandler(access),
                                ITEM.get()));
        bus.addListener(
                (BuildCreativeModeTabContentsEvent event) -> {
                    if (event.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES))
                        event.accept(ITEM);
                });
    }

    private ModToolbox() {}
}
