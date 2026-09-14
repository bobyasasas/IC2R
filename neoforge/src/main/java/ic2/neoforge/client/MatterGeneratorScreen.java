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
        LegacyMachineGui.blit(
                graphics,
                LegacyMachineGui.COMMON,
                leftPos + 96,
                topPos + 22,
                70,
                100,
                20,
                55);
        int fluidHeight = (int) Math.clamp((long) (47.0F * menu.familyFloat(0)), 0, 47);
        graphics.fill(
                leftPos + 100,
                topPos + 73 - fluidHeight,
                leftPos + 112,
                topPos + 73,
                0xff35c9ff);
        LegacyMachineGui.blit(
                graphics,
                LegacyMachineGui.COMMON,
                leftPos + 96,
                topPos + 22,
                38,
                100,
                20,
                55);
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        if (mouseX >= leftPos + 96
                && mouseX < leftPos + 116
                && mouseY >= topPos + 22
                && mouseY < topPos + 77) {
            graphics.setComponentTooltipForNextFrame(
                    font,
                    List.of(Component.translatable("ic2.Matter.gui.info.progress")),
                    mouseX,
                    mouseY,
                    ItemStack.EMPTY);
        }
    }
}
