package ic2.neoforge.client;

import ic2.neoforge.menu.HologramSlot;
import ic2.neoforge.menu.MachineMenu;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;

/** The batch crafter's hologram grid is edited through menu buttons, not slot clicks. */
public final class BatchCrafterScreen extends MachineScreen {
    public BatchCrafterScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    public void slotClicked(Slot slot, int slotId, int buttonNum, ContainerInput input) {
        if (slot instanceof HologramSlot hologram && input == ContainerInput.PICKUP) {
            if (minecraft.gameMode != null)
                minecraft.gameMode.handleInventoryButtonClick(
                        menu.containerId, hologram.index());
            return;
        }
        super.slotClicked(slot, slotId, buttonNum, input);
    }
}
