package ic2.neoforge.client;

import ic2.neoforge.menu.MachineMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.text.DecimalFormat;

public final class WaterGeneratorScreen extends MachineScreen {
    private final DecimalFormat power = new DecimalFormat("0.##");

    public WaterGeneratorScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.text(
                font,
                Component.translatable("ic2.water.nearby", menu.familyValue(0)),
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
}
