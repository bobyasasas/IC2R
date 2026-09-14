package ic2.neoforge.client;

import ic2.neoforge.menu.MachineMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Shared layout for the two radioisotope machines: six single-pellet slots and readouts. */
public class RadioisotopeScreen extends MachineScreen {
    public RadioisotopeScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        drawFittedText(
                graphics,
                Component.translatable("ic2.radioisotope.installed", menu.familyValue(0)),
                34, 18, 134, 0xff404040);
        drawFittedText(
                graphics,
                Component.translatable("ic2.radioisotope.rate", menu.familyValue(1)),
                34, 56, 134, 0xff404040);
    }
}
