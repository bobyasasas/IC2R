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
        for (boolean output : new boolean[] {false, true})
            FluidTankDisplay.draw(
                    minecraft,
                    graphics,
                    leftPos + (output ? 153 : 8),
                    topPos + 18,
                    menu.tankFluid(output),
                    menu.tankAmount(output),
                    10000);
        graphics.text(
                font,
                Component.translatable("ic2.solar_distiller.sunlight"),
                leftPos + 55,
                topPos + 18,
                0xff404040,
                false);
        graphics.text(
                font,
                Component.literal(menu.progress() / 10 + "%"),
                leftPos + 55,
                topPos + 28,
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
                    10000);
    }
}
