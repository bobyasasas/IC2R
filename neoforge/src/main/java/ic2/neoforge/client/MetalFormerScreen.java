package ic2.neoforge.client;

import ic2.core.machine.MetalFormerMode;
import ic2.neoforge.menu.MachineMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class MetalFormerScreen extends MachineScreen {
    private Button modeButton;

    public MetalFormerScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    public void init() {
        super.init();
        modeButton =
                addRenderableWidget(
                        Button.builder(
                                        modeLabel(),
                                        button -> {
                                            if (minecraft.gameMode != null)
                                                minecraft.gameMode.handleInventoryButtonClick(
                                                        menu.containerId, 0);
                                        })
                                .bounds(leftPos + 77, topPos + 57, 90, 16)
                                .build());
    }

    private Component modeLabel() {
        return Component.translatable(
                "ic2.MetalFormer.gui.switch."
                        + switch (MetalFormerMode.byId(menu.familyValue(0))) {
                            case EXTRUDING -> "Extruding";
                            case ROLLING -> "Rolling";
                            case CUTTING -> "Cutting";
                        });
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        modeButton.setMessage(modeLabel());
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }
}
