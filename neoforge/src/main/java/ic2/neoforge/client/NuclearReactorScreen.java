package ic2.neoforge.client;

import ic2.neoforge.menu.MachineMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Heat gauge next to the component grid; EU output is shown by the energy bar. */
public final class NuclearReactorScreen extends MachineScreen {
    public NuclearReactorScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int heatHeight = (int) Math.clamp((long) (48.0F * menu.familyFloat(0)), 0, 48);
        graphics.fill(leftPos + 25, topPos + 18, leftPos + 37, topPos + 68, 0xff373737);
        graphics.fill(
                leftPos + 26, topPos + 67 - heatHeight, leftPos + 36, topPos + 67, 0xffff5c26);
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        if (mouseX >= leftPos + 25
                && mouseX < leftPos + 37
                && mouseY >= topPos + 18
                && mouseY < topPos + 68) {
            graphics.setComponentTooltipForNextFrame(
                    font,
                    List.of(Component.translatable("ic2.tooltip.reactor_heat")),
                    mouseX,
                    mouseY,
                    ItemStack.EMPTY);
        }
    }
}
