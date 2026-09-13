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
        if (menu.tradeEditable()) return;
        int stock = menu.familyValue(1);
        graphics.text(
                font,
                Component.translatable("ic2.container.personalTrader.stock"),
                leftPos + 8,
                topPos + 46,
                0xff404040,
                false);
        graphics.text(
                font,
                Component.literal(stock < 0 ? "∞" : String.valueOf(stock)),
                leftPos + 48,
                topPos + 46,
                stock == 0 ? 0xff5555 : 0xff404040,
                false);
    }
}
