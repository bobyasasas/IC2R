package ic2.neoforge.client;

import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.menu.MachineMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class HeatedMachineScreen extends MachineScreen {
    public HeatedMachineScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void drawLegacyMachineBackground(GuiGraphicsExtractor graphics) {
        if (menu.kind() == MachineKind.CENTRIFUGE) {
            LegacyMachineGui.blit(
                    graphics,
                    LegacyMachineGui.THERMAL_CENTRIFUGE,
                    leftPos + 40,
                    topPos + 18,
                    40,
                    18,
                    80,
                    60);
        } else {
            blitWhole(graphics, LegacyMachineGui.INDUCTION_INPUT, 42, 16, 34, 18);
            blitWhole(graphics, LegacyMachineGui.INDUCTION_OUTPUT, 110, 30, 38, 26);
        }
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        if (menu.kind() == MachineKind.CENTRIFUGE) {
            LegacyMachineGui.drawGauge(
                    graphics,
                    leftPos,
                    topPos,
                    LegacyMachineGui.GaugeSpec.heatCentrifuge(68, 67),
                    menu.familyValue(0),
                    menu.familyValue(1));
        } else {
            graphics.text(
                    font,
                    Component.translatable("ic2.generic.text.heat"),
                    leftPos + 10,
                    topPos + 36,
                    0xff404040,
                    false);
            graphics.text(
                    font,
                    Component.literal(Integer.toString(menu.familyValue(0))),
                    leftPos + 10,
                    topPos + 46,
                    0xff404040,
                    false);
        }
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        var heat = LegacyMachineGui.GaugeSpec.heatCentrifuge(68, 67);
        if (menu.kind() == MachineKind.CENTRIFUGE
                && LegacyMachineGui.contains(heat, leftPos, topPos, mouseX, mouseY))
            graphics.setComponentTooltipForNextFrame(
                    font,
                    List.of(
                            Component.translatable(
                                    "ic2.tooltip.processing_heat",
                                    menu.familyValue(0),
                                    menu.familyValue(1))),
                    mouseX,
                    mouseY,
                    ItemStack.EMPTY);
    }

    private void blitWhole(
            GuiGraphicsExtractor graphics,
            net.minecraft.resources.Identifier texture,
            int x,
            int y,
            int width,
            int height) {
        graphics.blit(
                net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,
                texture,
                leftPos + x,
                topPos + y,
                0.0F,
                0.0F,
                width,
                height,
                width,
                height);
    }
}
