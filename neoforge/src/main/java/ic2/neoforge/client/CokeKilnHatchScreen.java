package ic2.neoforge.client;

import ic2.neoforge.menu.MachineMenu;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Plain one-slot screen for the kiln's input hatch. */
public class CokeKilnHatchScreen extends MachineScreen {
    public CokeKilnHatchScreen(MachineMenu menu, Inventory inventory, Component title) {
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
