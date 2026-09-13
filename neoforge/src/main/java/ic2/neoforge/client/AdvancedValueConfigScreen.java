package ic2.neoforge.client;

import ic2.neoforge.component.AdvancedFilterSettings;
import ic2.neoforge.menu.AdvancedValueConfigMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Legacy GuiValueConfig: the comparison type cycle, its operator/bound controls and the back
 * button. Legacy's two number text boxes become stepper buttons — values are always valid, so
 * the legacy empty-box downgrade ladder has nothing to do. The extra row only matters for RANGE
 * comparisons; legacy disabled it otherwise, the port keeps it inert-but-clickable.
 */
public final class AdvancedValueConfigScreen
        extends ContainerScreenBase<AdvancedValueConfigMenu> {
    private Button typeButton;
    private Button normalOpButton;
    private Button extraOpButton;

    public AdvancedValueConfigScreen(
            AdvancedValueConfigMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 200);
    }

    @Override
    public void init() {
        super.init();
        typeButton =
                addRenderableWidget(
                        button(AdvancedValueConfigMenu.TYPE_CYCLE, 10, 30, 90, 15, ""));
        normalOpButton =
                addRenderableWidget(
                        button(AdvancedValueConfigMenu.NORMAL_OP_CYCLE, 10, 50, 25, 15, ""));
        addRenderableWidget(
                button(AdvancedValueConfigMenu.NORMAL_DOWN_10, 40, 50, 18, 15, "-10"));
        addRenderableWidget(button(AdvancedValueConfigMenu.NORMAL_DOWN, 60, 50, 18, 15, "-1"));
        addRenderableWidget(button(AdvancedValueConfigMenu.NORMAL_UP, 80, 50, 18, 15, "+1"));
        addRenderableWidget(button(AdvancedValueConfigMenu.NORMAL_UP_10, 100, 50, 18, 15, "+10"));
        extraOpButton =
                addRenderableWidget(
                        button(AdvancedValueConfigMenu.EXTRA_OP_TOGGLE, 128, 50, 25, 15, ""));
        addRenderableWidget(
                button(AdvancedValueConfigMenu.EXTRA_DOWN_10, 40, 70, 18, 15, "-10"));
        addRenderableWidget(button(AdvancedValueConfigMenu.EXTRA_DOWN, 60, 70, 18, 15, "-1"));
        addRenderableWidget(button(AdvancedValueConfigMenu.EXTRA_UP, 80, 70, 18, 15, "+1"));
        addRenderableWidget(button(AdvancedValueConfigMenu.EXTRA_UP_10, 100, 70, 18, 15, "+10"));
        addRenderableWidget(button(AdvancedValueConfigMenu.BACK, 10, 90, 50, 15, "back"));
    }

    private Button button(int id, int x, int y, int width, int height, String labelKey) {
        return Button.builder(
                        labelKey.isEmpty()
                                ? Component.empty()
                                : Component.translatable("ic2.upgrade.advancedGUI." + labelKey),
                        control ->
                                minecraft.gameMode.handleInventoryButtonClick(
                                        menu.containerId, id))
                .bounds(leftPos + x, topPos + y, width, height)
                .build();
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        var types = AdvancedFilterSettings.ComparisonType.VALUES;
        var ops = AdvancedFilterSettings.ComparisonSetting.VALUES;
        typeButton.setMessage(
                Component.translatable(
                        "ic2.upgrade.advancedGUI."
                                + types[Math.floorMod(menu.type(), types.length)]
                                        .name()
                                        .toLowerCase(java.util.Locale.ROOT)));
        normalOpButton.setMessage(
                Component.literal(
                        ops[Math.floorMod(menu.normalOp(), ops.length)].symbol));
        extraOpButton.setMessage(
                Component.literal(ops[Math.floorMod(menu.extraOp(), ops.length)].symbol));
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        graphics.text(
                font,
                Component.translatable(
                        menu.tagIsEnergy()
                                ? "ic2.upgrade.advancedGUI.energy"
                                : "ic2.upgrade.advancedGUI.meta"),
                leftPos + 10,
                topPos + 18,
                2157374,
                false);
        graphics.text(
                font,
                Component.literal("" + menu.normalBound()),
                leftPos + 122,
                topPos + 55,
                5752026,
                false);
        graphics.text(
                font,
                Component.literal("" + menu.extraBound()),
                leftPos + 122,
                topPos + 75,
                5752026,
                false);
    }
}
