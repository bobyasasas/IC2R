package ic2.neoforge.client;

import ic2.neoforge.menu.MachineMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Scanner screen: input + crystal memory with the legacy status line, progress and buttons. */
public final class UuScannerScreen extends MachineScreen {
    private Button delete;
    private Button save;

    public UuScannerScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    public void init() {
        super.init();
        delete =
                addRenderableWidget(
                                Button.builder(
                                        Component.literal("×"),
                                        b -> send(0))
                                .bounds(leftPos + 102, topPos + 49, 12, 12)
                                .build());
        delete.setTooltip(
                Tooltip.create(Component.translatable("ic2.Scanner.gui.button.delete")));
        save =
                addRenderableWidget(
                        Button.builder(
                                        Component.literal("S"),
                                        b -> send(1))
                                .bounds(leftPos + 143, topPos + 49, 24, 12)
                                .build());
        save.setTooltip(Tooltip.create(Component.translatable("ic2.Scanner.gui.button.save")));
    }

    private void send(int id) {
        if (minecraft.gameMode != null)
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    /** Legacy enable handlers: delete on COMPLETED/TRANSFER_ERROR/FAILED, save without FAILED. */
    private void updateButtons() {
        int state = menu.familyValue(1);
        if (delete != null) delete.active = state >= 5;
        if (save != null) save.active = state == 6 || state == 7;
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        updateButtons();
        int x = leftPos, y = topPos;
        int state = menu.familyValue(1);
        graphics.text(
                font,
                Component.translatable("ic2.Scanner.gui.info5").getString() + ":",
                x + 123,
                y + 6,
                0xff404040,
                false);
        switch (state) {
            case 2 -> {
                status(graphics, "ic2.Scanner.gui.info1", 0xff20eb3e);
                int percent = menu.progressMaximum() <= 0 ? 0 : menu.progress() * 100 / menu.progressMaximum();
                graphics.text(
                        font, Component.literal(percent + "%"), x + 125, y + 69, 0xff20eb3e, false);
            }
            case 1 -> status(graphics, "ic2.Scanner.gui.info2", 0xffebeb20);
            case 3 -> status(graphics, "ic2.Scanner.gui.info3", 0xffd71010);
            case 4 -> status(graphics, "ic2.Scanner.gui.info8", 0xffd71010);
            case 5 -> {
                status(graphics, "ic2.Scanner.gui.info4", 0xff20eb3e);
                graphics.text(
                        font,
                        Component.translatable("ic2.Scanner.gui.info6"),
                        x + 110,
                        y + 30,
                        0xffd71010,
                        false);
            }
            case 6, 7 -> {
                if (state == 6) status(graphics, "ic2.Scanner.gui.info4", 0xff20eb3e);
                else status(graphics, "ic2.Scanner.gui.info7", 0xffd71010);
                graphics.text(
                        font,
                        Component.literal(
                                MeterScreen.toSiString(menu.familyFloat(3), 4) + "B UUM"),
                        x + 105,
                        y + 25,
                        0xffffff,
                        false);
                graphics.text(
                        font,
                        Component.literal(MeterScreen.toSiString(menu.familyFloat(4), 4) + "EU"),
                        x + 105,
                        y + 36,
                        0xffffff,
                        false);
            }
            default -> status(graphics, "ic2.Scanner.gui.idle", 0xffebeb20);
        }
    }

    private void status(GuiGraphicsExtractor graphics, String key, int color) {
        graphics.text(font, Component.translatable(key), leftPos + 10, topPos + 69, color, false);
    }
}
