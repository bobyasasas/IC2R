package ic2.neoforge.client;

import ic2.neoforge.menu.ScannerMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Legacy GuiToolScanner: a "Find:" heading, up to ten "count x name" rows ordered by pile size,
 * and the ore icons in the legacy two-column strip beside them.
 */
public final class ScannerScreen extends ContainerScreenBase<ScannerMenu> {
    private static final int FOUND_COLOR = 2157374;
    private static final int ROW_COLOR = 5752026;

    public ScannerScreen(ScannerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 230, 234);
    }

    @Override
    public void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        int x = leftPos, y = topPos;
        graphics.text(
                font,
                Component.translatable("ic2.scanner.found"),
                x + 10,
                y + 20,
                FOUND_COLOR,
                false);
        for (int row = 0; row < menu.resultCount(); row++) {
            graphics.text(
                    font,
                    Component.literal(menu.resultTotal(row) + " x ")
                            .append(menu.resultStack(row).getHoverName()),
                    x + 10,
                    y + 34 + row * 11,
                    ROW_COLOR,
                    false);
            graphics.item(
                    menu.resultStack(row),
                    x + 135 + (row & 1) * 15,
                    y + 28 + row * 11);
        }
    }
}
