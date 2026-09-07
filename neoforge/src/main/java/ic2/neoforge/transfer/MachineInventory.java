package ic2.neoforge.transfer;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;

import java.util.function.BiPredicate;
import java.util.function.IntUnaryOperator;

/** Inventory whose dirty callback runs on a root commit, never during a rolled-back simulation. */
public final class MachineInventory extends ItemStacksResourceHandler {
    private final Runnable changed;
    private final BiPredicate<Integer, ItemResource> accepts;
    private final IntUnaryOperator limits;

    public MachineInventory(
            int slots, Runnable changed, BiPredicate<Integer, ItemResource> accepts) {
        this(slots, changed, accepts, slot -> Integer.MAX_VALUE);
    }

    public MachineInventory(
            int slots,
            Runnable changed,
            BiPredicate<Integer, ItemResource> accepts,
            IntUnaryOperator limits) {
        super(slots);
        this.limits = limits;
        this.changed = changed;
        this.accepts = accepts;
    }

    @Override
    protected int getCapacity(int slot, ItemResource resource) {
        return Math.min(super.getCapacity(slot, resource), limits.applyAsInt(slot));
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
