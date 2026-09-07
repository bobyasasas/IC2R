package ic2.neoforge.machine;

import ic2.core.energy.EnergyStore;

import java.util.function.Consumer;
import java.util.function.Supplier;

/** Enlists pure machine state and EU storage in the same transaction as its inventory. */
final class MachineJournal<S> extends StateJournal<MachineJournal.Snapshot<S>> {
    record Snapshot<S>(double energy, S state) {}

    MachineJournal(EnergyStore energy, Supplier<S> capture, Consumer<S> restore, Runnable changed) {
        super(
                () -> new Snapshot<>(energy.stored(), capture.get()),
                snapshot -> {
                    energy.restore(snapshot.energy());
                    restore.accept(snapshot.state());
                },
                changed);
    }
}
