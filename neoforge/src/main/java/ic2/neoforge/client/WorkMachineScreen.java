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
    protected boolean showsEnergyBar() {
        return menu.kind() != MachineKind.FLUID_HEAT_GENERATOR;
    }

    @Override
    protected boolean showsProgress() {
        return false;
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        if (menu.kind() == MachineKind.FLUID_HEAT_GENERATOR)
            FluidTankDisplay.draw(
                    minecraft,
                    graphics,
                    leftPos + 25,
                    topPos + 18,
                    net.minecraft.core.registries.BuiltInRegistries.FLUID.byId(menu.familyValue(3)),
                    menu.familyValue(2),
                    menu.familyValue(4));
        Component text =
                (menu.kind().electricWork() || menu.kind().fuelHeat())
                        ? Component.translatable(
                                (menu.kind() == MachineKind.ELECTRIC_HEAT_GENERATOR
                                                || menu.kind().fuelHeat())
                                        ? "ic2.work.heat"
                                        : "ic2.work.kinetic",
                                storedWork())
                        : Component.translatable(
                                "ic2.tooltip.generation",
                                power.format(menu.familyValue(0) / 100.0));
        graphics.text(
                font,
                text,
                leftPos + 78,
                topPos + ((menu.kind().electricWork() || menu.kind().fuelHeat()) ? 53 : 56),
                0xff404040,
                false);
        if ((menu.kind().electricWork() || menu.kind().fuelHeat()))
            graphics.text(
                    font,
                    Component.translatable(
                            (menu.kind() == MachineKind.ELECTRIC_HEAT_GENERATOR
                                            || menu.kind().fuelHeat())
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

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        if (menu.kind() == MachineKind.FLUID_HEAT_GENERATOR)
            FluidTankDisplay.tooltip(
                    minecraft,
                    graphics,
                    leftPos + 25,
                    topPos + 18,
                    mouseX,
                    mouseY,
                    net.minecraft.core.registries.BuiltInRegistries.FLUID.byId(menu.familyValue(3)),
                    menu.familyValue(2),
                    menu.familyValue(4));
    }

    private String storedWork() {
        double amount = menu.kind().fuelHeat() ? menu.familyFloat(0) : menu.familyValue(0);
        if (amount >= 1e12) return power.format(amount / 1e12) + "T";
        if (amount >= 1e9) return power.format(amount / 1e9) + "G";
        if (amount >= 1e6) return power.format(amount / 1e6) + "M";
        if (amount >= 1e5) return power.format(amount / 1e3) + "k";
        return power.format(amount);
    }
}
