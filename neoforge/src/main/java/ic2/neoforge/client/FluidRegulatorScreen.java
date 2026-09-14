package ic2.neoforge.client;

import ic2.neoforge.menu.MachineMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class FluidRegulatorScreen extends MachineScreen {
    private Button mode;

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
            int plus = column, minus = column + 4, value = (int) Math.pow(10, column);
            int x = leftPos + 102 + column * 10;
            addRenderableWidget(
                    Button.builder(Component.literal("+"), b -> send(plus))
                            .bounds(x, topPos + 44, 9, 9)
                            .build());
            addRenderableWidget(
                    Button.builder(Component.literal("-"), b -> send(minus))
                            .bounds(x, topPos + 68, 9, 9)
                            .build());
        }
        mode =
                addRenderableWidget(
                        Button.builder(Component.literal("M"), b -> send(menu.familyValue(2) == 0 ? 8 : 9))
                                .bounds(leftPos + 152, topPos + 44, 9, 9)
                                .build());
    }

    private Component modeLabel() {
        return Component.translatable(
                menu.familyValue(2) == 0 ? "ic2.regulator.second" : "ic2.regulator.tick");
    }

    private void send(int id) {
        if (minecraft.gameMode != null)
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        mode.setTooltip(net.minecraft.client.gui.components.Tooltip.create(modeLabel()));
        FluidTankDisplay.drawNormal(
                minecraft,
                graphics,
                leftPos + 78,
                topPos + 34,
                menu.tankFluid(false),
                menu.tankAmount(false),
                10000);
        graphics.text(
                font,
                Component.literal(menu.familyValue(0) + " mB"),
                leftPos + 105,
                topPos + 57,
                0xff404040,
                false);
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
