package ic2.neoforge.machine;

import ic2.core.energy.grid.EnergyNode;
import ic2.neoforge.component.ModDataComponents;
import ic2.neoforge.item.CrystalMemoryItem;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.uu.UuValues;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import org.jetbrains.annotations.Nullable;

/**
 * UU-matter scanner (legacy TileEntityScanner, 256 EU/tick for 3300 ticks, tier 4): resolves the
 * input item against the UU value graph and, once complete, keeps the resolved pattern in memory
 * until the GUI record button stores it on an inserted crystal memory or into an adjacent pattern
 * storage (legacy two-phase completion). Items the graph never heard of fail immediately; items it
 * knows but cannot value still burn a full scan before failing (legacy quirk — the machine then
 * holds until the input changes or the reset button is pressed).
 */
public final class UuScannerBlockEntity extends PoweredBlockEntity {
    public static final int INPUT = 0;
    public static final int DISK = 1;
    public static final int SCANNER_TICKS = 3300;
    private static final int EU_PER_TICK = 256;

    private int progress;
    private ItemStack currentStack = ItemStack.EMPTY;
    private ItemStack pattern = ItemStack.EMPTY;
    private String state = "IDLE";

    public UuScannerBlockEntity(BlockPos pos, BlockState state) {
        super(
                ModMachines.entityType(((MachineBlock) state.getBlock()).kind()),
                pos,
                state,
                512000,
                2);
    }

    @Override
    protected boolean acceptsInventorySlot(int slot, ItemResource resource) {
        if (slot == DISK) return resource.getItem() instanceof CrystalMemoryItem;
        return resource.getItem() != Items.AIR;
    }

    @Override
    public ResourceHandler<ItemResource> automation(Direction side) {
        return new ic2.neoforge.transfer.ResourcePort<>(
                inventory, slot -> slot == INPUT, slot -> slot == DISK);
    }

    @Override
    public EnergyNode.Terminal energyNode() {
        // Legacy Energy.asBasicSink(this, 512000, 4): tier 4 accepts 512 EU packets.
        return EnergyNode.Terminal.sink(energy, 512, 1);
    }

    @Override
    public void serverTick(ServerLevel level) {
        boolean newActive = false;
        if (progress < SCANNER_TICKS) {
            var input = inventory.stack(INPUT);
            var storage = patternStorageNear(level);
            boolean hasDisk = inventory.stack(DISK).getItem() instanceof CrystalMemoryItem;
            if (!input.isEmpty()
                    && (currentStack.isEmpty()
                            || ItemStack.isSameItemSameComponents(input, currentStack))) {
                if (storage == null && !hasDisk) {
                    state = "NO_STORAGE";
                    reset();
                } else if (energy.stored() >= EU_PER_TICK) {
                    if (currentStack.isEmpty()) currentStack = input.copyWithCount(1);
                    var graph = UuValues.graph(level);
                    String key = key(currentStack);
                    if (!graph.knows(key)) {
                        // Legacy UuGraph.find comes up empty: instant failure, no energy used.
                        state = "FAILED";
                        pattern = ItemStack.EMPTY;
                    } else if (isPatternRecorded(level, storage)) {
                        state = "ALREADY_RECORDED";
                        reset();
                    } else {
                        newActive = true;
                        state = "SCANNING";
                        // Legacy UuGraph.find: the resolved pattern stays in memory until saved.
                        pattern = currentStack.copyWithCount(1);
                        energy.extract(EU_PER_TICK);
                        progress++;
                        if (progress >= SCANNER_TICKS) {
                            if (Double.isFinite(graph.get(key))) {
                                state = "COMPLETED";
                                try (var transaction = Transaction.openRoot()) {
                                    inventory.extract(
                                            INPUT, ItemResource.of(input), 1, transaction);
                                    transaction.commit();
                                }
                                setChanged();
                            } else {
                                // Known but valueless: the scan fails while finished progress
                                // holds (legacy leaves the machine stuck until reset).
                                state = "FAILED";
                            }
                        }
                    }
                } else {
                    state = "NO_ENERGY";
                }
            } else {
                state = "IDLE";
                reset();
            }
        } else if (pattern.isEmpty()) {
            state = "IDLE";
            progress = 0;
        }

        setActive(newActive);
    }

    /** Legacy onNetworkEvent: 0 delete (reset), 1 save (record the finished scan). */
    @Override
    public boolean menuAction(int action) {
        switch (action) {
            case 0 -> {
                reset();
                return true;
            }
            case 1 -> {
                if (progress >= SCANNER_TICKS) {
                    record();
                    return true;
                }
                return false;
            }
            default -> {
                return false;
            }
        }
    }

    /** Legacy record(): disk first, then the adjacent pattern storage; failure is TRANSFER_ERROR. */
    private void record() {
        if (getLevel() instanceof ServerLevel level
                && !pattern.isEmpty()
                && Double.isFinite(valueOf(level, pattern))) {
            double value = valueOf(level, pattern);
            var memory = inventory.stack(DISK);
            if (memory.getItem() instanceof CrystalMemoryItem crystal) {
                crystal.writePattern(memory, pattern.copy());
                crystal.writeValue(memory, value);
                inventory.set(DISK, ItemResource.of(memory), memory.getCount());
            } else {
                var storage = patternStorageNear(level);
                if (storage == null || !storage.addPattern(pattern)) {
                    state = "TRANSFER_ERROR";
                    return;
                }
            }
        }
        reset();
    }

    private boolean isPatternRecorded(ServerLevel level, @Nullable PatternStorageBlockEntity storage) {
        var memory = inventory.stack(DISK);
        if (memory.getItem() instanceof CrystalMemoryItem crystal) {
            var recorded = crystal.readPattern(memory);
            if (!recorded.isEmpty() && recorded.getItem() == currentStack.getItem()) return true;
        }
        if (storage != null) {
            for (var stored : storage.getPatterns())
                if (stored.getItem() == currentStack.getItem()) return true;
        }
        return false;
    }

    private void reset() {
        progress = 0;
        currentStack = ItemStack.EMPTY;
        pattern = ItemStack.EMPTY;
    }

    private PatternStorageBlockEntity patternStorageNear(ServerLevel level) {
        for (Direction direction : Direction.values()) {
            if (level.getBlockEntity(worldPosition.relative(direction))
                    instanceof PatternStorageBlockEntity storage) return storage;
        }
        return null;
    }

    /** Legacy patternUu = UuIndex.getInBuckets: the graph value in buckets (value × 1e-5). */
    private double valueOf(ServerLevel level, ItemStack stack) {
        return UuValues.graph(level).get(key(stack)) * 1.0E-5;
    }

    private static String key(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    public String state() {
        return state;
    }

    @Override
    public int progress() {
        return progress;
    }

    @Override
    public int progressMaximum() {
        return SCANNER_TICKS;
    }

    @Override
    public int menuValue(int index) {
        return switch (index) {
            case 0 -> state.equals("COMPLETED") ? 1 : 0;
            default -> 0;
        };
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        progress = input.getIntOr("progress", 0);
        currentStack = input.read("currentStack", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        pattern = input.read("pattern", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        state = input.getStringOr("state", "IDLE");
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("progress", progress);
        if (!currentStack.isEmpty()) output.store("currentStack", ItemStack.CODEC, currentStack);
        if (!pattern.isEmpty()) output.store("pattern", ItemStack.CODEC, pattern);
        output.putString("state", state);
    }
}
