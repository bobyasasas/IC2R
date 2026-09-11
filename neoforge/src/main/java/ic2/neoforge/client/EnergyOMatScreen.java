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
        for (int column = 0; column < 4; column++) {
            int step = (int) Math.pow(10, column + 2);
            int x = leftPos + 8 + column * 36;
            int plus = 7 - column, minus = 3 - column;
            addRenderableWidget(
                    Button.builder(Component.literal("+" + step), b -> send(plus))
                            .bounds(x, topPos + 18, 35, 18)
                            .build());
            addRenderableWidget(
                    Button.builder(Component.literal("-" + step), b -> send(minus))
                            .bounds(x, topPos + 38, 35, 18)
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
        graphics.text(
                font,
                Component.literal(menu.familyValue(0) + " EU"),
                leftPos + 8,
                topPos + 61,
                0xff404040,
                false);
    }
}
