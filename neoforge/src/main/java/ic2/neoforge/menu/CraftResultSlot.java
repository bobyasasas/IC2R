package ic2.neoforge.menu;

import java.util.function.Consumer;
import java.util.function.Supplier;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Legacy Ic2CraftingResultSlot: renders a computed preview and consumes the crafting
 * grid only when the player actually takes the result.
 */
public final class CraftResultSlot extends SyncedViewSlot {
    private final Supplier<ItemStack> preview;
    private final Consumer<Player> take;

    public CraftResultSlot(
            int x, int y, boolean serverSide, Supplier<ItemStack> preview,
            Consumer<Player> take) {
        super(x, y, serverSide);
        this.preview = preview;
        this.take = take;
    }

    @Override
    protected ItemStack compute() {
        return preview.get();
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return false;
    }

    @Override
    public boolean mayPickup(Player player) {
        return !compute().isEmpty();
    }

    @Override
    public ItemStack remove(int amount) {
        ItemStack result = compute();
        return result.isEmpty() ? ItemStack.EMPTY : result.copy();
    }

    @Override
    public void onTake(Player player, ItemStack stack) {
        take.accept(player);
    }
}
