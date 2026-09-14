package ic2.neoforge.client;

import ic2.neoforge.menu.MachineMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** Pattern storage screen: legacy last/next/export/import buttons plus the pattern info rows. */
public final class PatternStorageScreen extends MachineScreen {
    public PatternStorageScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    public void init() {
        super.init();
        smallButton("‹", "ic2.PatternStorage.gui.info.last", 0, 7, 19, 9, 18);
        smallButton("›", "ic2.PatternStorage.gui.info.next", 1, 36, 19, 9, 18);
        smallButton("↑", "ic2.PatternStorage.gui.info.export", 2, 10, 37, 16, 8);
        smallButton("↓", "ic2.PatternStorage.gui.info.import", 3, 26, 37, 16, 8);
    }

    private void smallButton(
            String symbol, String tooltip, int action, int x, int y, int width, int height) {
        var button =
                Button.builder(Component.literal(symbol), b -> send(action))
                        .bounds(leftPos + x, topPos + y, width, height)
                        .build();
        button.setTooltip(Tooltip.create(Component.translatable(tooltip)));
        addRenderableWidget(button);
    }

    private void send(int id) {
        if (minecraft.gameMode != null)
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int x = leftPos, y = topPos;
        int size = menu.familyValue(1);
        if (size > 0) {
            int index = menu.familyValue(0);
            graphics.text(
                    font,
                    Component.literal(Math.min(index + 1, size) + " / " + size),
                    x + (imageWidth - font.width(Math.min(index + 1, size) + " / " + size)) / 2,
                    y + 30,
                    0xff404040,
                    false);
        }
        graphics.text(
                font,
                Component.translatable("ic2.generic.text.Name"),
                x + 10,
                y + 48,
                0xffffff,
                false);
        graphics.text(
                font,
                Component.translatable("ic2.generic.text.UUMatte"),
                x + 10,
                y + 59,
                0xffffff,
                false);
        graphics.text(
                font,
                Component.translatable("ic2.generic.text.Energy"),
                x + 10,
                y + 70,
                0xffffff,
                false);
        if (size > 0 && menu.familyValue(2) >= 0) {
            ItemStack pattern =
                    new ItemStack(BuiltInRegistries.ITEM.byId(menu.familyValue(2)));
            graphics.text(
                    font, pattern.getHoverName(), x + 80, y + 48, 0xffffff, false);
            graphics.text(
                    font,
                    Component.literal(
                            MeterScreen.toSiString(menu.familyFloat(3), 4)
                                    + Component.translatable("ic2.generic.text.bucketUnit")
                                            .getString()),
                    x + 80,
                    y + 59,
                    0xffffff,
                    false);
            graphics.text(
                    font,
                    Component.literal(
                            MeterScreen.toSiString(menu.familyFloat(4), 4)
                                    + Component.translatable("ic2.generic.text.EU").getString()),
                    x + 80,
                    y + 70,
                    0xffffff,
                    false);
        }
    }
}
