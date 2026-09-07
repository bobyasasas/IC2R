package ic2.neoforge.api;

import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/** Integer HU or KU. Queries do not mutate; extraction participates in the caller's transaction. */
public interface WorkSource {
    int bandwidth();

    int available();

    int extract(int maximum, TransactionContext transaction);
}
