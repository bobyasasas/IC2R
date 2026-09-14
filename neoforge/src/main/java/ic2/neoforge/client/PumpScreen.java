package ic2.neoforge.client;

import ic2.neoforge.machine.PumpBlockEntity;
import ic2.neoforge.menu.MachineMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Legacy pump arrow and tank, with the fluid read from the synchronized machine state. */
public final class PumpScreen extends MachineScreen {
    public PumpScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void drawLegacyMachineBackground(GuiGraphicsExtractor graphics) {
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                LegacyMachineGui.PUMP_ARROW,
                leftPos + 93,
                topPos + 36,
                0.0F,
                0.0F,
                36,
                13,
                36,
                13);
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        FluidTankDisplay.drawNormal(
                minecraft,
                graphics,
                leftPos + 70,
                topPos + 16,
                BuiltInRegistries.FLUID.byId(menu.familyValue(1)),
                menu.familyValue(0),
                PumpBlockEntity.TANK);
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        FluidTankDisplay.tooltip(
                minecraft,
                graphics,
                leftPos + 70,
                topPos + 16,
                20,
                55,
                mouseX,
                mouseY,
                BuiltInRegistries.FLUID.byId(menu.familyValue(1)),
                menu.familyValue(0),
                PumpBlockEntity.TANK);
    }
}
