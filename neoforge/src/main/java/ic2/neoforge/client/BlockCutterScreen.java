package ic2.neoforge.client;

import ic2.neoforge.menu.MachineMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Standard machine layout plus a warning while the blade is missing or too weak. */
public final class BlockCutterScreen extends MachineScreen {
    public BlockCutterScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        if (menu.familyValue(0) == 1) {
            graphics.setComponentTooltipForNextFrame(
                    font,
                    List.of(Component.translatable("ic2.BlockCutter.gui.bladeTooWeak")),
                    mouseX,
                    mouseY,
                    ItemStack.EMPTY);
        }
    }
}
