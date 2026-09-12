package ic2.neoforge.client;

import ic2.neoforge.menu.ContainmentBoxMenu;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Legacy GuiContainmentbox: twelve gated slots over the shared container frame. */
public final class ContainmentBoxScreen extends ContainerScreenBase<ContainmentBoxMenu> {
    public ContainmentBoxScreen(ContainmentBoxMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
    }
}
