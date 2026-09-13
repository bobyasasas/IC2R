package ic2.neoforge.client;

import ic2.neoforge.menu.AdvancedEditOreMenu;

import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Legacy GuiEditOre, minus its stdout-only placeholder list: the filter row and back button. */
public final class AdvancedEditOreScreen extends ContainerScreenBase<AdvancedEditOreMenu> {
    public AdvancedEditOreScreen(AdvancedEditOreMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 200);
    }

    @Override
    public void init() {
        super.init();
        addRenderableWidget(
                Button.builder(
                                Component.translatable("ic2.upgrade.advancedGUI.back"),
                                control ->
                                        minecraft.gameMode.handleInventoryButtonClick(
                                                menu.containerId, AdvancedEditOreMenu.BACK))
                        .bounds(leftPos + 10, topPos + 30, 50, 15)
                        .build());
    }
}
