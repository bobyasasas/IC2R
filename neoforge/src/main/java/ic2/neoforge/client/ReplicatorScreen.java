package ic2.neoforge.client;

import ic2.neoforge.menu.MachineMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Replicator screen: pattern browsing and mode buttons (legacy last/next/single/repeat/stop). */
public final class ReplicatorScreen extends MachineScreen {
    public ReplicatorScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    public void init() {
        super.init();
        addRenderableWidget(
                Button.builder(
                                Component.translatable("ic2.Replicator.gui.info.last"),
                                b -> send(0))
                        .bounds(leftPos + 8, topPos + 62, 48, 16)
                        .build());
        addRenderableWidget(
                Button.builder(
                                Component.translatable("ic2.Replicator.gui.info.next"),
                                b -> send(1))
                        .bounds(leftPos + 60, topPos + 62, 48, 16)
                        .build());
        addRenderableWidget(
                Button.builder(
                                Component.translatable("ic2.Replicator.gui.info.Stop"),
                                b -> send(3))
                        .bounds(leftPos + 112, topPos + 62, 60, 16)
                        .build());
        addRenderableWidget(
                Button.builder(
                                Component.translatable("ic2.Replicator.gui.info.single"),
                                b -> send(4))
                        .bounds(leftPos + 8, topPos + 80, 76, 16)
                        .build());
        addRenderableWidget(
                Button.builder(
                                Component.translatable("ic2.Replicator.gui.info.repeat"),
                                b -> send(5))
                        .bounds(leftPos + 88, topPos + 80, 76, 16)
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
        int count = menu.familyValue(3);
        if (count > 0) {
            graphics.text(
                    font,
                    Component.literal(menu.familyValue(2) + " / " + count),
                    leftPos + 8,
                    topPos + 52,
                    0xff404040,
                    false);
        }
    }
}
