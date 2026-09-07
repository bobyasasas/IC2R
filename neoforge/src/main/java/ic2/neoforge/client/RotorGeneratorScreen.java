package ic2.neoforge.client;

import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.menu.MachineMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.text.DecimalFormat;
import java.util.List;

public final class RotorGeneratorScreen extends MachineScreen {
    private final DecimalFormat power = new DecimalFormat("0.##");

    public RotorGeneratorScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.text(
                font,
                Component.translatable(
                        menu.kind() == MachineKind.WIND_GENERATOR
                                ? "ic2.wind.obstructions"
                                : "ic2.water.nearby",
                        menu.familyValue(0)),
                leftPos + 78,
                topPos + 19,
                0xff404040,
                false);
        graphics.text(
                font,
                Component.translatable(
                        "ic2.tooltip.generation", power.format(menu.familyValue(1) / 100.0)),
                leftPos + 78,
                topPos + 56,
                0xff404040,
                false);
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        if (menu.kind() == MachineKind.WIND_GENERATOR
                && mouseX >= leftPos + 79
                && mouseX < leftPos + 103
                && mouseY >= topPos + 36
                && mouseY < topPos + 45)
            graphics.setComponentTooltipForNextFrame(
                    font,
                    List.of(
                            Component.translatable(
                                    "ic2.wind.overload", power.format(menu.progress() / 10.0))),
                    mouseX,
                    mouseY,
                    ItemStack.EMPTY);
    }
}
