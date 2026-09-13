package ic2.neoforge.client;

import ic2.neoforge.menu.AdvancedUpgradeMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Legacy advanced_upgrade.xml: the meta/energy toggles beside the 3x3 hologram grid, the NBT
 * mode indicator, and — outside production builds only, like legacy's Util.inDev() gate — the
 * M/E/O buttons that open the meta/energy comparison and ore-dictionary editors.
 */
public final class AdvancedUpgradeScreen extends ContainerScreenBase<AdvancedUpgradeMenu> {
    private static final int STATE_COLOR_ON = 2157374;
    private static final int STATE_COLOR_OFF = 5752026;

    private Button metaButton;
    private Button energyButton;

    public AdvancedUpgradeScreen(AdvancedUpgradeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 182);
    }

    @Override
    public void init() {
        super.init();
        metaButton = addRenderableWidget(toggleButton(AdvancedUpgradeMenu.META_TOGGLE, 10, 18, "meta"));
        energyButton =
                addRenderableWidget(toggleButton(AdvancedUpgradeMenu.ENERGY_TOGGLE, 10, 40, "energy"));
        if (!net.neoforged.fml.loading.FMLLoader.getCurrent().isProduction()) {
            addRenderableWidget(devButton(AdvancedUpgradeMenu.META_CONFIG, 83, 23, "M"));
            addRenderableWidget(devButton(AdvancedUpgradeMenu.ENERGY_CONFIG, 83, 45, "E"));
            addRenderableWidget(devButton(AdvancedUpgradeMenu.ORE_CONFIG, 83, 67, "O"));
        }
    }

    private Button toggleButton(int id, int x, int y, String key) {
        return Button.builder(
                        Component.empty(),
                        control ->
                                minecraft.gameMode.handleInventoryButtonClick(
                                        menu.containerId, id))
                .bounds(leftPos + x, topPos + y, 50, 20)
                .build();
    }

    private Button devButton(int id, int x, int y, String label) {
        return Button.builder(
                        Component.literal(label),
                        control ->
                                minecraft.gameMode.handleInventoryButtonClick(
                                        menu.containerId, id))
                .bounds(leftPos + x, topPos + y, 10, 10)
                .build();
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        metaButton.setMessage(
                stateLabel("ic2.upgrade.advancedGUI.meta", menu.meta()));
        energyButton.setMessage(
                stateLabel("ic2.upgrade.advancedGUI.energy", menu.energy()));
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private static Component stateLabel(String key, boolean on) {
        return Component.translatable(key)
                .append(": ")
                .append(
                        Component.translatable(
                                on
                                        ? "ic2.upgrade.advancedGUI.on"
                                        : "ic2.upgrade.advancedGUI.off"));
    }

    @Override
    public void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        graphics.text(
                font,
                Component.translatable("ic2.upgrade.advancedGUI.nbt"),
                leftPos + 10,
                topPos + 67,
                menu.nbtIndicator() ? STATE_COLOR_ON : STATE_COLOR_OFF,
                false);
    }
}
