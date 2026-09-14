package ic2.neoforge.client;

import ic2.neoforge.fluid.FluidDefinition;
import ic2.neoforge.machine.ReplicatorBlockEntity;
import ic2.neoforge.menu.MachineMenu;
import ic2.neoforge.registration.ModFluids;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** Replicator screen: pattern browsing and mode buttons (legacy last/next/single/repeat/stop). */
public final class ReplicatorScreen extends MachineScreen {
    public ReplicatorScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    public void init() {
        super.init();
        addLegacyControl(80, 16, 9, 18, 0, () -> Component.translatable("ic2.Replicator.gui.info.last"));
        addLegacyControl(109, 16, 9, 18, 1, () -> Component.translatable("ic2.Replicator.gui.info.next"));
        addLegacyControl(75, 82, 16, 16, 3, () -> Component.translatable("ic2.Replicator.gui.info.Stop"));
        addLegacyControl(92, 82, 16, 16, 4, () -> Component.translatable("ic2.Replicator.gui.info.single"));
        addLegacyControl(109, 82, 16, 16, 5, () -> Component.translatable("ic2.Replicator.gui.info.repeat"));
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int tankAmount =
                Math.round(menu.familyFloat(0) * ReplicatorBlockEntity.TANK_CAPACITY);
        FluidTankDisplay.drawNormal(
                minecraft,
                graphics,
                leftPos + 27,
                topPos + 30,
                ModFluids.FAMILIES.get(FluidDefinition.UU_MATTER).source().get(),
                tankAmount,
                ReplicatorBlockEntity.TANK_CAPACITY);
        if (menu.familyValue(4) >= 0)
            graphics.item(
                    new ItemStack(BuiltInRegistries.ITEM.byId(menu.familyValue(4))),
                    leftPos + 91,
                    topPos + 17);
        int mode = menu.familyValue(1);
        Component status;
        int color;
        if (mode == 0) {
            status = Component.translatable("ic2.Replicator.gui.info.Waiting");
            color = 0xffebeb20;
        } else {
            int percent =
                    menu.progressMaximum() <= 0
                            ? 0
                            : Math.min(100, Math.round(menu.progress() * 100.0F / menu.progressMaximum()));
            status = Component.literal("UU:" + percent + "%  EU:" + percent + "%  >" + (mode == 1 ? "" : ">"));
            color = 0xff20eb3e;
        }
        drawFittedText(graphics, status, 53, 40, 88, color);
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        int tankAmount =
                Math.round(menu.familyFloat(0) * ReplicatorBlockEntity.TANK_CAPACITY);
        FluidTankDisplay.tooltip(
                minecraft,
                graphics,
                leftPos + 27,
                topPos + 30,
                20,
                55,
                mouseX,
                mouseY,
                ModFluids.FAMILIES.get(FluidDefinition.UU_MATTER).source().get(),
                tankAmount,
                ReplicatorBlockEntity.TANK_CAPACITY);
    }
}
