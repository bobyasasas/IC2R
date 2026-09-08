package ic2.neoforge.client;

import ic2.neoforge.menu.MachineMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Two tanks and paid heat are displayed directly from full-width native menu data. */
public final class FermenterScreen extends MachineScreen {
    public FermenterScreen(MachineMenu menu, Inventory inventory, Component title) {
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
                    output ? 2000 : 10000);
        graphics.text(
                font,
                Component.translatable("ic2.fermenter.heat"),
                leftPos + 55,
                topPos + 18,
                0xff404040,
                false);
        graphics.text(
                font,
                Component.literal(menu.fuelRemaining() + " / " + menu.fuelMaximum()),
                leftPos + 55,
                topPos + 28,
                0xff404040,
                false);
        int width =
                (int)
                        Math.clamp(
                                65L * menu.fuelRemaining() / Math.max(1, menu.fuelMaximum()),
                                0,
                                65);
        graphics.fill(leftPos + 55, topPos + 40, leftPos + 120, topPos + 45, 0xff373737);
        graphics.fill(leftPos + 55, topPos + 40, leftPos + 55 + width, topPos + 45, 0xffe9ae23);
        int progress =
                (int)
                        Math.clamp(
                                100L * menu.progress() / Math.max(1, menu.progressMaximum()),
                                0,
                                100);
        graphics.text(
                font,
                Component.literal(progress + "%"),
                leftPos + 101,
                topPos + 58,
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
                    output ? 2000 : 10000);
    }
}
