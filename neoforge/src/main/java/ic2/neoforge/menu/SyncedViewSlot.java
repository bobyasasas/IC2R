package ic2.neoforge.menu;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.Slot;

/**
 * Slot whose stacks are computed server-side rather than stored. Server menus recompute on
 * every read; client menus keep the last synced stack, because StackCopySlot-style handlers
 * would drop the incoming sync packet and re-emit it forever.
 */
abstract class SyncedViewSlot extends Slot {
    private final boolean serverSide;
    private ItemStack cache = ItemStack.EMPTY;

    SyncedViewSlot(int x, int y, boolean serverSide) {
        super(new SimpleContainer(0), 0, x, y);
        this.serverSide = serverSide;
    }

    @Override
    public final ItemStack getItem() {
        if (serverSide) cache = compute();
        return cache;
    }

    @Override
    public final void set(ItemStack stack) {
        cache = stack;
    }

    @Override
    public final void setChanged() {}

    protected abstract ItemStack compute();
}
