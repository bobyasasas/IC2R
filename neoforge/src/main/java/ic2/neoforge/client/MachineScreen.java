package ic2.neoforge.client;

import ic2.neoforge.menu.MachineMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.IntUnaryOperator;
import java.util.function.Supplier;

/** Small shared screen; slot geometry is taken from the menu, so visuals cannot drift. */
public class MachineScreen extends ContainerScreenBase<MachineMenu> {
    private final List<LegacyControl> legacyControls = new ArrayList<>();

    public MachineScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, menu.kind().menuWidth(), menu.kind().menuHeight());
    }

    protected boolean showsEnergyBar() {
        return true;
    }

    protected boolean showsProgress() {
        return !menu.kind().energyDevice();
    }

    @Override
    protected void init() {
        legacyControls.clear();
        super.init();
        // Ic2Gui did not draw AbstractContainerScreen's automatic inventory label. A few
        // personal-trading screens add it explicitly; everywhere else it overlaps machine art.
        inventoryLabelY = -1000;
    }

    /**
     * Adds an invisible click target over a control already painted into a legacy GUI texture.
     * This avoids drawing a second vanilla button and glyph over the original artwork.
     */
    protected final void addLegacyControl(
            int x, int y, int width, int height, int action, Supplier<Component> tooltip) {
        addLegacyControl(
                x,
                y,
                width,
                height,
                button -> button == 0 ? action : -1,
                tooltip,
                () -> true);
    }

    protected final void addLegacyControl(
            int x,
            int y,
            int width,
            int height,
            IntUnaryOperator action,
            Supplier<Component> tooltip,
            BooleanSupplier enabled) {
        legacyControls.add(new LegacyControl(x, y, width, height, action, tooltip, enabled));
    }

    @Override
    protected Identifier backgroundTexture() {
        return LegacyMachineGui.background(menu);
    }

    /** Static machine art from a legacy atlas, drawn before live gauges and tank contents. */
    protected void drawLegacyMachineBackground(GuiGraphicsExtractor graphics) {}

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        drawLegacyMachineBackground(graphics);
        if (showsEnergyBar())
            LegacyMachineGui.drawGauge(
                    graphics,
                    leftPos,
                    topPos,
                    LegacyMachineGui.energy(menu.kind()),
                    menu.energy(),
                    menu.capacity());
        LegacyMachineGui.drawGauge(
                graphics,
                leftPos,
                topPos,
                LegacyMachineGui.fuel(menu.kind()),
                menu.fuelRemaining(),
                menu.fuelMaximum());
        if (showsProgress())
            LegacyMachineGui.drawGauge(
                    graphics,
                    leftPos,
                    topPos,
                    LegacyMachineGui.progress(menu.kind()),
                    menu.progress(),
                    menu.progressMaximum());
        for (LegacyControl control : legacyControls) {
            if (control.enabled().getAsBoolean()
                    && control.contains(mouseX - leftPos, mouseY - topPos)) {
                graphics.fill(
                        leftPos + control.x(),
                        topPos + control.y(),
                        leftPos + control.x() + control.width(),
                        topPos + control.y() + control.height(),
                        0x80ffffff);
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int x = (int) event.x() - leftPos;
        int y = (int) event.y() - topPos;
        for (LegacyControl control : legacyControls) {
            if (!control.enabled().getAsBoolean() || !control.contains(x, y)) continue;
            int action = control.action().applyAsInt(event.button());
            if (action < 0) continue;
            if (minecraft.gameMode != null)
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, action);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        var energyGauge = LegacyMachineGui.energy(menu.kind());
        if (menu.capacity() > 0
                && LegacyMachineGui.contains(energyGauge, leftPos, topPos, mouseX, mouseY)) {
            graphics.setComponentTooltipForNextFrame(
                    font,
                    List.of(
                            Component.translatable(
                                    "ic2.tooltip.energy", menu.energy(), menu.capacity())),
                    mouseX,
                    mouseY,
                    ItemStack.EMPTY);
        }
        int x = mouseX - leftPos;
        int y = mouseY - topPos;
        for (LegacyControl control : legacyControls) {
            if (!control.enabled().getAsBoolean() || !control.contains(x, y)) continue;
            Component tooltip = control.tooltip() == null ? null : control.tooltip().get();
            if (tooltip != null)
                graphics.setTooltipForNextFrame(font, tooltip, mouseX, mouseY);
            break;
        }
    }

    private record LegacyControl(
            int x,
            int y,
            int width,
            int height,
            IntUnaryOperator action,
            Supplier<Component> tooltip,
            BooleanSupplier enabled) {
        boolean contains(int mouseX, int mouseY) {
            return mouseX >= x
                    && mouseX < x + width
                    && mouseY >= y
                    && mouseY < y + height;
        }
    }
}
