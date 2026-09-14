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
        FluidTankDisplay.drawPlain(
                minecraft,
                graphics,
                leftPos + 46,
                topPos + 27,
                84,
                33,
                menu.tankFluid(false),
                menu.tankAmount(false),
                100000);
        FluidTankDisplay.drawPlain(
                minecraft,
                graphics,
                leftPos + 46,
                topPos + 74,
                84,
                15,
                menu.tankFluid(true),
                menu.tankAmount(true),
                1000);
        LegacyMachineGui.drawGauge(
                graphics,
                leftPos,
                topPos,
                LegacyMachineGui.GaugeSpec.progressCondenser(47, 63),
                menu.progress(),
                10000);
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        FluidTankDisplay.tooltip(
                minecraft,
                graphics,
                leftPos + 46,
                topPos + 27,
                84,
                33,
                mouseX,
                mouseY,
                menu.tankFluid(false),
                menu.tankAmount(false),
                100000);
        FluidTankDisplay.tooltip(
                minecraft,
                graphics,
                leftPos + 46,
                topPos + 74,
                84,
                15,
                mouseX,
                mouseY,
                menu.tankFluid(true),
                menu.tankAmount(true),
                1000);
    }
}
