package ic2.neoforge.client;

import ic2.neoforge.menu.MachineMenu;
import ic2.neoforge.registration.BalanceConfig;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class SteamRepressurizerScreen extends MachineScreen {
    public SteamRepressurizerScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected boolean showsProgress() {
        return false;
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        for (boolean output : new boolean[] {false, true})
            FluidTankDisplay.drawPlain(
                    minecraft,
                    graphics,
                    leftPos + (output ? 123 : 15),
                    topPos + 19,
                    38,
                    47,
                    menu.tankFluid(output),
                    menu.tankAmount(output),
                    10000);
        drawFittedText(
                graphics,
                Component.translatable(
                        "ic2.steam_repressurizer.rate", BalanceConfig.STEAM_PER_STEAM.get()),
                54, 30, 67, 0xff404040);
        drawFittedText(
                graphics,
                Component.translatable(
                        "ic2.steam_repressurizer.super", BalanceConfig.STEAM_PER_SUPER_STEAM.get()),
                54, 44, 67, 0xff404040);
        if (menu.familyValue(4) < 0)
            drawFittedText(
                    graphics,
                    Component.translatable("ic2.steam_repressurizer.no_steam"),
                    54, 58, 67, 0xffb02020);
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        for (boolean output : new boolean[] {false, true})
            FluidTankDisplay.tooltip(
                    minecraft,
                    graphics,
                    leftPos + (output ? 123 : 15),
                    topPos + 19,
                    38,
                    47,
                    mouseX,
                    mouseY,
                    menu.tankFluid(output),
                    menu.tankAmount(output),
                    10000);
    }
}
