package ic2.neoforge.machine;

import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;

import java.util.function.Consumer;
import java.util.function.Supplier;

/** Transaction journal for immutable machine snapshots; dirty callbacks run only on root commit. */
class StateJournal<S> extends SnapshotJournal<S> {
    private final Supplier<S> capture;
    private final Consumer<S> restore;
    private final Runnable changed;

    StateJournal(Supplier<S> capture, Consumer<S> restore, Runnable changed) {
        this.capture = capture;
        this.restore = restore;
        this.changed = changed;
    }

    @Override
    protected S createSnapshot() {
        return capture.get();
    }

    @Override
    protected void revertToSnapshot(S snapshot) {
        restore.accept(snapshot);
    }

    @Override
    protected void onRootCommit(S original) {
        if (!original.equals(capture.get())) changed.run();
    }
}
