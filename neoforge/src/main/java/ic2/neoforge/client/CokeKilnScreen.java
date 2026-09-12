package ic2.neoforge.client;

import ic2.neoforge.menu.MachineMenu;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Fuel-free kiln controller: shows the charing progress without an energy bar. */
public class CokeKilnScreen extends MachineScreen {
    public CokeKilnScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected boolean showsEnergyBar() {
        return false;
    }
}
