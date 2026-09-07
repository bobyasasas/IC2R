package ic2.neoforge.client;

import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.menu.MachineMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Small shared screen; slot geometry is taken from the menu, so visuals cannot drift. */
public class MachineScreen extends AbstractContainerScreen<MachineMenu> {
    public MachineScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, menu.kind().upgradable() ? 202 : 176, 166);
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
        int energyHeight =
                (int)
                        Math.clamp(
                                48L
                                        * (menu.capacity() > 0
                                                ? menu.energy()
                                                : menu.fuelRemaining())
                                        / Math.max(
                                                1,
                                                menu.capacity() > 0
                                                        ? menu.capacity()
                                                        : menu.fuelMaximum()),
                                0,
                                48);
        graphics.fill(x + 25, y + 18, x + 37, y + 68, 0xff373737);
        graphics.fill(x + 26, y + 67 - energyHeight, x + 36, y + 67, 0xffe9ae23);
        if (menu.kind().energyDevice()) return;
        int progressWidth =
                menu.progressMaximum() <= 0
                        ? 0
                        : (int) Math.clamp(24L * menu.progress() / menu.progressMaximum(), 0, 24);
        graphics.fill(x + 79, y + 36, x + 103, y + 45, 0xff747474);
        graphics.fill(
                x + 79,
                y + 36,
                x + 79 + progressWidth,
                y + 45,
                menu.kind() == MachineKind.GENERATOR ? 0xffff8f35 : 0xffe9ae23);
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        if (menu.capacity() > 0
                && mouseX >= leftPos + 25
                && mouseX < leftPos + 37
                && mouseY >= topPos + 18
                && mouseY < topPos + 68) {
            graphics.setComponentTooltipForNextFrame(
                    font,
                    List.of(
                            Component.translatable(
                                    "ic2.tooltip.energy", menu.energy(), menu.capacity())),
                    mouseX,
                    mouseY,
                    ItemStack.EMPTY);
        }
    }
}
