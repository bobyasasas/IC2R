package ic2.neoforge.client;

import ic2.neoforge.menu.MachineMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class FluidDistributorScreen extends MachineScreen {
    private Button toggle;

    public FluidDistributorScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected boolean showsEnergyBar() {
        return false;
    }

    @Override
    protected boolean showsProgress() {
        return false;
    }

    @Override
    public void init() {
        super.init();
        toggle =
                addRenderableWidget(
                        Button.builder(modeLabel(), button -> send(0))
                                .bounds(leftPos + 66, topPos + 18, 44, 18)
                                .build());
    }

    private Component modeLabel() {
        return Component.translatable(
                menu.familyValue(0) == 1 ? "ic2.distributor.active" : "ic2.distributor.idle");
    }

    private void send(int id) {
        if (minecraft.gameMode != null)
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        toggle.setMessage(modeLabel());
        int amount = menu.familyValue(1);
        if (amount > 0)
            graphics.text(
                    font,
                    Component.literal(amount + " mB"),
                    leftPos + 8,
                    topPos + 42,
                    0xff404040,
                    false);
    }
}
