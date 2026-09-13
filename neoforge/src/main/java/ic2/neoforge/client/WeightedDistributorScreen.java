package ic2.neoforge.client;

import ic2.neoforge.menu.MachineMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

/**
 * Six side toggles replace the legacy 5x6 button matrix: clicking a side removes it from the
 * priority, adding appends it as the lowest entry; the label shows the position.
 */
public final class WeightedDistributorScreen extends MachineScreen {
    private final List<Button> buttons = new ArrayList<>();

    public WeightedDistributorScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected boolean showsEnergyBar() {
        return false;
    }

    @Override
    protected boolean showsProgress() {
        return false;
    }

    @Override
    public void init() {
        super.init();
        for (int data = 0; data < Direction.values().length; data++) {
            int id = data;
            buttons.add(
                    addRenderableWidget(
                            Button.builder(Component.empty(), button -> send(id))
                                    .bounds(leftPos + 7 + data * 27, topPos + 18, 26, 16)
                                    .build()));
        }
    }

    private Component label(int data) {
        var side = Direction.from3DDataValue(data);
        for (int index = 0; index < 5; index++)
            if (menu.familyValue(index) == data)
                return Component.translatable(
                        "ic2.distributor.priority", side.getName(), index + 1);
        return Component.translatable("ic2.distributor.side", side.getName());
    }

    private void send(int id) {
        if (minecraft.gameMode != null)
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        for (int data = 0; data < buttons.size(); data++) buttons.get(data).setMessage(label(data));
    }
}
