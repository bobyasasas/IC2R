package ic2.neoforge.client;

import ic2.core.energy.VoltageTier;
import ic2.neoforge.menu.MachineMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Shared storage/transformer/chargepad controls; server-side policies own each button's meaning. */
public final class EnergyDeviceScreen extends MachineScreen {
    private Button modeButton;

    public EnergyDeviceScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    public void init() {
        super.init();
        if (menu.kind().transformer()) {
            for (int mode = 0; mode < 3; mode++) {
                int selectedMode = mode;
                addRenderableWidget(
                        Button.builder(
                                        Component.translatable(
                                                "ic2.Transformer.gui.switch.mode" + (mode + 1)),
                                        button -> send(selectedMode))
                                .bounds(leftPos + 7, topPos + 65 + mode * 20, 144, 20)
                                .build());
            }
            return;
        }
        modeButton =
                addRenderableWidget(
                        Button.builder(
                                        Component.literal("R"),
                                        button ->
                                                send(
                                                        (menu.familyValue(0) + 1)
                                                                % (menu.kind().storage() ? 7 : 2)))
                                .bounds(leftPos + 152, topPos + 4, 20, 20)
                                .build());
    }

    private void send(int value) {
        if (minecraft.gameMode != null)
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, value);
    }

    private Component description() {
        if (menu.kind().chargepad())
            return Component.translatable(
                    "ic2.blockChargepad.gui.mod.redstone" + menu.familyValue(0));
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
            modeButton.setTooltip(Tooltip.create(description()));
        }
        if (menu.kind().transformer()) {
            graphics.text(
                    font,
                    Component.translatable(
                            "ic2.energy_device.input", menu.familyValue(1), menu.familyValue(2)),
                    leftPos + 8,
                    topPos + 28,
                    0xff404040,
                    false);
            graphics.text(
                    font,
                    Component.translatable(
                            "ic2.energy_device.output", menu.familyValue(3), menu.familyValue(4)),
                    leftPos + 8,
                    topPos + 44,
                    0xff404040,
                    false);
        } else {
            var tier = VoltageTier.fromIcTier(menu.kind().electricalTier());
            graphics.text(
                    font,
                    Component.translatable(
                            "ic2.EUStorage.gui.info.level",
                            Component.translatable(tier.getTranslationKey())),
                    leftPos + (menu.kind().chargepad() ? 79 : 82),
                    topPos + (menu.kind().chargepad() ? 25 : 24),
                    0xff404040,
                    false);
            graphics.text(
                    font,
                    Component.literal(" " + menu.energy()),
                    leftPos + 110,
                    topPos + 34,
                    0xff404040,
                    false);
            graphics.text(
                    font,
                    Component.literal("/" + menu.capacity()),
                    leftPos + 110,
                    topPos + 44,
                    0xff404040,
                    false);
            if (menu.kind().storage()) {
                // Legacy GuiElectricBlock labels the worn-armor row above the player slots.
                graphics.text(
                        font,
                        Component.translatable("ic2.EUStorage.gui.info.armor"),
                        leftPos + 8,
                        topPos + 74,
                        0xff404040,
                        false);
                graphics.text(
                        font,
                        Component.translatable("ic2.EUStorage.gui.info.output"),
                        leftPos + 82,
                        topPos + 59,
                        0xff404040,
                        false);
                graphics.text(
                        font,
                        Component.literal(tier.getVoltage() + " EU/t"),
                        leftPos + 82,
                        topPos + 69,
                        0xff404040,
                        false);
            }
        }
    }
}
