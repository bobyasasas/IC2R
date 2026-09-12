package ic2.neoforge.machine;

import ic2.core.energy.VoltageTier;
import ic2.neoforge.fluid.FluidDefinition;
import ic2.neoforge.item.UpgradeItem;
import ic2.neoforge.registration.ModCells;
import ic2.neoforge.registration.ModFluids;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.transfer.MachineFluidTank;
import ic2.neoforge.transfer.UpgradeTransfers;
import ic2.neoforge.uu.UuValues;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
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
 * ({@code getInBuckets = value × 1e-5}, 1 bucket = 1000 mB), consumed at 1e-4 buckets (0.1 mB)
 * per tick at 512 EU/tick, with the fractional remainder banked between integer mB drains (legacy
 * {@code extraUuStored}; unlike legacy the bank is restored when the tank cannot cover a drain —
 * that legacy leak was fixed deliberately in an earlier slice). Modes: stopped, single and
 * continuous. Four upgrade slots modulate the rates exactly like legacy: overclockers divide the
 * UU rate by 0.7ⁿ and multiply the draw by 1.6ⁿ, transformers raise the sink tier, storage
 * upgrades add 10000 EU of capacity. Selecting a different pattern (or losing it) resets the
 * progress and stops the machine (legacy refreshInfo).
 */
public final class ReplicatorBlockEntity extends PoweredBlockEntity implements FluidMachine {
    public static final int TANK_CAPACITY = 16000;
    public static final int FLUID_SLOT = 0, CELL_SLOT = 1, OUTPUT = 2;

    private static final double BASE_UU_PER_TICK = 1.0E-4; // buckets
    private static final double BASE_EU_PER_TICK = 512.0;
    private static final int BASE_TIER = 4;
    private static final double BASE_CAPACITY = 2000000.0;

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
    private double uuPerTick = BASE_UU_PER_TICK;
    private double euPerTick = BASE_EU_PER_TICK;
    private ItemStack selected = ItemStack.EMPTY;

    /** mB banked from past integer drains, covering sub-mB work (legacy extraUuStored). */
    private double uuBank;

    private int mode; // 0 stopped, 1 single, 2 continuous

