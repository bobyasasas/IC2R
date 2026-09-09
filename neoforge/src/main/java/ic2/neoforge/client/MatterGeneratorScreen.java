package ic2.neoforge.client;

import ic2.neoforge.menu.MachineMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** EU buffer bar plus the UU-matter tank gauge. */
public final class MatterGeneratorScreen extends MachineScreen {
    public MatterGeneratorScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int airHeight = (int) Math.clamp((long) (48.0F * menu.familyFloat(0)), 0, 48);
        graphics.fill(leftPos + 107, topPos + 18, leftPos + 119, topPos + 68, 0xff373737);
        graphics.fill(
                leftPos + 108, topPos + 67 - airHeight, leftPos + 118, topPos + 67, 0xff35c9ff);
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        if (mouseX >= leftPos + 107
                && mouseX < leftPos + 119
                && mouseY >= topPos + 18
                && mouseY < topPos + 68) {
            graphics.setComponentTooltipForNextFrame(
                    font,
                    List.of(Component.translatable("ic2.Matter.gui.info.progress")),
                    mouseX,
                    mouseY,
                    ItemStack.EMPTY);
        }
    }
}
