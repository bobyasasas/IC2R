package ic2.neoforge.client;

import ic2.neoforge.menu.MachineMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
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
        addLegacyControl(7, 19, 9, 18, 0, () -> Component.translatable("ic2.PatternStorage.gui.info.last"));
        addLegacyControl(36, 19, 9, 18, 1, () -> Component.translatable("ic2.PatternStorage.gui.info.next"));
        addLegacyControl(10, 37, 16, 8, 2, () -> Component.translatable("ic2.PatternStorage.gui.info.export"));
        addLegacyControl(26, 37, 16, 8, 3, () -> Component.translatable("ic2.PatternStorage.gui.info.import"));
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
        drawFittedText(graphics, Component.translatable("ic2.generic.text.Name"), 10, 48, 68, 0xffffff);
        drawFittedText(graphics, Component.translatable("ic2.generic.text.UUMatte"), 10, 59, 68, 0xffffff);
        drawFittedText(graphics, Component.translatable("ic2.generic.text.Energy"), 10, 70, 68, 0xffffff);
        if (size > 0 && menu.familyValue(2) >= 0) {
            ItemStack pattern =
                    new ItemStack(BuiltInRegistries.ITEM.byId(menu.familyValue(2)));
            drawFittedText(graphics, pattern.getHoverName(), 80, 48, 68, 0xffffff);
            drawFittedText(
                    graphics,
                    Component.literal(
                            MeterScreen.toSiString(menu.familyFloat(3), 4)
                                    + Component.translatable("ic2.generic.text.bucketUnit")
                                            .getString()),
                    80,
                    59,
                    68,
                    0xffffff);
            drawFittedText(
                    graphics,
                    Component.literal(
                            MeterScreen.toSiString(menu.familyFloat(4), 4)
                                    + Component.translatable("ic2.generic.text.EU").getString()),
                    80,
                    70,
                    68,
                    0xffffff);
            graphics.item(pattern, x + 152, y + 29);
        }
    }
}
