package ic2.neoforge.client;

import ic2.neoforge.machine.TankBlockEntity;
import ic2.neoforge.menu.MachineMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.fluids.FluidStack;

public final class TankScreen extends MachineScreen {
    public TankScreen(MachineMenu menu, Inventory inventory, Component title) {
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
    public void extractBackground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        FluidTankDisplay.draw(
                minecraft,
                graphics,
                leftPos + 24,
                topPos + 23,
                menu.tankFluid(false),
                menu.tankAmount(false),
                TankBlockEntity.CAPACITY);
        var name =
                menu.tankAmount(false) == 0
                        ? Component.translatable("ic2.canner.empty")
                        : new FluidStack(menu.tankFluid(false), menu.tankAmount(false))
                                .getHoverName();
        graphics.text(font, name, leftPos + 44, topPos + 25, 0xff404040, false);
        graphics.text(
                font,
                Component.literal(menu.tankAmount(false) + " / 24000 mB"),
                leftPos + 44,
                topPos + 38,
                0xff404040,
                false);
        graphics.text(
                font,
                Component.translatable("ic2.tank.cursor"),
                leftPos + 8,
                topPos + 64,
                0xff404040,
                false);
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        FluidTankDisplay.tooltip(
                minecraft,
                graphics,
                leftPos + 24,
                topPos + 23,
                mouseX,
                mouseY,
                menu.tankFluid(false),
                menu.tankAmount(false),
                TankBlockEntity.CAPACITY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0
                && event.x() >= leftPos + 24
                && event.x() < leftPos + 36
                && event.y() >= topPos + 23
                && event.y() < topPos + 60) {
            if (minecraft.gameMode != null)
                minecraft.gameMode.handleInventoryButtonClick(
                        menu.containerId, event.hasShiftDown() ? 1 : 0);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }
}
