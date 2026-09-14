package ic2.neoforge.client;

import ic2.neoforge.menu.MachineMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.player.Inventory;

/** Legacy GuiTradeOMatOpen/Closed: operators get the infinite toggle, visitors the stock line. */
public final class TradeOMatScreen extends MachineScreen {
    public TradeOMatScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected boolean showsEnergyBar() {
        // Legacy Trade-O-Mat trades goods, never EU; no bar is drawn.
        return false;
    }

    @Override
    protected boolean showsProgress() {
        return false;
    }

    @Override
    public void init() {
        super.init();
        if (!operator()) return;
        addRenderableWidget(
                Button.builder(Component.literal("∞"), b -> send(0))
                        .bounds(leftPos + 152, topPos + 4, 20, 20)
                        .build());
    }

    private void send(int id) {
        if (minecraft.gameMode != null)
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    /** The synced permission set mirrors the legacy server-side isOp probe of the ∞ button. */
    private boolean operator() {
        return minecraft.player != null
                && minecraft.player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        drawFittedText(
                graphics,
                Component.translatable("container.inventory"),
                8,
                imageHeight - 94,
                88,
                0xff404040);
        int stock = menu.familyValue(1);
        if (menu.tradeEditable()) {
            drawFittedText(
                    graphics,
                    Component.translatable("ic2.container.personalTrader.want"),
                    12, 23, 36, 0xff404040);
            drawFittedText(
                    graphics,
                    Component.translatable("ic2.container.personalTrader.offer"),
                    12, 57, 36, 0xff404040);
            drawFittedText(
                    graphics,
                    Component.translatable("ic2.container.personalTrader.totalTrades0"),
                    108, 28, 60, 0xff404040);
            drawFittedText(
                    graphics,
                    Component.translatable("ic2.container.personalTrader.totalTrades1"),
                    108, 36, 60, 0xff404040);
            drawFittedText(graphics, Component.literal(String.valueOf(menu.familyValue(2))), 112, 44, 56, 0xff404040);
            drawFittedText(
                    graphics,
                    Component.translatable("ic2.container.personalTrader.stock")
                            .append(" " + (stock < 0 ? "∞" : stock)),
                    108, 60, 60, 0xff404040);
        } else {
            drawFittedText(
                    graphics,
                    Component.translatable("ic2.container.personalTrader.want"),
                    12, 23, 36, 0xff404040);
            drawFittedText(
                    graphics,
                    Component.translatable("ic2.container.personalTrader.offer"),
                    12, 42, 36, 0xff404040);
            drawFittedText(
                    graphics,
                    Component.translatable("ic2.container.personalTrader.stock"),
                    12, 60, 36, 0xff404040);
            drawFittedText(
                    graphics,
                    Component.literal(stock < 0 ? "∞" : String.valueOf(stock)),
                    50, 60, 80, stock == 0 ? 0xffff5555 : 0xff404040);
        }
    }
}
