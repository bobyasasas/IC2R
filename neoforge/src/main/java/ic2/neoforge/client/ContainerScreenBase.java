package ic2.neoforge.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Shared IC2 container frame using the original GUI atlas and authoritative menu slots. */
public class ContainerScreenBase<T extends AbstractContainerMenu>
        extends AbstractContainerScreen<T> {
    protected ContainerScreenBase(
            T menu, Inventory inventory, Component title, int width, int height) {
        super(menu, inventory, title, width, height);
    }

    @Override
    protected void init() {
        super.init();
        titleLabelX = (imageWidth - font.width(title)) / 2;
    }

    /** A full 256x256 legacy background, or {@code null} for the tiled common background. */
    protected Identifier backgroundTexture() {
        return null;
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        Identifier background = backgroundTexture();
        if (background == null) drawCommonBackground(graphics);
        else
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    background,
                    leftPos,
                    topPos,
                    0.0F,
                    0.0F,
                    imageWidth,
                    imageHeight,
                    256,
                    256);

        // Full legacy textures already contain their slot art. Dynamic legacy GUIs used the
        // common atlas, so reproduce their normal slot frames from the live menu geometry.
        if (background != null) return;
        for (var slot : menu.slots) {
            LegacyMachineGui.blit(
                    graphics,
                    LegacyMachineGui.COMMON,
                    leftPos + slot.x - 1,
                    topPos + slot.y - 1,
                    103,
                    7,
                    18,
                    18);
        }
    }

    private void drawCommonBackground(GuiGraphicsExtractor graphics) {
        drawCommonPart(graphics, -16, -16, 32, 32, 0, 0);
        drawCommonPart(graphics, imageWidth - 16, -16, 32, 32, 64, 0);
        drawCommonPart(graphics, -16, imageHeight - 16, 32, 32, 0, 64);
        drawCommonPart(
                graphics, imageWidth - 16, imageHeight - 16, 32, 32, 64, 64);

        for (int side = 0; side < 2; side++) {
            int relativeY = imageHeight * side - 16;
            int v = 64 * side;
            for (int relativeX = 16; relativeX < imageWidth - 16; relativeX += 32)
                drawCommonPart(
                        graphics,
                        relativeX,
                        relativeY,
                        Math.min(32, imageWidth - 16 - relativeX),
                        32,
                        32,
                        v);
        }
        for (int side = 0; side < 2; side++) {
            int relativeX = imageWidth * side - 16;
            int u = 64 * side;
            for (int relativeY = 16; relativeY < imageHeight - 16; relativeY += 32)
                drawCommonPart(
                        graphics,
                        relativeX,
                        relativeY,
                        32,
                        Math.min(32, imageHeight - 16 - relativeY),
                        u,
                        32);
        }
        for (int relativeY = 16; relativeY < imageHeight - 16; relativeY += 32) {
            int height = Math.min(32, imageHeight - 16 - relativeY);
            for (int relativeX = 16; relativeX < imageWidth - 16; relativeX += 32)
                drawCommonPart(
                        graphics,
                        relativeX,
                        relativeY,
                        Math.min(32, imageWidth - 16 - relativeX),
                        height,
                        32,
                        32);
        }
    }

    private void drawCommonPart(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int width,
            int height,
            int u,
            int v) {
        LegacyMachineGui.blit(
                graphics, LegacyMachineGui.COMMON, leftPos + x, topPos + y, u, v, width, height);
    }
}
