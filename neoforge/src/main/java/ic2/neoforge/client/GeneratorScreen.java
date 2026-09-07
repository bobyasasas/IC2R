package ic2.neoforge.client;

import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.menu.MachineMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class GeneratorScreen extends MachineScreen {
    public GeneratorScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        if (menu.kind().fluidGenerator())
            FluidTankDisplay.draw(
                    minecraft,
                    graphics,
                    leftPos + 151,
                    topPos + 18,
                    BuiltInRegistries.FLUID.byId(menu.familyValue(1)),
                    menu.familyValue(0),
                    menu.familyValue(2));
        if (menu.kind() == MachineKind.SOLAR_GENERATOR)
            graphics.text(
                    font,
                    Component.translatable("ic2.solar.sunlight", menu.progress() / 10),
                    leftPos + 76,
                    topPos + 58,
                    0xff404040,
                    false);
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        if (menu.kind().fluidGenerator())
            FluidTankDisplay.tooltip(
                    minecraft,
                    graphics,
                    leftPos + 151,
                    topPos + 18,
                    mouseX,
                    mouseY,
                    BuiltInRegistries.FLUID.byId(menu.familyValue(1)),
                    menu.familyValue(0),
                    menu.familyValue(2));
    }
}
