package ic2.neoforge.client;

import ic2.neoforge.menu.MachineMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
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
        addRenderableWidget(
                Button.builder(
                                Component.translatable("ic2.PatternStorage.gui.info.last"),
                                b -> send(0))
                        .bounds(leftPos + 8, topPos + 62, 52, 16)
                        .build());
        addRenderableWidget(
                Button.builder(
                                Component.translatable("ic2.PatternStorage.gui.info.next"),
                                b -> send(1))
                        .bounds(leftPos + 64, topPos + 62, 52, 16)
                        .build());
        addRenderableWidget(
                Button.builder(
                                Component.translatable("ic2.PatternStorage.gui.info.export"),
                                b -> send(2))
                        .bounds(leftPos + 120, topPos + 62, 52, 16)
                        .build());
        addRenderableWidget(
                Button.builder(
                                Component.translatable("ic2.PatternStorage.gui.info.import"),
                                b -> send(3))
                        .bounds(leftPos + 8, topPos + 80, 52, 16)
                        .build());
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
                    x + 120,
                    y + 52,
                    0xff404040,
                    false);
        }
        graphics.text(
                font,
                Component.translatable("ic2.generic.text.Name"),
                x + 8,
                y + 96,
                0xffffff,
                false);
        graphics.text(
                font,
                Component.translatable("ic2.generic.text.UUMatte"),
                x + 8,
                y + 107,
                0xffffff,
                false);
        graphics.text(
                font,
                Component.translatable("ic2.generic.text.Energy"),
                x + 8,
                y + 118,
                0xffffff,
                false);
        if (size > 0 && menu.familyValue(2) >= 0) {
            ItemStack pattern =
                    new ItemStack(BuiltInRegistries.ITEM.byId(menu.familyValue(2)));
            graphics.text(
                    font, pattern.getHoverName(), x + 62, y + 96, 0xffffff, false);
            graphics.text(
                    font,
                    Component.literal(
                            MeterScreen.toSiString(menu.familyFloat(3), 4)
                                    + Component.translatable("ic2.generic.text.bucketUnit")
                                            .getString()),
                    x + 62,
                    y + 107,
                    0xffffff,
                    false);
            graphics.text(
                    font,
                    Component.literal(
                            MeterScreen.toSiString(menu.familyFloat(4), 4)
                                    + Component.translatable("ic2.generic.text.EU").getString()),
                    x + 62,
                    y + 118,
                    0xffffff,
                    false);
        }
    }
}
