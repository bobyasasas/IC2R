package ic2.neoforge.client;

import ic2.core.machine.CannerMode;
import ic2.neoforge.menu.MachineMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class CannerScreen extends MachineScreen {
    private Button modeButton;

    public CannerScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    public void init() {
        super.init();
        modeButton =
                addRenderableWidget(
                        Button.builder(
                                        modeLabel(),
                                        button -> sendButton((menu.cannerMode() + 1) % 4))
                                .bounds(leftPos + 77, topPos + 57, 64, 16)
                                .build());
        addRenderableWidget(
                Button.builder(Component.literal("↔"), button -> sendButton(5))
                        .bounds(leftPos + 144, topPos + 57, 24, 16)
                        .build());
    }

    private Component modeLabel() {
        return Component.translatable(
                "ic2.canner.mode."
                        + CannerMode.byId(Math.clamp(menu.cannerMode(), 0, 3)).serializedName());
    }

    private void sendButton(int id) {
        if (minecraft.gameMode != null)
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        drawTank(graphics, false, 8);
        drawTank(graphics, true, 151);
    }

    private void drawTank(GuiGraphicsExtractor graphics, boolean output, int offset) {
        FluidTankDisplay.draw(
                minecraft,
                graphics,
                leftPos + offset,
                topPos + 18,
                menu.tankFluid(output),
                menu.tankAmount(output),
                8000);
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        modeButton.setMessage(modeLabel());
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        for (boolean output : new boolean[] {false, true})
            FluidTankDisplay.tooltip(
                    minecraft,
                    graphics,
                    leftPos + (output ? 151 : 8),
                    topPos + 18,
                    mouseX,
                    mouseY,
                    menu.tankFluid(output),
                    menu.tankAmount(output),
                    8000);
    }
}
