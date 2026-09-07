package ic2.neoforge.client;

import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.menu.MachineMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.text.DecimalFormat;

public final class WorkMachineScreen extends MachineScreen {
    private final DecimalFormat power = new DecimalFormat("0.##");

    public WorkMachineScreen(MachineMenu menu, Inventory inventory, Component title) {
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
        Component text =
                menu.kind().electricWork()
                        ? Component.translatable(
                                menu.kind() == MachineKind.ELECTRIC_HEAT_GENERATOR
                                        ? "ic2.work.heat"
                                        : "ic2.work.kinetic",
                                menu.familyValue(0))
                        : Component.translatable(
                                "ic2.tooltip.generation",
                                power.format(menu.familyValue(0) / 100.0));
        graphics.text(
                font,
                text,
                leftPos + 78,
                topPos + (menu.kind().electricWork() ? 53 : 56),
                0xff404040,
                false);
        if (menu.kind().electricWork())
            graphics.text(
                    font,
                    Component.translatable(
                            menu.kind() == MachineKind.ELECTRIC_HEAT_GENERATOR
                                    ? "ic2.work.heat_rate"
                                    : "ic2.work.kinetic_rate",
                            menu.familyValue(1)),
                    leftPos + 78,
                    topPos + 64,
                    0xff404040,
                    false);
        if (menu.kind().workConversion())
            graphics.text(
                    font,
                    Component.translatable("ic2.work.voltage", menu.familyValue(1)),
                    leftPos + 56,
                    topPos + 25,
                    0xff404040,
                    false);
    }
}
