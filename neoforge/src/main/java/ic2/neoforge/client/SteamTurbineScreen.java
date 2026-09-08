package ic2.neoforge.client;

import ic2.neoforge.machine.SteamTurbineBlockEntity;
import ic2.neoforge.menu.MachineMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.Locale;

public final class SteamTurbineScreen extends MachineScreen {
    public SteamTurbineScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected boolean showsEnergyBar() {
        return false;
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
                    output ? 1000 : 21000);
        graphics.text(
                font,
                Component.literal(menu.familyValue(0) + " KU/t"),
                leftPos + 45,
                topPos + 42,
                0xff404040,
                false);
        graphics.text(
                font,
                Component.translatable(
                        "ic2.steam_turbine.pending",
                        String.format(Locale.ROOT, "%.3g", menu.familyFloat(6))),
                leftPos + 8,
                topPos + 60,
                0xff404040,
                false);
        int flags = menu.familyValue(5);
        String status =
                (flags & SteamTurbineBlockEntity.NO_TURBINE) != 0
                        ? "no_turbine"
                        : (flags & SteamTurbineBlockEntity.DISABLED) != 0
                                ? "disabled"
                                : (flags & SteamTurbineBlockEntity.WATER_BLOCKED) != 0
                                        ? "water_blocked"
                                        : (flags & SteamTurbineBlockEntity.VENTING) != 0
                                                ? "venting"
                                                : (flags & SteamTurbineBlockEntity.THROTTLED) != 0
                                                        ? "throttled"
                                                        : menu.familyValue(0) > 0
                                                                ? "running"
                                                                : "idle";
        graphics.text(
                font,
                Component.translatable("ic2.steam_turbine." + status),
                leftPos + 8,
                topPos + 74,
                0xff404040,
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
                    output ? 1000 : 21000);
    }
}
