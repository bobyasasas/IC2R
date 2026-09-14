package ic2.neoforge.client;

import ic2.neoforge.menu.MachineMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class OreWashingScreen extends MachineScreen {
    public OreWashingScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void drawLegacyMachineBackground(GuiGraphicsExtractor graphics) {
        LegacyMachineGui.blit(
                graphics,
                LegacyMachineGui.ORE_WASHING,
                leftPos + 37,
                topPos + 16,
                37,
                16,
                87,
                63);
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        FluidTankDisplay.drawNormal(
                minecraft,
                graphics,
                leftPos + 60,
                topPos + 20,
                BuiltInRegistries.FLUID.byId(menu.familyValue(1)),
                menu.familyValue(0),
                menu.familyValue(2));
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        FluidTankDisplay.tooltip(
                minecraft,
                graphics,
                leftPos + 60,
                topPos + 20,
                20,
                55,
                mouseX,
                mouseY,
                BuiltInRegistries.FLUID.byId(menu.familyValue(1)),
                menu.familyValue(0),
                menu.familyValue(2));
    }
}
