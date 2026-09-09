package ic2.neoforge.client;

import ic2.neoforge.menu.MachineMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Adds the legacy pump-mode toggle below the tool slots. */
public final class MinerScreen extends MachineScreen {
    private Button pumpButton;

    public MinerScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    public void init() {
        super.init();
        pumpButton =
                addRenderableWidget(
                        Button.builder(
                                        pumpLabel(),
                                        button -> {
                                            if (minecraft.gameMode != null)
                                                minecraft.gameMode.handleInventoryButtonClick(
                                                        menu.containerId, 0);
                                        })
                                .bounds(leftPos + 8, topPos + 72, 26, 16)
                                .build());
    }

    private Component pumpLabel() {
        return Component.translatable(
                menu.familyValue(0) == 1
                        ? "ic2.Miner.gui.pumpMode.on"
                        : "ic2.Miner.gui.pumpMode.off");
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        pumpButton.setMessage(pumpLabel());
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }
}
