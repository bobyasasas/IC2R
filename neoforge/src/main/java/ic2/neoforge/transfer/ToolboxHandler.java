package ic2.neoforge.transfer;

import ic2.neoforge.component.ModDataComponents;
import ic2.neoforge.item.ToolboxItem;

import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.item.ItemAccessItemHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

/**
 * Immediate component writes keep contents with the item when it is moved, dropped or disconnected.
 */
public final class ToolboxHandler extends ItemAccessItemHandler {
    private final @Nullable UUID identity;

    public ToolboxHandler(ItemAccess access) {
        super(access, ModDataComponents.TOOLBOX_CONTENTS.get(), 9);
        identity = access.getResource().get(ModDataComponents.TOOLBOX_ID);
    }

    private boolean bound(ItemResource resource) {
        return resource.is(validItem)
                && Objects.equals(identity, resource.get(ModDataComponents.TOOLBOX_ID));
    }

    @Override
    protected ItemContainerContents getContents(ItemResource resource) {
        return bound(resource) ? super.getContents(resource) : ItemContainerContents.EMPTY;
    }

    @Override
    public boolean isValid(int index, ItemResource resource) {
        return bound(itemAccess.getResource()) && ToolboxItem.accepts(resource);
    }

    public void set(int index, ItemResource resource, int amount) {
        Objects.checkIndex(index, 9);
        if (!bound(itemAccess.getResource())
                || amount < 0
                || amount > getCapacityAsInt(index, resource)
                || amount > 0 && !isValid(index, resource))
            throw new IllegalArgumentException("Invalid toolbox slot change");
        try (var tx = Transaction.openRoot()) {
            var updated = update(itemAccess.getResource(), index, resource, amount);
            if (itemAccess.exchange(updated, 1, tx) != 1)
                throw new IllegalStateException("Toolbox is no longer accessible");
            tx.commit();
        }
    }
}
