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
        graphics.text(
                font,
                Component.translatable(
                        menu.kind() == MachineKind.WATER_KINETIC_GENERATOR
                                ? "ic2.rotor.shore"
                                : "ic2.rotor.wind",
                        number.format(menu.familyFloat(3))),
                leftPos + 8,
                topPos + 24,
                0xff404040,
                false);
        graphics.text(
                font,
                Component.translatable("ic2.rotor.obstructions", menu.familyValue(2)),
                leftPos + 8,
                topPos + 36,
                0xff404040,
                false);
        graphics.text(
                font,
                Component.translatable("ic2.rotor.output", menu.familyValue(0)),
                leftPos + 8,
                topPos + 53,
                0xff404040,
                false);
        graphics.text(
                font,
                Component.translatable("ic2.rotor.health", menu.progress() / 10),
                leftPos + 109,
                topPos + 53,
                0xff404040,
                false);
        graphics.text(
                font,
                Component.translatable(
                        "ic2.rotor.status." + status.name().toLowerCase(Locale.ROOT)),
                leftPos + 8,
                topPos + 65,
                0xff404040,
                false);
    }
}
