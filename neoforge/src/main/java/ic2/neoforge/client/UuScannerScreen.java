package ic2.neoforge.client;

import ic2.neoforge.menu.MachineMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Scanner screen: input + crystal memory with progress and the legacy delete/save buttons. */
public final class UuScannerScreen extends MachineScreen {
    public UuScannerScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    public void init() {
        super.init();
        addRenderableWidget(
                Button.builder(
                                Component.translatable("ic2.Scanner.gui.button.delete"),
                                b -> send(0))
                        .bounds(leftPos + 8, topPos + 62, 76, 16)
                        .build());
        addRenderableWidget(
                Button.builder(
                                Component.translatable("ic2.Scanner.gui.button.save"), b -> send(1))
                        .bounds(leftPos + 92, topPos + 62, 76, 16)
                        .build());
    }

    private void send(int id) {
        if (minecraft.gameMode != null)
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        var status = menu.machine()
                        instanceof ic2.neoforge.machine.UuScannerBlockEntity scanner
                ? Component.literal(scanner.state())
                : Component.empty();
        graphics.text(font, status, leftPos + 8, topPos + 52, 0xff404040, false);
    }
}