    private int overclockers = -1, transformers = -1, storageUpgrades = -1;

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
        if (patterns.isEmpty()) {
            mode = 0;
            uuProcessed = 0;
            selected = ItemStack.EMPTY;
        } else {
            patternIndex = Math.floorMod(patternIndex, patterns.size());
            var current = patterns.get(patternIndex);
            if (!ItemStack.isSameItemSameComponents(current, selected)) {
                // Legacy refreshInfo: a different selected pattern restarts from zero, stopped.
                selected = current.copyWithCount(1);
                uuProcessed = 0;
                mode = 0;
            }
        }
    }

    /** Legacy upgrade arithmetic: overclocker 0.7ⁿ speed / 1.6ⁿ draw, transformer +1 tier. */
    private void refreshUpgrades() {
        int speed = 0, tier = 0, storage = 0;
        for (int slot = kind().upgradeStart(); slot < inventory.size(); slot++) {
            var stack = inventory.stack(slot);
            if (!(stack.getItem() instanceof UpgradeItem item)) continue;
            switch (item.kind()) {
                case OVERCLOCKER -> speed += stack.getCount();
                case TRANSFORMER -> tier += stack.getCount();
                case ENERGY_STORAGE -> storage += stack.getCount();
                default -> {}
            }
        }
        if (speed == overclockers && tier == transformers && storage == storageUpgrades) return;
        overclockers = speed;
        transformers = tier;
        storageUpgrades = storage;
        uuPerTick = BASE_UU_PER_TICK / Math.pow(0.7, speed);
        euPerTick = BASE_EU_PER_TICK * Math.pow(1.6, speed);
        energy.resize(BASE_CAPACITY + 10000.0 * storage);
        setChanged();
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
    public ResourceHandler<FluidResource> fluidAutomation(Direction side) {
        return tankView();
    }

    @Override
    public ResourceHandler<ItemResource> automation(Direction side) {
        return new ic2.neoforge.transfer.ResourcePort<>(
                inventory, slot -> slot == FLUID_SLOT, slot -> slot == CELL_SLOT || slot == OUTPUT);
    }

    @Override
    public ic2.core.energy.grid.EnergyNode.Terminal energyNode() {
        refreshUpgrades();
        return ic2.core.energy.grid.EnergyNode.Terminal.sink(
                energy,
                VoltageTier.fromIcTier(Math.clamp(BASE_TIER + transformers, 1, 5)).getVoltage(),
                1);
    }

    @Override
    public void serverTick(ServerLevel level) {
        refreshUpgrades();
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
        if (mode != 0 && !patterns.isEmpty() && energy.stored() >= euPerTick) {
            var pattern = patterns.get(Math.floorMod(patternIndex, patterns.size()));
            double requiredMb = requiredMb(level, pattern);
            boolean fits =
                    inventory.stack(OUTPUT).isEmpty()
                            || inventory.stack(OUTPUT).is(pattern.getItem())
                                    && inventory.stack(OUTPUT).getCount() + pattern.getCount()
                                            <= 64;
            if (fits) {
                double uuRemaining = requiredMb - uuProcessed;
                boolean finish;
                if (uuRemaining <= uuPerTick * 1000.0) {
                    finish = true;
                } else {
                    uuRemaining = uuPerTick * 1000.0;
                    finish = false;
                }
                if (consumeUu(uuRemaining)) {
                    running = true;
                    energy.extract(euPerTick);
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
        UpgradeTransfers.tick(level, this);
    }

    /** Legacy conversion: the pattern's graph value in buckets × 1000 mB per bucket. */
    private double requiredMb(ServerLevel level, ItemStack pattern) {
        return UuValues.graph(level)
                        .get(BuiltInRegistries.ITEM.getKey(pattern.getItem()).toString())
                * 1.0E-5
                * 1000.0;
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

    /** Legacy onNetworkEvent: 0/1 browse (stopped only), 3 stop, 4 single, 5 continuous. */
    @Override
    public boolean menuAction(int action) {
        refreshPatterns();
        switch (action) {
            case 0, 1 -> {
                if (mode == 0 && !patterns.isEmpty()) {
                    patternIndex =
                            Math.floorMod(patternIndex + (action == 0 ? -1 : 1), patterns.size());
                    return true;
                }
                return false;
            }
            case 3 -> {
                if (mode != 0) {
                    uuProcessed = 0;
                    mode = 0;
                }
                return true;
            }
            case 4, 5 -> {
                if (!patterns.isEmpty()) {
                    mode = action == 4 ? 1 : 2;
                    return true;
                }
                return false;
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

    public double uuProcessed() {
        return uuProcessed;
    }

    public double uuBank() {
        return uuBank;
    }

    @Override
    public int menuValue(int index) {
        return switch (index) {
            case 0 -> Float.floatToIntBits((float) tank.getAmountAsInt(0) / TANK_CAPACITY);
            case 1 -> mode;
            case 2 -> patterns.isEmpty() ? 0 : Math.floorMod(patternIndex, patterns.size()) + 1;
            case 3 -> patterns.size();
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

    @Override
    protected void loadAdditional(ValueInput input) {
        // The saved charge may exceed the pre-upgrade capacity; capture it before the base clamps.
        double raw = input.getDoubleOr("energy", 0);
        super.loadAdditional(input);
        uuProcessed = input.getDoubleOr("uuProcessed", 0);
        uuBank = input.getDoubleOr("extraUuStored", 0);
        patternIndex = input.getIntOr("index", 0);
        mode = input.getIntOr("mode", 0);
        selected = input.read("pattern", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        tank.deserialize(input.childOrEmpty("tank"));
        refreshUpgrades();
        if (raw > energy.capacity()) energy.forceAdd(raw - energy.capacity());
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        tank.serialize(output.child("tank"));
        output.putDouble("uuProcessed", uuProcessed);
        output.putDouble("extraUuStored", uuBank);
        output.putInt("index", patternIndex);
        output.putInt("mode", mode);
        if (!selected.isEmpty()) output.store("pattern", ItemStack.CODEC, selected);
    }
}
