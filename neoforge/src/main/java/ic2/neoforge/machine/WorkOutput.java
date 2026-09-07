package ic2.neoforge.machine;

import ic2.core.machine.WorkBuffer;
import ic2.neoforge.api.WorkSource;

import net.minecraft.core.Direction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.IntSupplier;

/** Live sided view; retaining a capability never bypasses rotation, removal or tick budgets. */
final class WorkOutput implements WorkSource {
    private final MachineBlockEntity owner;
    private final boolean directional;
    private final @Nullable Direction side;
    private final WorkBuffer work;
    private final IntSupplier bandwidth;
    private final Consumer<TransactionContext> enlist;

    WorkOutput(
            MachineBlockEntity owner,
            @Nullable Direction side,
            WorkBuffer work,
            IntSupplier bandwidth,
            Consumer<TransactionContext> enlist) {
        this(owner, side, work, bandwidth, enlist, true);
    }

    WorkOutput(
            MachineBlockEntity owner,
            @Nullable Direction side,
            WorkBuffer work,
            IntSupplier bandwidth,
            Consumer<TransactionContext> enlist,
            boolean directional) {
        this.directional = directional;
        this.owner = owner;
        this.side = side;
        this.work = work;
        this.bandwidth = bandwidth;
        this.enlist = enlist;
    }

    private boolean connected() {
        var level = owner.getLevel();
        return !owner.isRemoved()
                && level != null
                && !level.isClientSide()
                && (!directional || side == owner.getBlockState().getValue(MachineBlock.FACING));
    }

    @Override
    public int bandwidth() {
        return connected() ? bandwidth.getAsInt() : 0;
    }

    @Override
    public int available() {
        return connected() ? work.available(owner.getLevel().getGameTime(), bandwidth()) : 0;
    }

    @Override
    public int extract(int maximum, TransactionContext transaction) {
        if (maximum < 0) throw new IllegalArgumentException("Negative work request");
        if (!connected()) return 0;
        enlist.accept(transaction);
        return work.extract(owner.getLevel().getGameTime(), bandwidth(), maximum);
    }
}
