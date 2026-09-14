package ic2.neoforge.client;

import ic2.core.machine.CannerMode;
import ic2.neoforge.menu.MachineMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class CannerScreen extends MachineScreen {
    public CannerScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    public void init() {
        super.init();
        // The tall legacy tanks occupy the vanilla inventory-title row.
        inventoryLabelY = -1000;
        addLegacyControl(
                63,
                81,
                50,
                14,
                button ->
                        button == 0
                                ? (menu.cannerMode() + 1) % 4
                                : button == 1 ? Math.floorMod(menu.cannerMode() - 1, 4) : -1,
                this::modeLabel,
                () -> true);
        addLegacyControl(
                77,
                64,
                22,
                13,
                5,
                () -> Component.translatable("ic2.Canner.gui.switchTanks"));
    }

    private Component modeLabel() {
        return Component.translatable(
                "ic2.canner.mode."
                        + CannerMode.byId(Math.clamp(menu.cannerMode(), 0, 3)).serializedName());
    }

    @Override
    protected void drawLegacyMachineBackground(GuiGraphicsExtractor graphics) {
        int mode = Math.clamp(menu.cannerMode(), 0, 3);
        LegacyMachineGui.blit(
                graphics,
                backgroundTexture(),
                leftPos + 63,
                topPos + 81,
                176,
                18 + mode * 14,
                50,
                14);
        switch (CannerMode.byId(mode)) {
            case BOTTLE_SOLID -> {
                LegacyMachineGui.blit(
                        graphics, backgroundTexture(), leftPos + 59, topPos + 53, 3, 4, 9, 18);
                LegacyMachineGui.blit(
                        graphics, backgroundTexture(), leftPos + 99, topPos + 53, 3, 4, 18, 23);
            }
            case EMPTY_LIQUID -> {
                LegacyMachineGui.blit(
                        graphics, backgroundTexture(), leftPos + 71, topPos + 43, 196, 0, 26, 18);
                LegacyMachineGui.blit(
                        graphics, backgroundTexture(), leftPos + 59, topPos + 53, 3, 4, 9, 18);
            }
            case BOTTLE_LIQUID -> {
                LegacyMachineGui.blit(
                        graphics, backgroundTexture(), leftPos + 99, topPos + 53, 3, 4, 18, 23);
                LegacyMachineGui.blit(
                        graphics, backgroundTexture(), leftPos + 71, topPos + 43, 196, 0, 26, 18);
            }
            case ENRICH_LIQUID -> {}
        }
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        drawTank(graphics, false, 39);
        drawTank(graphics, true, 117);
    }

    private void drawTank(GuiGraphicsExtractor graphics, boolean output, int offset) {
        FluidTankDisplay.drawNormal(
                minecraft,
                graphics,
                leftPos + offset,
                topPos + 42,
                menu.tankFluid(output),
                menu.tankAmount(output),
                8000);
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        for (boolean output : new boolean[] {false, true})
            FluidTankDisplay.tooltip(
                    minecraft,
                    graphics,
                    leftPos + (output ? 117 : 39),
                    topPos + 42,
                    20,
                    55,
                    mouseX,
                    mouseY,
                    menu.tankFluid(output),
                    menu.tankAmount(output),
                    8000);
    }
}
