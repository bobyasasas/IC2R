package ic2.neoforge.client;

import ic2.core.machine.CannerMode;
import ic2.neoforge.menu.MachineMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;

public final class CannerScreen extends MachineScreen {
    private Button modeButton;

    public CannerScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    public void init() {
        super.init();
        modeButton =
                addRenderableWidget(
                        Button.builder(
                                        modeLabel(),
                                        button -> sendButton((menu.cannerMode() + 1) % 4))
                                .bounds(leftPos + 77, topPos + 57, 64, 16)
                                .build());
        addRenderableWidget(
                Button.builder(Component.literal("↔"), button -> sendButton(5))
                        .bounds(leftPos + 144, topPos + 57, 24, 16)
                        .build());
    }

    private Component modeLabel() {
        return Component.translatable(
                "ic2.canner.mode."
                        + CannerMode.byId(Math.clamp(menu.cannerMode(), 0, 3)).serializedName());
    }

    private void sendButton(int id) {
        if (minecraft.gameMode != null)
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        drawTank(graphics, false, 8);
        drawTank(graphics, true, 151);
    }

    private void drawTank(GuiGraphicsExtractor graphics, boolean output, int offset) {
        int amount = menu.tankAmount(output);
        int height = Math.clamp(amount * 35 / 8000, 0, 35);
        int color = 0xff3f76e4;
        var fluid = menu.tankFluid(output);
        if (amount > 0 && fluid != null) {
            var tint =
                    minecraft
                            .getModelManager()
                            .getFluidStateModelSet()
                            .get(fluid.defaultFluidState())
                            .fluidTintSource();
            if (tint != null) color = tint.colorAsStack(new FluidStack(fluid, amount)) | 0xff000000;
        }
        graphics.fill(
                leftPos + offset, topPos + 18, leftPos + offset + 12, topPos + 55, 0xff373737);
        graphics.fill(
                leftPos + offset + 1,
                topPos + 54 - height,
                leftPos + offset + 11,
                topPos + 54,
                color);
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        modeButton.setMessage(modeLabel());
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        for (boolean output : new boolean[] {false, true}) {
            int offset = output ? 151 : 8;
            if (mouseX >= leftPos + offset
                    && mouseX < leftPos + offset + 12
                    && mouseY >= topPos + 18
                    && mouseY < topPos + 55) {
                var fluid = menu.tankFluid(output);
                int amount = menu.tankAmount(output);
                Component content =
                        amount <= 0 || fluid == null
                                ? Component.translatable("ic2.canner.empty")
                                : new FluidStack(fluid, amount).getHoverName();
                graphics.setComponentTooltipForNextFrame(
                        font,
                        List.of(content, Component.literal(amount + " / 8000 mB")),
                        mouseX,
                        mouseY,
                        ItemStack.EMPTY);
            }
        }
    }
}
