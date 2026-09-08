package ic2.neoforge.client;

import ic2.neoforge.menu.MachineMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class CondenserScreen extends MachineScreen {
    public CondenserScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected boolean showsProgress() {
        return false;
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        for (boolean output : new boolean[] {false, true})
            FluidTankDisplay.draw(
                    minecraft,
                    graphics,
                    leftPos + (output ? 153 : 8),
                    topPos + 18,
                    menu.tankFluid(output),
                    menu.tankAmount(output),
                    output ? 1000 : 100000);
        graphics.text(
                font,
                Component.literal((100 + menu.familyValue(0) * 100) + " mB/t"),
                leftPos + 54,
                topPos + 40,
                0xff404040,
                false);
        graphics.text(
                font,
                Component.literal(menu.familyValue(0) * 2 + " EU/t"),
                leftPos + 54,
                topPos + 52,
                0xff404040,
                false);
        graphics.fill(leftPos + 54, topPos + 65, leftPos + 124, topPos + 71, 0xff747474);
        graphics.fill(
                leftPos + 54,
                topPos + 65,
                leftPos + 54 + Math.clamp(70 * menu.progress() / 10000, 0, 70),
                topPos + 71,
                0xffe9ae23);
        graphics.text(
                font,
                Component.literal(menu.progress() + " mB"),
                leftPos + 54,
                topPos + 78,
                0xff404040,
                false);
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        for (boolean output : new boolean[] {false, true})
            FluidTankDisplay.tooltip(
                    minecraft,
                    graphics,
                    leftPos + (output ? 153 : 8),
                    topPos + 18,
                    mouseX,
                    mouseY,
                    menu.tankFluid(output),
                    menu.tankAmount(output),
                    output ? 1000 : 100000);
    }
}
