package ic2.neoforge.machine;

import ic2.core.energy.grid.EnergyNode;
import ic2.core.uu.UuValueGraph;
import ic2.neoforge.component.ModDataComponents;
import ic2.neoforge.item.CrystalMemoryItem;
import ic2.neoforge.registration.ModMachines;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * UU-matter scanner (legacy TileEntityScanner, 256 EU/tick for 3300 ticks): scans the input item,
 * looks up its UU value in the value graph and records the pattern onto an inserted crystal memory.
 * Items without a value cannot be scanned.
 */
public final class UuScannerBlockEntity extends PoweredBlockEntity {
    public static final int INPUT = 0;
    public static final int DISK = 1;
    public static final int SCANNER_TICKS = 3300;
    private static final int EU_PER_TICK = 256;

    /** Shared value graph, seeded with the slice's base resources. */
    private static final UuValueGraph GRAPH = new UuValueGraph();

    static {
        GRAPH.setInitial("minecraft:iron_ingot", 14);
        GRAPH.setInitial("minecraft:copper_ingot", 14);
        GRAPH.addTransformation(
                new UuValueGraph.Transformation(
                        0,
                        java.util.List.of(java.util.List.of("minecraft:iron_ore")),
                        java.util.List.of("ic2:iron_dust")));
    }

    private int progress;
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
        return EnergyNode.Terminal.sink(energy, 256, 1);
    }

    @Override
    public void serverTick(ServerLevel level) {
        if (progress >= SCANNER_TICKS) {
            state = "COMPLETED";
            setActive(false);
            return;
        }
        var input = inventory.stack(INPUT);
        var memory = inventory.stack(DISK);
        var storage = patternStorageNear(level);
        boolean hasDisk = memory.getItem() instanceof CrystalMemoryItem;
        if (input.isEmpty() || (!hasDisk && storage == null)) {
            state = "NO_STORAGE";
            reset();
            setActive(false);
            return;
        }
        double uu =
                GRAPH.get(
                        net.minecraft.core.registries.BuiltInRegistries.ITEM
                                .getKey(input.getItem())
                                .toString());
        if (Double.isInfinite(uu)) {
            state = "FAILED";
            reset();
            setActive(false);
            return;
        }
        if (energy.stored() < EU_PER_TICK) {
            state = "NO_ENERGY";
            setActive(false);
            return;
        }
        state = "SCANNING";
        energy.extract(EU_PER_TICK);
        progress++;
        setActive(true);
        if (progress >= SCANNER_TICKS) {
            var pattern = new ItemStack(input.getItem());
            if (hasDisk) {
                memory.set(
                        ModDataComponents.CRYSTAL_MEMORY_PATTERN,
                        net.minecraft.world.item.component.ItemContainerContents.fromItems(
                                java.util.List.of(pattern)));
                // stack() hands out a copy; the recorded memory must be written back.
                inventory.set(DISK, ItemResource.of(memory), memory.getCount());
            } else if (storage != null && storage.addPattern(pattern)) {
                // legacy: without a disk the pattern lands in the adjacent pattern storage
            } else {
                setActive(false);
                return;
            }
            try (var transaction = Transaction.openRoot()) {
                inventory.extract(INPUT, ItemResource.of(input), 1, transaction);
                transaction.commit();
            }
            state = "COMPLETED";
            setChanged();
        }
    }

    private ic2.neoforge.machine.PatternStorageBlockEntity patternStorageNear(
            ServerLevel level) {
        for (Direction direction : Direction.values()) {
            if (level.getBlockEntity(worldPosition.relative(direction))
                    instanceof ic2.neoforge.machine.PatternStorageBlockEntity storage)
                return storage;
        }
        return null;
    }

    private void reset() {
        progress = 0;
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
        return 0;
    }
}
