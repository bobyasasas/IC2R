package ic2.neoforge.client;

import ic2.neoforge.menu.MiningFilterMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Hologram editing grid with the blacklist/whitelist toggle above it. */
public final class MiningFilterScreen extends ContainerScreenBase<MiningFilterMenu> {
    private Button modeButton;

    public MiningFilterScreen(MiningFilterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 215);
    }

    @Override
    public void init() {
        super.init();
        modeButton =
                addRenderableWidget(
                        Button.builder(
                                        Component.empty(),
                                        control ->
                                                minecraft.gameMode.handleInventoryButtonClick(
                                                        menu.containerId, 0))
                                .bounds(leftPos + 8, topPos + 10, 100, 15)
                                .build());
    }

    private Component modeLabel() {
        return Component.translatable(
                menu.blacklist()
                        ? "ic2.MiningFilter.gui.mode.blacklist"
                        : "ic2.MiningFilter.gui.mode.whitelist");
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        modeButton.setMessage(modeLabel());
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }
}
