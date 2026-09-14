package ic2.neoforge.client;

import ic2.neoforge.menu.MachineMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class ElectrolyzerScreen extends MachineScreen {
    public ElectrolyzerScreen(MachineMenu menu, Inventory inventory, Component title) {
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
        FluidTankDisplay.draw(
                minecraft,
                graphics,
                leftPos + 76,
                topPos + 18,
                menu.tankFluid(false),
                menu.tankAmount(false),
                8000);
        drawFittedText(
                graphics,
                Component.translatable("ic2.tooltip.generation", menu.familyValue(0)),
                98, 20, 70, 0xff404040);
        drawFittedText(
                graphics,
                Component.literal(menu.progress() + " / " + menu.progressMaximum()),
                98, 34, 70, 0xff404040);
        drawFittedText(graphics, Component.translatable("ic2.electrolyzer.tanks"), 76, 60, 92, 0xff404040);
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        FluidTankDisplay.tooltip(
                minecraft,
                graphics,
                leftPos + 76,
                topPos + 18,
                mouseX,
                mouseY,
                menu.tankFluid(false),
                menu.tankAmount(false),
                8000);
    }
}
