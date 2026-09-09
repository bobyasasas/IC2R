package ic2.neoforge.client;

import ic2.neoforge.menu.MachineMenu;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Scanner screen: input + crystal memory with the standard progress display. */
public final class UuScannerScreen extends MachineScreen {
    public UuScannerScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }
}
