package ic2.neoforge.client;

import ic2.core.machine.SteamBoiler;
import ic2.neoforge.menu.MachineMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
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
        inventoryLabelY = -1000;
        for (int column = 0; column < 4; column++) {
            addLegacyControl(92 + column * 10, 162, 9, 9, 3 - column, null);
            addLegacyControl(92 + column * 10, 186, 9, 9, 7 - column, null);
            if (column < 3) {
                addLegacyControl(23 + column * 10, 25, 9, 9, 10 - column, null);
                addLegacyControl(23 + column * 10, 49, 9, 9, 13 - column, null);
            }
        }
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        FluidTankDisplay.drawPlain(
                minecraft, graphics, leftPos + 10, topPos + 155, 75, 47,
                menu.tankFluid(false), menu.tankAmount(false), 10000);
        LegacyMachineGui.drawGauge(
                graphics, leftPos, topPos, LegacyMachineGui.GaugeSpec.heatSteamGenerator(13, 70),
                Math.round(menu.familyFloat(4)), 500);
        LegacyMachineGui.drawGauge(
                graphics, leftPos, topPos,
                LegacyMachineGui.GaugeSpec.calcificationSteamGenerator(155, 61),
                menu.progress(), SteamBoiler.MAX_SCALE);
        drawFittedText(
                graphics,
                Component.literal(menu.familyValue(0) + Component.translatable("ic2.generic.text.mb").getString()
                        + Component.translatable("ic2.generic.text.tick").getString()),
                91, 172, 59, 0xff20eb3e);
        drawFittedText(
                graphics,
                Component.translatable("ic2.steam_generator.gui.heatInput", menu.fuelRemaining()),
                35, 135, 107, 0xff20eb3e);
        drawFittedText(
                graphics,
                Component.translatable("ic2.steam_generator.gui.pressurevalve", menu.familyValue(2)),
                26, 37, 38, 0xff20eb3e);
        var outputs = SteamBoiler.Output.values();
        int type = Math.clamp(menu.familyValue(6), 0, outputs.length - 1);
        drawFittedText(
                graphics,
                Component.literal(menu.familyValue(5) + Component.translatable("ic2.generic.text.mb").getString()
                        + Component.translatable("ic2.generic.text.tick").getString()),
                70, 27, 77, 0xff20eb3e);
        drawFittedText(
                graphics,
                Component.translatable("ic2.boiler.output." + outputs[type].name().toLowerCase(Locale.ROOT)),
                70, 47, 96, 0xff20eb3e);
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        FluidTankDisplay.tooltip(
                minecraft,
                graphics,
                leftPos + 10,
                topPos + 155,
                75,
                47,
                mouseX,
                mouseY,
                menu.tankFluid(false),
                menu.tankAmount(false),
                10000);
        int x = mouseX - leftPos, y = mouseY - topPos;
        Component tooltip = null;
        if (x >= 13 && x < 20) {
            if (y >= 70 && y < 146)
                tooltip =
                        Component.translatable(
                                "ic2.steam_generator.gui.systemheat",
                                String.format(Locale.ROOT, "%.1f", menu.familyFloat(4)));
        } else if (x >= 155 && x < 162 && y >= 61 && y < 119) {
            tooltip = Component.translatable("ic2.steam_generator.gui.calcification", menu.progress() / 1000);
        }
        if (tooltip != null) graphics.setTooltipForNextFrame(font, tooltip, mouseX, mouseY);
    }
}
