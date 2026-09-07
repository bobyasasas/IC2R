package ic2.neoforge.client;

import ic2.neoforge.menu.MachineMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class CentrifugeScreen extends MachineScreen {
    public CentrifugeScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int height =
                (int)
                        Math.clamp(
                                48L * menu.familyValue(0) / Math.max(1, menu.familyValue(1)),
                                0,
                                48);
        graphics.fill(leftPos + 8, topPos + 18, leftPos + 20, topPos + 68, 0xff373737);
        graphics.fill(leftPos + 9, topPos + 67 - height, leftPos + 19, topPos + 67, 0xffe86d30);
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        if (mouseX >= leftPos + 8
                && mouseX < leftPos + 20
                && mouseY >= topPos + 18
                && mouseY < topPos + 68)
            graphics.setComponentTooltipForNextFrame(
                    font,
                    List.of(
                            Component.translatable(
                                    "ic2.tooltip.processing_heat",
                                    menu.familyValue(0),
                                    menu.familyValue(1))),
                    mouseX,
                    mouseY,
                    ItemStack.EMPTY);
    }
}
