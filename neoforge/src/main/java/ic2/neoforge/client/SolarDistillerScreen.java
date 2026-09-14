package ic2.neoforge.client;

import ic2.neoforge.menu.MachineMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Water and distilled tank displays with the sampled brightness in place of a heat bar. */
public final class SolarDistillerScreen extends MachineScreen {
    public SolarDistillerScreen(MachineMenu menu, Inventory inventory, Component title) {
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
        FluidTankDisplay.drawPlain(
                minecraft,
                graphics,
                leftPos + 37,
                topPos + 43,
                53,
                18,
                menu.tankFluid(false),
                menu.tankAmount(false),
                10000);
        FluidTankDisplay.drawPlain(
                minecraft,
                graphics,
                leftPos + 115,
                topPos + 55,
                17,
                43,
                menu.tankFluid(true),
                menu.tankAmount(true),
                10000);
        drawFittedText(graphics, Component.translatable("ic2.solar_distiller.sunlight"), 55, 18, 105, 0xff404040);
        drawFittedText(graphics, Component.literal(menu.progress() / 10 + "%"), 55, 28, 105, 0xff404040);
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        FluidTankDisplay.tooltip(
                minecraft,
                graphics,
                leftPos + 37,
                topPos + 43,
                53,
                18,
                mouseX,
                mouseY,
                menu.tankFluid(false),
                menu.tankAmount(false),
                10000);
        FluidTankDisplay.tooltip(
                minecraft,
                graphics,
                leftPos + 115,
                topPos + 55,
                17,
                43,
                mouseX,
                mouseY,
                menu.tankFluid(true),
                menu.tankAmount(true),
                10000);
    }
}
