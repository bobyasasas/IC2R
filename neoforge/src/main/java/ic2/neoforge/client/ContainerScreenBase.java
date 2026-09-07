package ic2.neoforge.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Shared container frame uses authoritative menu slot positions. */
public class ContainerScreenBase<T extends AbstractContainerMenu>
        extends AbstractContainerScreen<T> {
    protected ContainerScreenBase(
            T menu, Inventory inventory, Component title, int width, int height) {
        super(menu, inventory, title, width, height);
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int x = leftPos, y = topPos;
        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xff373737);
        graphics.fill(x + 1, y + 1, x + imageWidth - 2, y + imageHeight - 2, 0xffffffff);
        graphics.fill(x + 3, y + 3, x + imageWidth - 3, y + imageHeight - 3, 0xffc6c6c6);
        for (var slot : menu.slots) {
            int sx = x + slot.x, sy = y + slot.y;
            graphics.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xffffffff);
            graphics.fill(sx - 1, sy - 1, sx + 16, sy + 16, 0xff373737);
            graphics.fill(sx, sy, sx + 16, sy + 16, 0xff8b8b8b);
        }
    }
}
