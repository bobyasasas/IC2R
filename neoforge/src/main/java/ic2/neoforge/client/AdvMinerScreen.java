package ic2.neoforge.client;

import ic2.neoforge.menu.MachineMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
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
        resetButton = button("R", "ic2.AdvMiner.gui.switch.reset", 0, 133, 101, 36);
        modeButton = button("M", "ic2.AdvMiner.gui.switch.mode", 1, 123, 27, 18);
        silkButton =
                button("S", "ic2.AdvMiner.gui.switch.silktouch", 2, 129, 45, 18);
    }

    private Button button(
            String symbol, String tooltip, int action, int x, int y, int width) {
        var button =
                Button.builder(
                                Component.literal(symbol),
                                control -> {
                                    if (minecraft.gameMode != null)
                                        minecraft.gameMode.handleInventoryButtonClick(
                                                menu.containerId, action);
                                })
                        .bounds(leftPos + x, topPos + y, width, 15)
                        .build();
        button.setTooltip(Tooltip.create(Component.translatable(tooltip)));
        return addRenderableWidget(button);
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
        modeButton.setTooltip(Tooltip.create(modeLabel()));
        silkButton.setTooltip(
                Tooltip.create(
                        Component.translatable(
                                "ic2.AdvMiner.gui.switch.silktouch",
                                menu.familyValue(1) == 1)));
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.text(font, modeLabel(), leftPos + 40, topPos + 30, 0xff20eb3e, false);
    }
}
