package ic2.neoforge.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;

/** Shared native-fluid tint and tooltip rendering for machine tanks. */
final class FluidTankDisplay {
    static void draw(
            Minecraft client,
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            Fluid fluid,
            int amount,
            int capacity) {
        int height = (int) Math.clamp(35L * amount / Math.max(1, capacity), 0, 35);
        graphics.fill(x, y, x + 12, y + 37, 0xff373737);
        drawFluid(client, graphics, x + 1, y + 1, 10, 35, height, fluid, amount);
    }

    /** Original IC2 tank with a 20x55 frame and a 12x47 fluid window. */
    static void drawNormal(
            Minecraft client,
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            Fluid fluid,
            int amount,
            int capacity) {
        int frameU = amount > 0 && fluid != null ? 6 : 70;
        LegacyMachineGui.blit(graphics, LegacyMachineGui.COMMON, x, y, frameU, 100, 20, 55);
        int height = (int) Math.clamp(47L * amount / Math.max(1, capacity), 0, 47);
        drawFluid(client, graphics, x + 4, y + 4, 12, 47, height, fluid, amount);
        // Tick marks were a separate mirrored overlay in the original TankGauge.
        LegacyMachineGui.blit(graphics, LegacyMachineGui.COMMON, x, y, 38, 100, 20, 55);
    }

    /** Fluid-only window used by legacy machine-specific backgrounds. */
    static void drawPlain(
            Minecraft client,
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int width,
            int height,
            Fluid fluid,
            int amount,
            int capacity) {
        int fill = (int) Math.clamp((long) height * amount / Math.max(1, capacity), 0, height);
        drawFluid(client, graphics, x, y, width, height, fill, fluid, amount);
    }

    private static void drawFluid(
            Minecraft client,
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int width,
            int fullHeight,
            int fillHeight,
            Fluid fluid,
            int amount) {
        if (fillHeight == 0 || fluid == null) return;
        var model = client.getModelManager().getFluidStateModelSet().get(fluid.defaultFluidState());
        int color =
                model.fluidTintSource() == null
                        ? 0xffffffff
                        : model.fluidTintSource().colorAsStack(new FluidStack(fluid, amount))
                                | 0xff000000;
        graphics.blitSprite(
                RenderPipelines.GUI_TEXTURED,
                model.stillMaterial().sprite(),
                x,
                y + fullHeight - fillHeight,
                width,
                fillHeight,
                color);
    }

    static void tooltip(
            Minecraft client,
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int mouseX,
            int mouseY,
            Fluid fluid,
            int amount,
            int capacity) {
        tooltip(client, graphics, x, y, 12, 37, mouseX, mouseY, fluid, amount, capacity);
    }

    static void tooltip(
            Minecraft client,
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int width,
            int height,
            int mouseX,
            int mouseY,
            Fluid fluid,
            int amount,
            int capacity) {
        if (mouseX < x || mouseX >= x + width || mouseY < y || mouseY >= y + height) return;
        var content =
                amount <= 0 || fluid == null
                        ? Component.translatable("ic2.canner.empty")
                        : new FluidStack(fluid, amount).getHoverName();
        graphics.setComponentTooltipForNextFrame(
                client.font,
                List.of(content, Component.literal(amount + " / " + capacity + " mB")),
                mouseX,
                mouseY,
                ItemStack.EMPTY);
    }

    private FluidTankDisplay() {}
}
