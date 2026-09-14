package ic2.neoforge.client;

import ic2.neoforge.menu.MachineMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Buttons for the sweep cursor, the blacklist mode and the silk touch switch. */
public final class AdvMinerScreen extends MachineScreen {
    public AdvMinerScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    public void init() {
        super.init();
        addLegacyControl(133, 101, 36, 15, 0, () -> Component.translatable("ic2.AdvMiner.gui.switch.reset"));
        addLegacyControl(123, 27, 18, 15, 1, this::modeLabel);
        addLegacyControl(
                129,
                45,
                18,
                15,
                2,
                () -> Component.translatable("ic2.AdvMiner.gui.switch.silktouch", menu.familyValue(1) == 1));
    }

    private Component modeLabel() {
        return Component.translatable(
                menu.familyValue(0) == 1
                        ? "ic2.AdvMiner.gui.mode.blacklist"
                        : "ic2.AdvMiner.gui.mode.whitelist");
    }

    @Override
    protected void drawLegacyMachineBackground(GuiGraphicsExtractor graphics) {
        LegacyMachineGui.blit(graphics, LegacyMachineGui.COMMON, leftPos + 133, topPos + 101, 192, 32, 36, 15);
        LegacyMachineGui.blit(graphics, LegacyMachineGui.COMMON, leftPos + 123, topPos + 27, 228, 32, 18, 15);
        LegacyMachineGui.blit(graphics, LegacyMachineGui.COMMON, leftPos + 129, topPos + 45, 192, 47, 18, 15);
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        drawFittedText(graphics, modeLabel(), 40, 30, 80, 0xff20eb3e);
    }
}
