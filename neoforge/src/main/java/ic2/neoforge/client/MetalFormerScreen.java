package ic2.neoforge.client;

import ic2.core.machine.MetalFormerMode;
import ic2.neoforge.menu.MachineMenu;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.registration.ModTools;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

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
                                        Component.literal("M"),
                                        button -> {
                                            if (minecraft.gameMode != null)
                                                minecraft.gameMode.handleInventoryButtonClick(
                                                        menu.containerId, 0);
                                        })
                                .bounds(leftPos + 65, topPos + 53, 20, 20)
                                .build());
        modeButton.setMessage(Component.empty());
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
    public void extractBackground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        ItemStack icon =
                switch (MetalFormerMode.byId(menu.familyValue(0))) {
                    case EXTRUDING -> new ItemStack(ModMachines.CABLES.get("copper_cable").get());
                    case ROLLING -> new ItemStack(ModTools.FORGE_HAMMER.get());
                    case CUTTING -> new ItemStack(ModTools.CUTTER.get());
                };
        graphics.item(icon, leftPos + 67, topPos + 55);
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        modeButton.setTooltip(Tooltip.create(modeLabel()));
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }
}
