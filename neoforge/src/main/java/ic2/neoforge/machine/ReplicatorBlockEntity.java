package ic2.neoforge.machine;

import ic2.neoforge.fluid.FluidDefinition;
import ic2.neoforge.registration.ModCells;
import ic2.neoforge.registration.ModFluids;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.transfer.MachineFluidTank;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Replicator (legacy TileEntityReplicator): materialises the selected pattern from an adjacent
 * pattern storage using UU-matter. The pattern costs its UU graph value converted legacy-style
 * ({@code getInBuckets = value × 1e-5}, 1 bucket = 1000 mB), consumed at the legacy rate of 1e-4
 * buckets (0.1 mB) per tick at 512 EU/tick, with the fractional remainder banked between integer mB
 * drains (legacy {@code extraUuStored}). Modes: stopped, single and continuous.
 */
public final class ReplicatorBlockEntity extends PoweredBlockEntity {
    public static final int TANK_CAPACITY = 16000;
    public static final int FLUID_SLOT = 0, CELL_SLOT = 1, OUTPUT = 2;

    private static final double UU_PER_TICK_MB = 0.1;

    private final MachineFluidTank tank =
            new MachineFluidTank(
                    TANK_CAPACITY,
                    this::setChanged,
                    resource ->
                            resource.is(
                                    ModFluids.FAMILIES
                                            .get(FluidDefinition.UU_MATTER)
                                            .source()
                                            .get()));
    private final List<ItemStack> patterns = new ArrayList<>();
    private int patternIndex;
    private double uuProcessed;

    /** mB banked from past integer drains, covering sub-mB work (legacy extraUuStored). */
    private double uuBank;

    private int mode; // 0 stopped, 1 single, 2 continuous

    public ReplicatorBlockEntity(BlockPos pos, BlockState state) {
        super(
                ModMachines.entityType(((MachineBlock) state.getBlock()).kind()),
                pos,
                state,
                2000000,
                ((MachineBlock) state.getBlock()).kind().slots());
    }

    private PatternStorageBlockEntity patternStorage() {
        if (getLevel() instanceof ServerLevel level) {
            for (Direction direction : Direction.values()) {
                if (level.getBlockEntity(worldPosition.relative(direction))
                        instanceof PatternStorageBlockEntity storage) return storage;
            }
        }
        return null;
    }

    private void refreshPatterns() {
        var storage = patternStorage();
        patterns.clear();
        if (storage != null) patterns.addAll(storage.getPatterns());
        if (patterns.isEmpty()) mode = 0;
        else patternIndex = Math.floorMod(patternIndex, patterns.size());
    }

    /** Exposes the UU tank through the fluid port capability surface. */
    public ResourceHandler<FluidResource> tankView() {
        return new ResourceHandler<>() {
            @Override
            public int size() {
                return 1;
            }

            @Override
            public FluidResource getResource(int index) {
                return tank.getResource(0);
            }

            @Override
            public long getAmountAsLong(int index) {
                return tank.getAmountAsLong(0);
            }

            @Override
            public long getCapacityAsLong(int index, FluidResource resource) {
                return TANK_CAPACITY;
            }

            @Override
            public boolean isValid(int index, FluidResource resource) {
                return resource.is(uuMatter().getFluid());
            }

            @Override
            public int insert(
                    int index, FluidResource resource, int amount, TransactionContext transaction) {
                return tank.insert(0, resource, amount, transaction);
            }

            @Override
            public int extract(
                    int index, FluidResource resource, int amount, TransactionContext transaction) {
                return tank.extract(0, resource, amount, transaction);
            }
        };
    }

    @Override
    public ResourceHandler<ItemResource> automation(Direction side) {
        return new ic2.neoforge.transfer.ResourcePort<>(
                inventory, slot -> slot == FLUID_SLOT, slot -> slot == CELL_SLOT || slot == OUTPUT);
    }

    @Override
    public ic2.core.energy.grid.EnergyNode.Terminal energyNode() {
        return ic2.core.energy.grid.EnergyNode.Terminal.sink(energy, 512, 1);
    }

