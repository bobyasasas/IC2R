package ic2.neoforge.client;

import ic2.neoforge.menu.MachineMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Small shared screen; slot geometry is taken from the menu, so visuals cannot drift. */
public class MachineScreen extends ContainerScreenBase<MachineMenu> {
    public MachineScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, menu.kind().menuWidth(), menu.kind().menuHeight());
    }

    protected boolean showsEnergyBar() {
        return true;
    }

    protected boolean showsProgress() {
        return !menu.kind().energyDevice();
    }

    @Override
    protected Identifier backgroundTexture() {
        return LegacyMachineGui.background(menu);
    }

    /** Static machine art from a legacy atlas, drawn before live gauges and tank contents. */
    protected void drawLegacyMachineBackground(GuiGraphicsExtractor graphics) {}

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        drawLegacyMachineBackground(graphics);
        if (showsEnergyBar())
            LegacyMachineGui.drawGauge(
                    graphics,
                    leftPos,
                    topPos,
                    LegacyMachineGui.energy(menu.kind()),
                    menu.energy(),
                    menu.capacity());
        LegacyMachineGui.drawGauge(
                graphics,
                leftPos,
                topPos,
                LegacyMachineGui.fuel(menu.kind()),
                menu.fuelRemaining(),
                menu.fuelMaximum());
        if (showsProgress())
            LegacyMachineGui.drawGauge(
                    graphics,
                    leftPos,
                    topPos,
                    LegacyMachineGui.progress(menu.kind()),
                    menu.progress(),
                    menu.progressMaximum());
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        var energyGauge = LegacyMachineGui.energy(menu.kind());
        if (menu.capacity() > 0
                && LegacyMachineGui.contains(energyGauge, leftPos, topPos, mouseX, mouseY)) {
            graphics.setComponentTooltipForNextFrame(
                    font,
                    List.of(
                            Component.translatable(
                                    "ic2.tooltip.energy", menu.energy(), menu.capacity())),
                    mouseX,
                    mouseY,
                    ItemStack.EMPTY);
        }
    }
}
