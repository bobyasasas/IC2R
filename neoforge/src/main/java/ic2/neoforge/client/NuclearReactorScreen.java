package ic2.neoforge.client;

import ic2.neoforge.fluid.FluidDefinition;
import ic2.neoforge.machine.NuclearReactorBlockEntity;
import ic2.neoforge.menu.NuclearReactorMenu;
import ic2.neoforge.registration.ModFluids;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluid;

import java.util.List;
import java.util.Locale;

/** Legacy GuiNuclearReactor drawn programmatically: heat gauge, inactive-column shading, and the
 * fluid-mode tank windows with their temperature bars. */
public final class NuclearReactorScreen extends ContainerScreenBase<NuclearReactorMenu> {
    private static final int HOT_COLOR = 0xffff5c26;

    public NuclearReactorScreen(NuclearReactorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 212, 243);
    }

    @Override
    protected Identifier backgroundTexture() {
        return Identifier.fromNamespaceAndPath(
                "ic2",
                menu.fluidCooled()
                        ? "textures/gui/guinuclearreactorfluid.png"
                        : "textures/gui/guinuclearreactor.png");
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int x = leftPos, y = topPos;
        // The legacy background darkens component cells beyond the active column count.
        for (int column = menu.reactorColumns();
                column < NuclearReactorBlockEntity.GRID_COLUMNS;
                column++)
            for (int row = 0; row < NuclearReactorBlockEntity.GRID_ROWS; row++) {
                int cellX = x + 26 + column * 18, cellY = y + 25 + row * 18;
                graphics.fill(cellX, cellY, cellX + 16, cellY + 16, 0xff373737);
            }
        // Legacy GaugeStyle.HeatNuclearReactor is a horizontal 100x13 texture strip.
        int heatWidth = heatGaugeWidth();
        if (heatWidth > 0)
            LegacyMachineGui.blit(
                    graphics,
                    Identifier.fromNamespaceAndPath("ic2", "textures/gui/guinuclearreactor.png"),
                    x + 7,
                    y + 136,
                    0,
                    243,
                    heatWidth,
                    13);
        if (!menu.fluidCooled()) return;
        // Fluid mode: seven thin temperature bars fill leftward from the grid's right edge.
        int barWidth = (int) Math.clamp(160L * menu.heat() / Math.max(1, menu.maxHeat()), 0, 160);
        for (int row = 0; row < 7; row++) {
            int barY = y + 23 + row * 18;
            graphics.fill(x + 186 - barWidth, barY, x + 186, barY + 2, HOT_COLOR);
        }
        FluidTankDisplay.draw(
                minecraft,
                graphics,
                x + 10,
                y + 54,
                tankFluid(FluidDefinition.COOLANT),
                menu.coolantAmount(),
                NuclearReactorBlockEntity.COOLANT_TANK_CAPACITY);
        FluidTankDisplay.draw(
                minecraft,
                graphics,
                x + 190,
                y + 54,
                tankFluid(FluidDefinition.HOT_COOLANT),
                menu.hotCoolantAmount(),
                NuclearReactorBlockEntity.COOLANT_TANK_CAPACITY);
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        int x = leftPos, y = topPos;
        if (mouseX >= x + 7 && mouseX < x + 107 && mouseY >= y + 136 && mouseY < y + 149) {
            String percent =
                    String.format(
                            Locale.ROOT,
                            "%.2f",
                            100.0 * menu.heat() / Math.max(1, menu.maxHeat()));
            graphics.setComponentTooltipForNextFrame(
                    font,
                    List.of(Component.translatable("ic2.NuclearReactor.gui.info.temp", percent)),
                    mouseX,
                    mouseY,
                    net.minecraft.world.item.ItemStack.EMPTY);
        }
        if (mouseX >= x + 5 && mouseX < x + 23 && mouseY >= y + 160 && mouseY < y + 178) {
            graphics.setComponentTooltipForNextFrame(
                    font,
                    List.of(
                            Component.translatable(
                                    menu.fluidCooled()
                                            ? "ic2.NuclearReactor.gui.mode.fluid"
                                            : "ic2.NuclearReactor.gui.mode.electric")),
                    mouseX,
                    mouseY,
                    net.minecraft.world.item.ItemStack.EMPTY);
        }
        if (!menu.fluidCooled()) return;
        FluidTankDisplay.tooltip(
                minecraft,
                graphics,
                x + 10,
                y + 54,
                mouseX,
                mouseY,
                tankFluid(FluidDefinition.COOLANT),
                menu.coolantAmount(),
                NuclearReactorBlockEntity.COOLANT_TANK_CAPACITY);
        FluidTankDisplay.tooltip(
                minecraft,
                graphics,
                x + 190,
                y + 54,
                mouseX,
                mouseY,
                tankFluid(FluidDefinition.HOT_COOLANT),
                menu.hotCoolantAmount(),
                NuclearReactorBlockEntity.COOLANT_TANK_CAPACITY);
    }

    private int heatGaugeWidth() {
        return (int) Math.clamp(100L * menu.heat() / Math.max(1, menu.maxHeat()), 0, 100);
    }

    private static Fluid tankFluid(FluidDefinition definition) {
        return ModFluids.FAMILIES.get(definition).source().get();
    }
}
