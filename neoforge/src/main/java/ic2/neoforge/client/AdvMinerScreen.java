package ic2.neoforge.client;

import ic2.neoforge.menu.MachineMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Buttons for the sweep cursor, the blacklist mode and the silk touch switch. */
public final class AdvMinerScreen extends MachineScreen {
    private Button resetButton;
    private Button modeButton;
    private Button silkButton;

    public AdvMinerScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    public void init() {
        super.init();
        resetButton = addRenderableWidget(button("ic2.AdvMiner.gui.switch.reset", 0, 8, 8, 130));
        modeButton = addRenderableWidget(button("ic2.AdvMiner.gui.switch.mode", 1, 8, 92, 130));
        silkButton =
                addRenderableWidget(button("ic2.AdvMiner.gui.switch.silktouch", 2, 8, 110, 130));
    }

    private Button button(String key, int action, int x, int y, int width) {
        return addRenderableWidget(
                Button.builder(
                                key.endsWith("silktouch")
                                        ? Component.translatable(key, menu.familyValue(1) == 1)
                                        : Component.translatable(key),
                                control -> {
                                    if (minecraft.gameMode != null)
                                        minecraft.gameMode.handleInventoryButtonClick(
                                                menu.containerId, action);
                                })
                        .bounds(leftPos + x, topPos + y, width, 16)
                        .build());
    }

    private Component modeLabel() {
        return Component.translatable(
                menu.familyValue(0) == 1
                        ? "ic2.AdvMiner.gui.mode.blacklist"
                        : "ic2.AdvMiner.gui.mode.whitelist");
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        modeButton.setMessage(modeLabel());
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }
}
