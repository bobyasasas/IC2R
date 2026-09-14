package ic2.neoforge.client;

import ic2.neoforge.menu.MachineMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class FluidRegulatorScreen extends MachineScreen {
    public FluidRegulatorScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected boolean showsProgress() {
        return false;
    }

    @Override
    public void init() {
        super.init();
        for (int column = 0; column < 4; column++) {
            int plus = 3 - column;
            int minus = 7 - column;
            int x = 102 + column * 10;
            addLegacyControl(x, 44, 9, 9, plus, null);
            addLegacyControl(x, 68, 9, 9, minus, null);
        }
        addLegacyControl(152, 44, 9, 9, 8, this::modeLabel);
        addLegacyControl(152, 68, 9, 9, 9, this::modeLabel);
    }

    private Component modeLabel() {
        return Component.translatable(
                menu.familyValue(2) == 0 ? "ic2.regulator.second" : "ic2.regulator.tick");
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        FluidTankDisplay.drawNormal(
                minecraft,
                graphics,
                leftPos + 78,
                topPos + 34,
                menu.tankFluid(false),
                menu.tankAmount(false),
                10000);
        drawFittedText(graphics, Component.literal(menu.familyValue(0) + " mB"), 105, 57, 38, 0xff20eb3e);
        drawFittedText(
                graphics,
                Component.translatable(
                        menu.familyValue(2) == 0
                                ? "ic2.generic.text.sec"
                                : "ic2.generic.text.tick"),
                145,
                57,
                24,
                0xff20eb3e);
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        FluidTankDisplay.tooltip(
                minecraft,
                graphics,
                leftPos + 78,
                topPos + 34,
                20,
                55,
                mouseX,
                mouseY,
                menu.tankFluid(false),
                menu.tankAmount(false),
                10000);
    }
}
