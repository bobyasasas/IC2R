package ic2.neoforge.machine;

import ic2.neoforge.item.CrystalMemoryItem;
import ic2.neoforge.registration.ModMachines;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

import java.util.ArrayList;
import java.util.List;

/**
 * Pattern storage (legacy TileEntityPatternStorage): keeps a duplicate-free list of scanned item
 * patterns, persisted with the block. A crystal memory in the disk slot can be written from the
 * stored list or read into it.
 */
public final class PatternStorageBlockEntity extends MachineBlockEntity {
    public static final int DISK_SLOT = 0;

    private final List<ItemStack> patterns = new ArrayList<>();
    private int index;

    public PatternStorageBlockEntity(BlockPos pos, BlockState state) {
        super(
                ModMachines.entityType(((MachineBlock) state.getBlock()).kind()),
                pos,
                state,
                ((MachineBlock) state.getBlock()).kind().slots());
    }

    public boolean addPattern(ItemStack pattern) {
        for (var existing : patterns)
            if (existing.getItem() == pattern.getItem()) return false;
        patterns.add(pattern.copyWithCount(1));
        setChanged();
        return true;
    }

    public List<ItemStack> getPatterns() {
        return java.util.Collections.unmodifiableList(patterns);
    }

    public ItemStack selectedPattern() {
        if (patterns.isEmpty()) return ItemStack.EMPTY;
        int i = Math.floorMod(index, patterns.size());
        return patterns.get(i);
    }

    /** Legacy onNetworkEvent: 0/1 browse, 2 write the selected pattern onto the disk. */
    @Override
    public boolean menuAction(int action) {
        switch (action) {
            case 0 -> {
                if (!patterns.isEmpty()) index = Math.floorMod(index - 1, patterns.size());
                return true;
            }
            case 1 -> {
                if (!patterns.isEmpty()) index = Math.floorMod(index + 1, patterns.size());
                return true;
            }
            case 2 -> {
                if (!patterns.isEmpty() && !inventory.stack(DISK_SLOT).isEmpty()) {
                    var memory = inventory.stack(DISK_SLOT);
                    if (memory.getItem() instanceof CrystalMemoryItem crystal) {
                        crystal.writePattern(memory, selectedPattern().copy());
                        // Snapshot the UU value with the pattern (legacy tooltips read the live
                        // graph; the port bakes the value in because the graph is server-only).
                        if (getLevel() instanceof net.minecraft.server.level.ServerLevel level) {
                            double value =
                                    ic2.neoforge.uu.UuValues.graph(level)
                                            .get(
                                                    net.minecraft.core.registries.BuiltInRegistries
                                                            .ITEM
                                                            .getKey(selectedPattern().getItem())
                                                            .toString()) * 1.0E-5;
                            crystal.writeValue(memory, value);
                        }
                        setChanged();
                        return true;
                    }
                }
                return false;
            }
            default -> {
                return false;
            }
        }
    }

    @Override
    public int menuValue(int index) {
        return switch (index) {
            case 0 -> patterns.isEmpty() ? -1 : index();
            case 1 -> patterns.size();
            default -> 0;
        };
    }

    private int index() {
        return patterns.isEmpty() ? 0 : Math.floorMod(index, patterns.size());
    }

    @Override
    public void serverTick(ServerLevel level) {}

    @Override
    public int progress() {
        return 0;
    }

    @Override
    public int progressMaximum() {
        return 0;
    }

    @Override
    protected boolean acceptsInventorySlot(int slot, ItemResource resource) {
        return slot == DISK_SLOT;
    }

    @Override
    public ResourceHandler<ItemResource> automation(Direction side) {
        return new ic2.neoforge.transfer.ResourcePort<>(
                inventory, slot -> slot == DISK_SLOT, slot -> slot == DISK_SLOT);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        patterns.clear();
        for (ItemStack stack : input.listOrEmpty("patterns", ItemStack.CODEC)) {
            if (!stack.isEmpty()) patterns.add(stack);
        }
        index = 0;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("patterns", ItemStack.CODEC.listOf(), patterns);
    }

    /** Disk-slot view for tests and tooling. */
    public ItemStack diskStack() {
        return inventory.stack(DISK_SLOT);
    }

}
