package ic2.neoforge.client;

import ic2.neoforge.menu.ToolboxMenu;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class ToolboxScreen extends ContainerScreenBase<ToolboxMenu> {
    public ToolboxScreen(ToolboxMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
    }
}
