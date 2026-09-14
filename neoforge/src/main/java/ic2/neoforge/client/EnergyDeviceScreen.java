package ic2.neoforge.client;

import ic2.core.energy.VoltageTier;
import ic2.neoforge.menu.MachineMenu;
import ic2.neoforge.registration.ModTools;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

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
        modeButton.setMessage(Component.empty());
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
            drawFittedText(graphics, Component.translatable("ic2.Transformer.gui.Output"), 8, 28, 42, 0xff404040);
            drawFittedText(
                    graphics,
                    Component.literal(menu.familyValue(3) + " V × " + menu.familyValue(4) + " A"),
                    52, 28, 116, 0xff20eb3e);
            drawFittedText(graphics, Component.translatable("ic2.Transformer.gui.Input"), 8, 44, 42, 0xff404040);
            drawFittedText(
                    graphics,
                    Component.literal(menu.familyValue(1) + " V × " + menu.familyValue(2) + " A"),
                    52, 44, 116, 0xff20eb3e);
            graphics.item(
                    new ItemStack(ModTools.WRENCH.get()),
                    leftPos + 152,
                    topPos + 67 + Math.clamp(menu.familyValue(0), 0, 2) * 20);
        } else {
            var tier = VoltageTier.fromIcTier(menu.kind().electricalTier());
            drawFittedText(
                    graphics,
                    Component.translatable(
                            "ic2.EUStorage.gui.info.level",
                            Component.translatable(tier.getTranslationKey())),
                    menu.kind().chargepad() ? 79 : 82,
                    menu.kind().chargepad() ? 25 : 24,
                    menu.kind().chargepad() ? 89 : 86,
                    0xff404040);
            drawFittedText(
                    graphics,
                    Component.literal(" " + menu.energy()),
                    110,
                    34,
                    58,
                    0xff404040);
            drawFittedText(
                    graphics,
                    Component.literal("/" + menu.capacity()),
                    110,
                    44,
                    58,
                    0xff404040);
            if (menu.kind().storage()) {
                // Legacy GuiElectricBlock labels the worn-armor row above the player slots.
                drawFittedText(
                        graphics,
                        Component.translatable("ic2.EUStorage.gui.info.armor"),
                        8, 74, 70, 0xff404040);
                drawFittedText(
                        graphics,
                        Component.translatable("ic2.EUStorage.gui.info.output"),
                        82, 59, 86, 0xff404040);
                drawFittedText(
                        graphics,
                        Component.literal(tier.getVoltage() + " EU/t"),
                        82, 69, 86, 0xff404040);
            }
            if (modeButton != null)
                graphics.item(new ItemStack(Items.REDSTONE), leftPos + 154, topPos + 6);
        }
    }
}
