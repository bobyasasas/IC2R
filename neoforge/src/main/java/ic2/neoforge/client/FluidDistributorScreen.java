package ic2.neoforge.client;

import ic2.neoforge.machine.FluidDistributorBlockEntity;
import ic2.neoforge.menu.MachineMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class FluidDistributorScreen extends MachineScreen {
    public FluidDistributorScreen(MachineMenu menu, Inventory inventory, Component title) {
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
    public void init() {
        super.init();
        addLegacyControl(117, 58, 18, 8, 0, this::modeLabel);
    }

    private Component modeLabel() {
        return Component.translatable(
                menu.familyValue(0) == 1
                        ? "ic2.fluid_distributor.gui.mode.concentrate"
                        : "ic2.fluid_distributor.gui.mode.distribute");
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int amount = menu.familyValue(1);
        var fluid = BuiltInRegistries.FLUID.byId(menu.familyValue(2));
        FluidTankDisplay.drawPlain(
                minecraft,
                graphics,
                leftPos + 29,
                topPos + 38,
                55,
                47,
                fluid,
                amount,
                FluidDistributorBlockEntity.CAPACITY);
        drawFittedText(
                graphics,
                Component.translatable("ic2.fluid_distributor.gui.mode.info"),
                116,
                47,
                51,
                0xff57c4da);
        drawFittedText(graphics, modeLabel(), 99, 71, 68, 0xff57c4da);
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        FluidTankDisplay.tooltip(
                minecraft,
                graphics,
                leftPos + 29,
                topPos + 38,
                55,
                47,
                mouseX,
                mouseY,
                BuiltInRegistries.FLUID.byId(menu.familyValue(2)),
                menu.familyValue(1),
                FluidDistributorBlockEntity.CAPACITY);
    }
}
