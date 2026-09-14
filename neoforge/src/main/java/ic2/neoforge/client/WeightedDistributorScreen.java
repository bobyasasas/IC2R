package ic2.neoforge.client;

import ic2.neoforge.menu.MachineMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Legacy five-level priority matrix. The server receives both the selected row and direction.
 */
public final class WeightedDistributorScreen extends MachineScreen {
    private final List<SideButton> buttons = new ArrayList<>();

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
        buttons.clear();
        for (int row = 0; row < 5; row++) {
            for (int column = 0; column < 6; column++) {
                int selectedRow = row;
                int data = (column + 1) % 6;
                Direction side = Direction.from3DDataValue(data);
                var button =
                        Button.builder(
                                        Component.literal(
                                                side.getSerializedName()
                                                        .substring(0, 1)
                                                        .toUpperCase(Locale.ROOT)),
                                        control -> send(6 + selectedRow * 6 + data))
                                .bounds(leftPos + 63 + column * 18, topPos + 17 + row * 18, 16, 16)
                                .build();
                button.setTooltip(
                        net.minecraft.client.gui.components.Tooltip.create(
                                Component.translatable("direction.minecraft." + side.getName())));
                addRenderableWidget(button);
                buttons.add(new SideButton(button, selectedRow, data));
            }
        }
    }

    private void send(int id) {
        if (minecraft.gameMode != null)
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        Component[] rows = {
            Component.translatable("ic2.WeightedDistributor.gui.highest"),
            Component.literal("↑"),
            Component.translatable("ic2.WeightedDistributor.gui.priority"),
            Component.literal("↓"),
            Component.translatable("ic2.WeightedDistributor.gui.lowest")
        };
        for (int row = 0; row < rows.length; row++)
            drawFittedText(graphics, rows[row], 8, 21 + row * 18, 52, 0xff404040);
        for (SideButton entry : buttons) {
            boolean selected = menu.familyValue(entry.row()) == entry.data();
            String name = Direction.from3DDataValue(entry.data()).getSerializedName().substring(0, 1);
            entry.button().setMessage(
                    Component.literal(selected ? name.toUpperCase(Locale.ROOT) : name.toLowerCase(Locale.ROOT)));
        }
    }

    private record SideButton(Button button, int row, int data) {}
}
