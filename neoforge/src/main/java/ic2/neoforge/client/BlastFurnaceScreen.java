package ic2.neoforge.client;

import ic2.neoforge.menu.MachineMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Progress plus heat and air gauges; the furnace runs on HU heat, not EU. */
public final class BlastFurnaceScreen extends MachineScreen {
    public BlastFurnaceScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected boolean showsEnergyBar() {
        return false;
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int x = leftPos, y = topPos;
        int heatHeight = (int) Math.clamp(48L * (long) menu.familyFloat(0), 0, 48);
        graphics.fill(x + 25, y + 18, x + 37, y + 68, 0xff373737);
        graphics.fill(x + 26, y + 67 - heatHeight, x + 36, y + 67, 0xffff5c26);
        int airHeight = (int) Math.clamp(48L * (long) menu.familyFloat(1), 0, 48);
        graphics.fill(x + 139, y + 18, x + 151, y + 68, 0xff373737);
        graphics.fill(x + 140, y + 67 - airHeight, x + 150, y + 67, 0xff9aa7b0);
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        if (mouseY >= topPos + 18 && mouseY < topPos + 68) {
            if (mouseX >= leftPos + 25 && mouseX < leftPos + 37) {
                graphics.setComponentTooltipForNextFrame(
                        font,
                        List.of(Component.translatable("ic2.BlastFurnace.gui.heat")),
                        mouseX,
                        mouseY,
                        ItemStack.EMPTY);
            } else if (mouseX >= leftPos + 139 && mouseX < leftPos + 151) {
                graphics.setComponentTooltipForNextFrame(
                        font,
                        List.of(Component.translatable("ic2.BlastFurnace.gui.air")),
                        mouseX,
                        mouseY,
                        ItemStack.EMPTY);
            }
        }
    }
}
