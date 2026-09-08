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
            int x = leftPos + 54 + column * 36;
            addRenderableWidget(
                    Button.builder(Component.literal("+" + value), b -> send(plus))
                            .bounds(x, topPos + 18, 35, 18)
                            .build());
            addRenderableWidget(
                    Button.builder(Component.literal("-" + value), b -> send(minus))
                            .bounds(x, topPos + 50, 35, 18)
                            .build());
        }
        mode =
                addRenderableWidget(
                        Button.builder(modeLabel(), b -> send(menu.familyValue(2) == 0 ? 8 : 9))
                                .bounds(leftPos + 104, topPos + 72, 90, 18)
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
        mode.setMessage(modeLabel());
        FluidTankDisplay.draw(
                minecraft,
                graphics,
                leftPos + 8,
                topPos + 18,
                menu.tankFluid(false),
                menu.tankAmount(false),
                10000);
        graphics.text(
                font,
                Component.literal(menu.familyValue(0) + " mB"),
                leftPos + 96,
                topPos + 41,
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
                leftPos + 8,
                topPos + 18,
                mouseX,
                mouseY,
                menu.tankFluid(false),
                menu.tankAmount(false),
                10000);
    }
}
