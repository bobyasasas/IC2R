package ic2.neoforge.menu;

import ic2.neoforge.transfer.MachineInventory;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Legacy SlotHologramSlot: a read-only template cell synced through the menu but edited
 * only via menu buttons, which copy the carried stack instead of moving it.
 */
public final class HologramSlot extends SyncedViewSlot {
    private final MachineInventory hologram;
    private final int index;

    public HologramSlot(MachineInventory hologram, int index, int x, int y, boolean serverSide) {
        super(x, y, serverSide);
        this.hologram = hologram;
        this.index = index;
    }

    @Override
    protected ItemStack compute() {
        return hologram.stack(index).copy();
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return false;
    }

    @Override
    public boolean mayPickup(Player player) {
        return false;
    }

    @Override
    public ItemStack remove(int amount) {
        return ItemStack.EMPTY;
    }

    /** Menu button id: one per hologram cell (legacy ghost templates hold a single item). */
    public int index() {
        return index;
    }
}
