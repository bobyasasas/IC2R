package ic2.neoforge.client;

import ic2.neoforge.menu.MachineMenu;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Twin 4x6 groups; the buffer has no energy or progress display. */
public final class ItemBufferScreen extends MachineScreen {
    public ItemBufferScreen(MachineMenu menu, Inventory inventory, Component title) {
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
}
