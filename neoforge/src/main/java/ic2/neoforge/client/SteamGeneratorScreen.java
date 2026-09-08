package ic2.neoforge.client;

import ic2.core.machine.SteamBoiler;
import ic2.neoforge.menu.MachineMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.Locale;

public final class SteamGeneratorScreen extends MachineScreen {
    public SteamGeneratorScreen(MachineMenu menu, Inventory inventory, Component title) {
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
    public void init() {
        super.init();
        for (int column = 0; column < 4; column++) {
            int value = (int) Math.pow(10, column);
            button(column, "+" + value, 54 + column * 36, 18);
            button(column + 4, "-" + value, 54 + column * 36, 50);
            if (column < 3) {
                button(column + 8, "+" + value, 54 + column * 46, 76);
                button(column + 11, "-" + value, 54 + column * 46, 108);
            }
        }
    }

    private void button(int id, String label, int x, int y) {
        addRenderableWidget(
                Button.builder(
                                Component.literal(label),
                                button -> {
                                    if (minecraft.gameMode != null)
                                        minecraft.gameMode.handleInventoryButtonClick(
                                                menu.containerId, id);
                                })
                        .bounds(leftPos + x, topPos + y, 35, 18)
                        .build());
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        FluidTankDisplay.draw(
                minecraft,
                graphics,
                leftPos + 8,
                topPos + 18,
                menu.tankFluid(false),
                menu.tankAmount(false),
                10000);
        text(graphics, Component.literal(menu.familyValue(0) + " mB/t"), 80, 40);
        text(graphics, Component.literal(menu.familyValue(2) + " bar"), 80, 98);
        text(
                graphics,
                Component.literal(String.format(Locale.ROOT, "%.0f°C", menu.familyFloat(4))),
                8,
                60);
        text(graphics, Component.literal(menu.fuelRemaining() + " HU"), 8, 78);
        graphics.fill(leftPos + 8, topPos + 96, leftPos + 44, topPos + 102, 0xff747474);
        graphics.fill(
                leftPos + 8,
                topPos + 96,
                leftPos + 8 + 36 * menu.progress() / SteamBoiler.MAX_SCALE,
                topPos + 102,
                0xffaa7744);
        text(graphics, Component.literal(menu.progress() / 1000 + "%"), 8, 108);
        var outputs = SteamBoiler.Output.values();
        int type = Math.clamp(menu.familyValue(6), 0, outputs.length - 1);
        text(
                graphics,
                Component.literal(menu.familyValue(5) + " mB/t ")
                        .append(
                                Component.translatable(
                                        "ic2.boiler.output."
                                                + outputs[type].name().toLowerCase(Locale.ROOT))),
                8,
                132);
    }

    private void text(GuiGraphicsExtractor graphics, Component text, int x, int y) {
        graphics.text(font, text, leftPos + x, topPos + y, 0xff404040, false);
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        FluidTankDisplay.tooltip(
                minecraft,
                graphics,
                leftPos + 8,
                topPos + 18,
                mouseX,
                mouseY,
                menu.tankFluid(false),
                menu.tankAmount(false),
                10000);
        int x = mouseX - leftPos, y = mouseY - topPos;
        Component tooltip = null;
        if (x >= 8 && x < 48) {
            if (y >= 57 && y < 70)
                tooltip =
                        Component.translatable(
                                "ic2.boiler.temperature",
                                String.format(Locale.ROOT, "%.1f", menu.familyFloat(4)));
            else if (y >= 75 && y < 90)
                tooltip = Component.translatable("ic2.boiler.heat", menu.fuelRemaining());
            else if (y >= 94 && y < 120)
                tooltip =
                        Component.translatable(
                                "ic2.boiler.scale", menu.progress(), SteamBoiler.MAX_SCALE);
        } else if (x >= 54 && x < 194) {
            if (y >= 37 && y < 49) tooltip = Component.translatable("ic2.boiler.flow");
            else if (y >= 95 && y < 107) tooltip = Component.translatable("ic2.boiler.pressure");
        }
        if (tooltip != null) graphics.setTooltipForNextFrame(font, tooltip, mouseX, mouseY);
    }
}
