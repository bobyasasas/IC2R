package ic2.neoforge.machine;

import ic2.core.energy.EnergyStore;

import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;

import java.util.function.Consumer;
import java.util.function.Supplier;

/** Enlists pure machine state and EU storage in the same transaction as its inventory. */
final class MachineJournal<S> extends SnapshotJournal<MachineJournal.Snapshot<S>> {
    record Snapshot<S>(double energy, S state) {}

    private final EnergyStore energy;
    private final Supplier<S> capture;
    private final Consumer<S> restore;
    private final Runnable changed;

    MachineJournal(EnergyStore energy, Supplier<S> capture, Consumer<S> restore, Runnable changed) {
        this.energy = energy;
        this.capture = capture;
        this.restore = restore;
        this.changed = changed;
    }

    @Override
    protected Snapshot<S> createSnapshot() {
        return new Snapshot<>(energy.stored(), capture.get());
    }

    @Override
    protected void revertToSnapshot(Snapshot<S> snapshot) {
        energy.restore(snapshot.energy());
        restore.accept(snapshot.state());
    }

    @Override
    protected void onRootCommit(Snapshot<S> original) {
        if (original.energy() != energy.stored() || !original.state().equals(capture.get()))
            changed.run();
    }
}
