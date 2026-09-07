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
        if (height == 0 || fluid == null) return;
        var model = client.getModelManager().getFluidStateModelSet().get(fluid.defaultFluidState());
        int color =
                model.fluidTintSource() == null
                        ? 0xffffffff
                        : model.fluidTintSource().colorAsStack(new FluidStack(fluid, amount))
                                | 0xff000000;
        graphics.blitSprite(
                RenderPipelines.GUI_TEXTURED,
                model.stillMaterial().sprite(),
                x + 1,
                y + 36 - height,
                10,
                height,
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
        if (mouseX < x || mouseX >= x + 12 || mouseY < y || mouseY >= y + 37) return;
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
