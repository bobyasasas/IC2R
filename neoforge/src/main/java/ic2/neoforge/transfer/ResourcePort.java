package ic2.neoforge.transfer;

import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.resource.Resource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.Objects;
import java.util.function.IntPredicate;

/**
 * Read-through view with separate insert/extract permissions; bulk operations use these same rules.
 */
public final class ResourcePort<T extends Resource> implements ResourceHandler<T> {
    private final ResourceHandler<T> contents;
    private final IntPredicate canInsert, canExtract;
    private final java.util.function.BiPredicate<Integer, T> accepts;

    public ResourcePort(
            ResourceHandler<T> contents, IntPredicate canInsert, IntPredicate canExtract) {
        this(contents, canInsert, canExtract, (slot, resource) -> true);
    }

    public ResourcePort(
            ResourceHandler<T> contents,
            IntPredicate canInsert,
            IntPredicate canExtract,
            java.util.function.BiPredicate<Integer, T> accepts) {
        this.accepts = Objects.requireNonNull(accepts);
        this.contents = contents;
        this.canInsert = canInsert;
        this.canExtract = canExtract;
    }

    @Override
    public int size() {
        return contents.size();
    }

    @Override
    public T getResource(int index) {
        return contents.getResource(index);
    }

    @Override
    public long getAmountAsLong(int index) {
        return contents.getAmountAsLong(index);
    }

    @Override
    public long getCapacityAsLong(int index, T resource) {
        return contents.getCapacityAsLong(index, resource);
    }

    @Override
    public boolean isValid(int index, T resource) {
        return canInsert.test(index)
                && accepts.test(index, resource)
                && contents.isValid(index, resource);
    }

    @Override
    public int insert(int index, T resource, int amount, TransactionContext transaction) {
        Objects.checkIndex(index, size());
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        return isValid(index, resource) ? contents.insert(index, resource, amount, transaction) : 0;
    }

    @Override
    public int extract(int index, T resource, int amount, TransactionContext transaction) {
        Objects.checkIndex(index, size());
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        return canExtract.test(index) ? contents.extract(index, resource, amount, transaction) : 0;
    }
}
