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
        LegacyMachineGui.drawGauge(
                graphics,
                leftPos,
                topPos,
                LegacyMachineGui.GaugeSpec.heatFermenter(42, 41),
                menu.fuelRemaining(),
                menu.fuelMaximum());
        LegacyMachineGui.drawGauge(
                graphics,
                leftPos,
                topPos,
                LegacyMachineGui.GaugeSpec.progressFermenter(38, 88),
                menu.progress(),
                menu.progressMaximum());
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
