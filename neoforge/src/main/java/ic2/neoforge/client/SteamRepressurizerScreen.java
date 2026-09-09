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
            FluidTankDisplay.draw(
                    minecraft,
                    graphics,
                    leftPos + (output ? 153 : 8),
                    topPos + 18,
                    menu.tankFluid(output),
                    menu.tankAmount(output),
                    10000);
        graphics.text(
                font,
                Component.translatable(
                        "ic2.steam_repressurizer.rate", BalanceConfig.STEAM_PER_STEAM.get()),
                leftPos + 54,
                topPos + 30,
                0xff404040,
                false);
        graphics.text(
                font,
                Component.translatable(
                        "ic2.steam_repressurizer.super", BalanceConfig.STEAM_PER_SUPER_STEAM.get()),
                leftPos + 54,
                topPos + 44,
                0xff404040,
                false);
        if (menu.familyValue(4) < 0)
            graphics.text(
                    font,
                    Component.translatable("ic2.steam_repressurizer.no_steam"),
                    leftPos + 54,
                    topPos + 58,
                    0xffb02020,
                    false);
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        for (boolean output : new boolean[] {false, true})
            FluidTankDisplay.tooltip(
                    minecraft,
                    graphics,
                    leftPos + (output ? 153 : 8),
                    topPos + 18,
                    mouseX,
                    mouseY,
                    menu.tankFluid(output),
                    menu.tankAmount(output),
                    10000);
    }
}
