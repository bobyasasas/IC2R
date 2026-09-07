package ic2.neoforge.client;

import ic2.neoforge.menu.MachineMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Shared storage/transformer controls; server-side policies own each button's meaning. */
public final class EnergyDeviceScreen extends MachineScreen {
    private Button modeButton;

    public EnergyDeviceScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    public void init() {
        super.init();
        modeButton =
                addRenderableWidget(
                        Button.builder(
                                        label(),
                                        button -> {
                                            if (minecraft.gameMode != null)
                                                minecraft.gameMode.handleInventoryButtonClick(
                                                        menu.containerId,
                                                        (menu.familyValue(0) + 1)
                                                                % (menu.kind().storage() ? 7 : 3));
                                        })
                                .bounds(leftPos + 76, topPos + 58, 92, 18)
                                .build());
    }

    private Component label() {
        return Component.translatable(
                "ic2.energy_device."
                        + (menu.kind().storage() ? "storage." : "transformer.")
                        + menu.familyValue(0));
    }

    private Component description() {
        return Component.translatable(
                menu.kind().storage()
                        ? "ic2.EUStorage.gui.mod.redstone" + menu.familyValue(0)
                        : "ic2.Transformer.gui.switch.mode" + (menu.familyValue(0) + 1));
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        if (modeButton != null) {
            modeButton.setMessage(label());
            modeButton.setTooltip(Tooltip.create(description()));
        }
        if (menu.kind().transformer()) {
            graphics.text(
                    font,
                    Component.translatable(
                            "ic2.energy_device.input", menu.familyValue(1), menu.familyValue(2)),
                    leftPos + 45,
                    topPos + 22,
                    0xff404040,
                    false);
            graphics.text(
                    font,
                    Component.translatable(
                            "ic2.energy_device.output", menu.familyValue(3), menu.familyValue(4)),
                    leftPos + 45,
                    topPos + 38,
                    0xff404040,
                    false);
        } else {
            graphics.text(
                    font,
                    Component.translatable("ic2.energy_device.charge"),
                    leftPos + 78,
                    topPos + 21,
                    0xff404040,
                    false);
            graphics.text(
                    font,
                    Component.translatable("ic2.energy_device.discharge"),
                    leftPos + 78,
                    topPos + 43,
                    0xff404040,
                    false);
        }
    }
}
