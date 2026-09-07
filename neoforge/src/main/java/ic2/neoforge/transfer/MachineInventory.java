package ic2.neoforge.transfer;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;

import java.util.function.BiPredicate;

/** Inventory whose dirty callback runs on a root commit, never during a rolled-back simulation. */
public final class MachineInventory extends ItemStacksResourceHandler {
    private final Runnable changed;
    private final BiPredicate<Integer, ItemResource> accepts;

    public MachineInventory(
            int slots, Runnable changed, BiPredicate<Integer, ItemResource> accepts) {
        super(slots);
        this.changed = changed;
        this.accepts = accepts;
    }

    public ItemStack stack(int slot) {
        return getResource(slot).toStack(getAmountAsInt(slot));
    }

    @Override
    public boolean isValid(int slot, ItemResource resource) {
        return accepts.test(slot, resource);
    }

    @Override
    protected void onContentsChanged(int slot, ItemStack previousContents) {
        changed.run();
    }
}
