package ic2.neoforge.client;

import ic2.neoforge.menu.MachineMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Progress plus heat and air gauges; the furnace runs on HU heat, not EU. */
public final class BlastFurnaceScreen extends MachineScreen {
    public BlastFurnaceScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected boolean showsEnergyBar() {
        return false;
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        LegacyMachineGui.drawGauge(
                graphics,
                leftPos,
                topPos,
                LegacyMachineGui.GaugeSpec.heatCentrifuge(15, 34),
                Math.round(menu.familyFloat(0) * 1_000_000),
                1_000_000);
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        if (LegacyMachineGui.contains(
                LegacyMachineGui.GaugeSpec.heatCentrifuge(15, 34),
                leftPos,
                topPos,
                mouseX,
                mouseY))
            graphics.setComponentTooltipForNextFrame(
                    font,
                    List.of(Component.translatable("ic2.BlastFurnace.gui.heat")),
                    mouseX,
                    mouseY,
                    ItemStack.EMPTY);
    }
}
