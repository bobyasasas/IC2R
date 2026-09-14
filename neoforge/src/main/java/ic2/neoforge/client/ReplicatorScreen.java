package ic2.neoforge.client;

import ic2.neoforge.fluid.FluidDefinition;
import ic2.neoforge.machine.ReplicatorBlockEntity;
import ic2.neoforge.menu.MachineMenu;
import ic2.neoforge.registration.ModFluids;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Replicator screen: pattern browsing and mode buttons (legacy last/next/single/repeat/stop). */
public final class ReplicatorScreen extends MachineScreen {
    public ReplicatorScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    public void init() {
        super.init();
        smallButton("‹", "ic2.Replicator.gui.info.last", 0, 80, 16, 9, 18);
        smallButton("›", "ic2.Replicator.gui.info.next", 1, 109, 16, 9, 18);
        smallButton("■", "ic2.Replicator.gui.info.Stop", 3, 75, 82, 16, 16);
        smallButton("1", "ic2.Replicator.gui.info.single", 4, 92, 82, 16, 16);
        smallButton("∞", "ic2.Replicator.gui.info.repeat", 5, 109, 82, 16, 16);
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
        int tankAmount =
                Math.round(menu.familyFloat(0) * ReplicatorBlockEntity.TANK_CAPACITY);
        FluidTankDisplay.drawNormal(
                minecraft,
                graphics,
                leftPos + 27,
                topPos + 30,
                ModFluids.FAMILIES.get(FluidDefinition.UU_MATTER).source().get(),
                tankAmount,
                ReplicatorBlockEntity.TANK_CAPACITY);
        int count = menu.familyValue(3);
        if (count > 0) {
            graphics.text(
                    font,
                    Component.literal(menu.familyValue(2) + " / " + count),
                    leftPos + 8,
                    topPos + 52,
                    0xff404040,
                    false);
        }
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        int tankAmount =
                Math.round(menu.familyFloat(0) * ReplicatorBlockEntity.TANK_CAPACITY);
        FluidTankDisplay.tooltip(
                minecraft,
                graphics,
                leftPos + 27,
                topPos + 30,
                20,
                55,
                mouseX,
                mouseY,
                ModFluids.FAMILIES.get(FluidDefinition.UU_MATTER).source().get(),
                tankAmount,
                ReplicatorBlockEntity.TANK_CAPACITY);
    }
}
