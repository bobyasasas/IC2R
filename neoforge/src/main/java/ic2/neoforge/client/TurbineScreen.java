package ic2.neoforge.client;

import ic2.core.machine.RotorOperation;
import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.menu.MachineMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.text.DecimalFormat;
import java.util.Locale;

public final class TurbineScreen extends MachineScreen {
    private final DecimalFormat number = new DecimalFormat("0.#");

    public TurbineScreen(MachineMenu menu, Inventory inventory, Component title) {
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
        var statuses = RotorOperation.Status.values();
        var status = statuses[Math.clamp(menu.familyValue(1), 0, statuses.length - 1)];
        drawFittedText(
                graphics,
                Component.translatable(
                        menu.kind() == MachineKind.WATER_KINETIC_GENERATOR
                                ? "ic2.rotor.shore"
                                : "ic2.rotor.wind",
                        number.format(menu.familyFloat(3))),
                8, 24, 160, 0xff404040);
        drawFittedText(
                graphics,
                Component.translatable("ic2.rotor.obstructions", menu.familyValue(2)),
                8, 36, 160, 0xff404040);
        drawFittedText(
                graphics,
                Component.translatable("ic2.rotor.output", menu.familyValue(0)),
                8, 53, 98, 0xff404040);
        drawFittedText(
                graphics,
                Component.translatable("ic2.rotor.health", menu.progress() / 10),
                109, 53, 59, 0xff404040);
        drawFittedText(
                graphics,
                Component.translatable(
                        "ic2.rotor.status." + status.name().toLowerCase(Locale.ROOT)),
                8, 65, 160, 0xff404040);
    }
}
