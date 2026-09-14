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
        FluidTankDisplay.drawPlain(
                minecraft,
                graphics,
                leftPos + 75,
                topPos + 21,
                26,
                26,
                menu.tankFluid(true),
                menu.tankAmount(true),
                1000);
        drawFittedText(graphics, Component.literal(menu.familyValue(0) + " KU/t"), 45, 42, 28, 0xff404040);
        drawFittedText(
                graphics,
                Component.translatable(
                        "ic2.steam_turbine.pending",
                        String.format(Locale.ROOT, "%.3g", menu.familyFloat(6))),
                8, 60, 160, 0xff404040);
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
        drawFittedText(graphics, Component.translatable("ic2.steam_turbine." + status), 8, 74, 160, 0xff404040);
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        FluidTankDisplay.tooltip(
                minecraft,
                graphics,
                leftPos + 75,
                topPos + 21,
                26,
                26,
                mouseX,
                mouseY,
                menu.tankFluid(true),
                menu.tankAmount(true),
                1000);
    }
}
