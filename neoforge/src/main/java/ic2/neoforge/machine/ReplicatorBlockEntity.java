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
 * pattern storage using UU-matter. Documented unit simplification: the pattern's stack count is
 * paid as mB of UU-matter, one mB per tick at 512 EU/tick (the legacy UU-unit conversion is
 * deferred to the value-graph datapack slice). Modes: stopped, single and continuous.
 */
public final class ReplicatorBlockEntity extends PoweredBlockEntity {
    public static final int TANK_CAPACITY = 16000;
    public static final int FLUID_SLOT = 0, CELL_SLOT = 1, OUTPUT = 2;

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
            boolean fits =
                    inventory.stack(OUTPUT).isEmpty()
                            || inventory.stack(OUTPUT).is(pattern.getItem())
                                    && inventory.stack(OUTPUT).getCount() + pattern.getCount()
                                            <= 64;
            if (fits) {
                running = true;
                boolean paid;
                try (var drain = Transaction.openRoot()) {
                    paid = tank.extract(0, uuMatter(), 1, drain) == 1;
                    drain.commit();
                }
                if (!paid) {
                    setActive(false);
                    return;
                }
                uuProcessed++;
                if (uuProcessed >= requiredMb(pattern)) {
                    uuProcessed = 0;
                    if (mode == 1) mode = 0;
                    patternIndex = Math.floorMod(patternIndex + 1, patterns.size());
                    try (var transaction = Transaction.openRoot()) {
                        inventory.insert(
                                OUTPUT, ItemResource.of(pattern), pattern.getCount(), transaction);
                        transaction.commit();
                    }
                    setChanged();
                }
            }
        }
        setActive(running);
    }

    private double requiredMb(ItemStack pattern) {
        return pattern.getCount();
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
        return patterns.isEmpty() ? 1 : Math.max(1, patterns.get(0).getCount());
    }
}
