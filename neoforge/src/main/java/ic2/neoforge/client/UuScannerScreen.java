package ic2.neoforge.client;

import ic2.neoforge.menu.MachineMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Scanner screen: input + crystal memory with the legacy status line, progress and buttons. */
public final class UuScannerScreen extends MachineScreen {
    public UuScannerScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    public void init() {
        super.init();
        addLegacyControl(
                102, 49, 12, 12, button -> button == 0 ? 0 : -1,
                () -> Component.translatable("ic2.Scanner.gui.button.delete"),
                () -> menu.familyValue(1) >= 5);
        addLegacyControl(
                143, 49, 24, 12, button -> button == 0 ? 1 : -1,
                () -> Component.translatable("ic2.Scanner.gui.button.save"),
                () -> menu.familyValue(1) == 6 || menu.familyValue(1) == 7);
    }

    @Override
    protected void drawLegacyMachineBackground(GuiGraphicsExtractor graphics) {
        int state = menu.familyValue(1);
        if (state >= 5)
            LegacyMachineGui.blit(graphics, backgroundTexture(), leftPos + 102, topPos + 49, 176, 57, 12, 12);
        if (state == 6 || state == 7)
            LegacyMachineGui.blit(graphics, backgroundTexture(), leftPos + 143, topPos + 49, 176, 69, 24, 12);
        if (state == 2 && menu.progressMaximum() > 0) {
            int scanning = Math.clamp(Math.round(menu.progress() * 43.0F / menu.progressMaximum()), 0, 43);
            if (scanning > 0)
                LegacyMachineGui.blit(
                        graphics,
                        backgroundTexture(),
                        leftPos + 30,
                        topPos + 63 - scanning,
                        176,
                        57 - scanning,
                        66,
                        scanning);
        }
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int x = leftPos, y = topPos;
        int state = menu.familyValue(1);
        drawFittedText(
                graphics,
                Component.literal(Component.translatable("ic2.Scanner.gui.info5").getString() + ":"),
                123, 6, 45, 0xff404040);
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
                drawFittedText(graphics, Component.translatable("ic2.Scanner.gui.info6"), 110, 30, 58, 0xffd71010);
            }
            case 6, 7 -> {
                if (state == 6) status(graphics, "ic2.Scanner.gui.info4", 0xff20eb3e);
                else status(graphics, "ic2.Scanner.gui.info7", 0xffd71010);
                drawFittedText(
                        graphics,
                        Component.literal(MeterScreen.toSiString(menu.familyFloat(3), 4) + "B UUM"),
                        105,
                        25,
                        63,
                        0xffffff);
                drawFittedText(
                        graphics,
                        Component.literal(MeterScreen.toSiString(menu.familyFloat(4), 4) + "EU"),
                        105,
                        36,
                        63,
                        0xffffff);
            }
            default -> status(graphics, "ic2.Scanner.gui.idle", 0xffebeb20);
        }
    }

    private void status(GuiGraphicsExtractor graphics, String key, int color) {
        drawFittedText(graphics, Component.translatable(key), 10, 69, 110, color);
    }
}
