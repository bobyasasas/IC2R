package ic2.neoforge.client;

import ic2.neoforge.menu.MachineMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** No energy or progress: the workbench is a crafting bench with a buffer and tool combos. */
public final class IndustrialWorkbenchScreen extends MachineScreen {
    private static final int CLEAR_X = 124, CLEAR_Y = 84, CLEAR_SIZE = 16;

    public IndustrialWorkbenchScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected boolean showsEnergyBar() {
        return false;
    }

    @Override
    protected boolean showsProgress() {
        return false;
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.text(
                font,
                Component.translatable("ic2.workbench.clear"),
                leftPos + 8,
                topPos + 84,
                0xff404040,
                false);
        graphics.fill(
                leftPos + CLEAR_X,
                topPos + CLEAR_Y,
                leftPos + CLEAR_X + CLEAR_SIZE,
                topPos + CLEAR_Y + CLEAR_SIZE,
                0xffa04040);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0
                && event.x() >= leftPos + CLEAR_X
                && event.x() < leftPos + CLEAR_X + CLEAR_SIZE
                && event.y() >= topPos + CLEAR_Y
                && event.y() < topPos + CLEAR_Y + CLEAR_SIZE) {
            if (minecraft.gameMode != null)
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }
}
