package ic2.neoforge.client;

import ic2.neoforge.menu.MachineMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Legacy price keypad: four step sizes up and down with a 100 EU floor. */
public final class EnergyOMatScreen extends MachineScreen {
    public EnergyOMatScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected boolean showsProgress() {
        return false;
    }

    @Override
    public void init() {
        super.init();
        String[] steps = {"100k", "10k", "1k", "100"};
        for (int row = 0; row < 4; row++) {
            int index = row;
            addRenderableWidget(
                    Button.builder(Component.literal("-" + steps[row]), b -> send(index))
                            .bounds(leftPos + 102, topPos + 16 + row * 10, 32, 10)
                            .build());
            addRenderableWidget(
                    Button.builder(Component.literal("+" + steps[row]), b -> send(index + 4))
                            .bounds(leftPos + 134, topPos + 16 + row * 10, 32, 10)
                            .build());
        }
    }

    private void send(int id) {
        if (minecraft.gameMode != null)
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        drawFittedText(
                graphics,
                Component.translatable("container.inventory"),
                8,
                imageHeight - 94,
                88,
                0xff404040);
        drawFittedText(
                graphics, Component.translatable("ic2.container.personalTrader.offer"),
                100, 60, 68, 0xff404040);
        drawFittedText(
                graphics, Component.literal(menu.familyValue(0) + " EU"),
                100, 68, 68, 0xff404040);
    }
}
