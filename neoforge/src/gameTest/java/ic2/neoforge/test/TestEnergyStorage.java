package ic2.neoforge.test;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.HashMap;
import java.util.Map;

/**
 * Sink backing the fake AE2 acceptor block, one instance per block position: game
 * tests run concurrently in one world, so a shared instance would couple their
 * assertions.
 */
public final class TestEnergyStorage implements EnergyHandler {
    private static final Map<BlockPos, TestEnergyStorage> STORES = new HashMap<>();

    private static final long CAPACITY = 1_000_000;
    private long amount;

    public static TestEnergyStorage at(Level level, BlockPos pos) {
        return STORES.computeIfAbsent(pos.immutable(), ignored -> new TestEnergyStorage());
    }

    private TestEnergyStorage() {}

    public long stored() {
        return amount;
    }

    @Override
    public long getAmountAsLong() {
        return amount;
    }

    @Override
    public long getCapacityAsLong() {
        return CAPACITY;
    }

    @Override
    public int insert(int amount, TransactionContext transaction) {
        long accepted = Math.min(amount, CAPACITY - this.amount);
        this.amount += accepted;
        return (int) accepted;
    }

    @Override
    public int extract(int amount, TransactionContext transaction) {
        long extracted = Math.min(amount, this.amount);
        this.amount -= extracted;
        return (int) extracted;
    }
}