    @Override
    public void serverTick(ServerLevel level) {
        refreshPatterns();
        // Fill the UU tank from a UU-matter cell in the fluid slot (whole-cell exchange).
        if (tank.getAmountAsInt(0) < TANK_CAPACITY
                && inventory.stack(FLUID_SLOT).is(uuCell().getItem())) {
            try (var transaction = Transaction.openRoot()) {
                if (inventory.extract(FLUID_SLOT, ItemResource.of(uuCell()), 1, transaction) == 1) {
                    tank.insert(0, uuMatter(), 1000, transaction);
                    inventory.set(
                            CELL_SLOT,
                            ItemResource.of(ModCells.EMPTY.get().getDefaultInstance()),
                            1);
                    transaction.commit();
                    setChanged();
                }
            }
        }
        boolean running = false;
        if (mode != 0 && !patterns.isEmpty() && energy.stored() >= 512) {
            var pattern = patterns.get(Math.floorMod(patternIndex, patterns.size()));
            double requiredMb = requiredMb(level, pattern);
            boolean fits =
                    inventory.stack(OUTPUT).isEmpty()
                            || inventory.stack(OUTPUT).is(pattern.getItem())
                                    && inventory.stack(OUTPUT).getCount() + pattern.getCount()
                                            <= 64;
            if (Double.isFinite(requiredMb) && fits) {
                double uuRemaining = requiredMb - uuProcessed;
                boolean finish;
                if (uuRemaining <= UU_PER_TICK_MB) {
                    finish = true;
                } else {
                    uuRemaining = UU_PER_TICK_MB;
                    finish = false;
                }
                if (consumeUu(uuRemaining)) {
                    running = true;
                    energy.extract(512);
                    uuProcessed += uuRemaining;
                    if (finish) {
                        uuProcessed = 0;
                        if (mode == 1) mode = 0;
                        patternIndex = Math.floorMod(patternIndex + 1, patterns.size());
                        try (var transaction = Transaction.openRoot()) {
                            inventory.insert(
                                    OUTPUT,
                                    ItemResource.of(pattern),
                                    pattern.getCount(),
                                    transaction);
                            transaction.commit();
                        }
                        setChanged();
                    }
                } else {
                    setActive(false);
                    return;
                }
            }
        }
        setActive(running);
    }

    /** Legacy conversion: the pattern's graph value in buckets × 1000 mB per bucket. */
    private double requiredMb(ServerLevel level, ItemStack pattern) {
        double value =
                ic2.neoforge.uu.UuValues.graph(level)
                        .get(
                                net.minecraft.core.registries.BuiltInRegistries.ITEM
                                        .getKey(pattern.getItem())
                                        .toString());
        return value * 1.0E-5 * 1000.0;
    }

    /**
     * Legacy consumeUu: sub-mB work is served from the bank; a drain takes the smallest whole mB
     * that covers the remaining work and banks what it did not use. Unlike the legacy code the bank
     * is restored when the tank cannot supply the whole drain.
     */
    private boolean consumeUu(double amountMb) {
        if (amountMb <= uuBank) {
            uuBank -= amountMb;
            return true;
        }
        amountMb -= uuBank;
        int toDrain = (int) Math.ceil(amountMb);
        try (var drain = Transaction.openRoot()) {
            int extracted = tank.extract(0, uuMatter(), toDrain, drain);
            if (extracted != toDrain) return false;
            uuBank = toDrain - amountMb;
            drain.commit();
            return true;
        }
    }

    private FluidResource uuMatter() {
        return FluidResource.of(ModFluids.FAMILIES.get(FluidDefinition.UU_MATTER).source().get());
    }

    private ItemStack uuCell() {
        return ModCells.CELLS.get("uu_matter_cell").get().getDefaultInstance();
    }

    @Override
    public boolean menuAction(int action) {
        refreshPatterns();
        switch (action) {
            case 0 -> {
                if (!patterns.isEmpty())
                    patternIndex = Math.floorMod(patternIndex - 1, patterns.size());
                return true;
            }
            case 1 -> {
                if (!patterns.isEmpty())
                    patternIndex = Math.floorMod(patternIndex + 1, patterns.size());
                return true;
            }
            case 2 -> {
                if (!patterns.isEmpty()) {
                    mode = 1;
                    return true;
                }
                return false;
            }
            case 3 -> {
                if (!patterns.isEmpty()) {
                    mode = 2;
                    return true;
                }
                return false;
            }
            case 4 -> {
                mode = 0;
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    public int tankAmount() {
        return tank.getAmountAsInt(0);
    }

    public int mode() {
        return mode;
    }

    @Override
    public int menuValue(int index) {
        return switch (index) {
            case 0 -> Float.floatToIntBits((float) tank.getAmountAsInt(0) / TANK_CAPACITY);
            case 1 -> mode;
            default -> 0;
        };
    }

    @Override
    public int progress() {
        return (int) uuProcessed;
    }

    @Override
    public int progressMaximum() {
        if (patterns.isEmpty() || !(getLevel() instanceof ServerLevel level)) return 1;
        var pattern = patterns.get(Math.floorMod(patternIndex, patterns.size()));
        double requiredMb = requiredMb(level, pattern);
        return Double.isFinite(requiredMb) ? Math.max(1, (int) Math.ceil(requiredMb)) : 1;
    }
}
