package ic2.neoforge.client;

import ic2.neoforge.menu.MachineMenu;
import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.registration.ModMachines;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

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
                                        Component.literal("P"),
                                        button -> {
                                            if (minecraft.gameMode != null)
                                                minecraft.gameMode.handleInventoryButtonClick(
                                                        menu.containerId, 0);
                                        })
                                .bounds(leftPos + 152, topPos + 40, 18, 18)
                                .build());
        pumpButton.setMessage(Component.empty());
    }

    private Component pumpLabel() {
        return Component.translatable(
                menu.familyValue(0) == 1
                        ? "ic2.Miner.gui.pumpMode.on"
                        : "ic2.Miner.gui.pumpMode.off");
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.item(
                new ItemStack(ModMachines.block(MachineKind.PUMP)),
                leftPos + 153,
                topPos + 41);
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        pumpButton.setTooltip(Tooltip.create(pumpLabel()));
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }
}
