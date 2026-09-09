package ic2.neoforge.machine;

import ic2.core.energy.grid.EnergyNode;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.transfer.MachineInventory;
import ic2.neoforge.transfer.ResourcePort;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * Routes buffer contents into adjacent inventories: a face only receives an item when one of its
 * seven filter slots matches and covers the carried stack, while unmatched items leave through the
 * default face one at a time. Every moved item costs twenty EU.
 */
public final class SortingMachineBlockEntity extends PoweredBlockEntity {
    public static final int BUFFER_SLOTS = 11, FILTERS_PER_FACE = 7, UPGRADES = 3;
    private static final int EU_PER_ITEM = 20;
    private final MachineInventory filters =
            new MachineInventory(
                    6 * FILTERS_PER_FACE, this::setChanged, (slot, resource) -> true, slot -> 1);
    private final ResourceHandler<ItemResource> filterPort =
            new ResourcePort<>(filters, slot -> false, slot -> false, (slot, resource) -> true);
    private Direction defaultRoute = Direction.DOWN;

    public SortingMachineBlockEntity(BlockPos pos, BlockState state) {
        super(ModMachines.entityType(MachineKind.SORTING_MACHINE), pos, state, 15000, 14);
    }

    @Override
    public EnergyNode.Terminal energyNode() {
        return EnergyNode.Terminal.sink(
                energy,
                ic2.core.energy.VoltageTier.fromIcTier(kind().electricalTier()).getVoltage(),
                1);
    }

    public MachineInventory filters() {
        return filters;
    }

    public Direction defaultRoute() {
        return defaultRoute;
    }

    public void defaultRoute(Direction route) {
        defaultRoute = route;
        setChanged();
    }

    private static Direction face(int index) {
        return Direction.values()[index];
    }

    private int faceBase(Direction side) {
        return side.ordinal() * FILTERS_PER_FACE;
    }

    private record Template(ItemResource resource, int count) {}

    private java.util.List<Template> filterSlots(Direction side) {
        var list = new java.util.ArrayList<Template>();
        int base = faceBase(side);
        for (int slot = base; slot < base + FILTERS_PER_FACE; slot++) {
            var stack = filters.stack(slot);
            if (!stack.isEmpty()) list.add(new Template(ItemResource.of(stack), stack.getCount()));
        }
        return list;
    }

    @Override
    public ResourceHandler<ItemResource> automation(Direction side) {
        return new ResourcePort<>(
                inventory,
                slot -> slot < BUFFER_SLOTS,
                slot -> slot < BUFFER_SLOTS,
                (slot, resource) -> slot < BUFFER_SLOTS);
    }

    /** Filter slots are editor-facing only; automation never touches them. */
    public ResourceHandler<ItemResource> filterAutomation() {
        return filterPort;
    }

    @Override
    public void serverTick(ServerLevel level) {
        if (energy.stored() < EU_PER_ITEM) return;
        boolean moved = false;
        for (int slot = 0; slot < BUFFER_SLOTS; slot++) {
            var stack = inventory.stack(slot);
            if (stack.isEmpty()) continue;
            var resource = ItemResource.of(stack);
            for (var direction : Direction.values()) {
                if (direction == defaultRoute || energy.stored() < EU_PER_ITEM) continue;
                for (var filter : filterSlots(direction)) {
                    if (stack.getCount() < filter.count() || !filter.resource().equals(resource))
                        continue;
                    if (energy.stored() < (double) filter.count() * EU_PER_ITEM) continue;
                    if (route(level, direction, resource, filter.count(), slot)) moved = true;
                    break;
                }
            }
        }
        // Default face: any item no filter claims leaves one piece at a time.
        for (int slot = 0; slot < BUFFER_SLOTS && energy.stored() >= EU_PER_ITEM; slot++) {
            var stack = inventory.stack(slot);
            if (stack.isEmpty()) continue;
            var resource = ItemResource.of(stack);
            boolean wanted = false;
            for (var direction : Direction.values()) {
                if (direction == defaultRoute) continue;
                for (var filter : filterSlots(direction)) {
                    if (filter.resource().equals(resource)) {
                        wanted = true;
                        break;
                    }
                }
                if (wanted) break;
            }
            if (wanted) continue;
            if (route(level, defaultRoute, resource, 1, slot)) moved = true;
        }
        setActive(moved);
    }

    /** Atomically moves up to {@code amount} items into the neighbour on {@code side}. */
    private boolean route(
            ServerLevel level, Direction side, ItemResource resource, int amount, int slot) {
        var pos = worldPosition.relative(side);
        if (!level.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) return false;
        var target = level.getCapability(Capabilities.Item.BLOCK, pos, side.getOpposite());
        if (target == null) return false;
        try (var transaction = Transaction.openRoot()) {
            int accepted = target.insert(0, resource, amount, transaction);
            if (accepted <= 0) return false;
            if (inventory.extract(slot, resource, accepted, transaction) != accepted) return false;
            energy.extract((double) accepted * EU_PER_ITEM);
            transaction.commit();
            return true;
        }
    }

    private int deposit(ServerLevel level, Direction side, ItemResource resource, int amount) {
        var pos = worldPosition.relative(side);
        if (!level.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) return 0;
        var target = level.getCapability(Capabilities.Item.BLOCK, pos, side.getOpposite());
        if (target == null) return 0;
        try (var transaction = Transaction.openRoot()) {
            int accepted = target.insert(0, resource, amount, transaction);
            transaction.commit();
            return accepted;
        }
    }

    @Override
    public int progress() {
        return 0;
    }

    @Override
    public int progressMaximum() {
        return 0;
    }

    @Override
    public boolean menuAction(int id) {
        if (id < 0 || id > 5) return false;
        defaultRoute = Direction.values()[id];
        return true;
    }

    @Override
    public int menuValue(int index) {
        return switch (index) {
            case 0 -> defaultRoute.ordinal();
            default -> 0;
        };
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        filters.deserialize(input.childOrEmpty("filters"));
        int route = input.getIntOr("route", Direction.DOWN.ordinal());
        if (route >= 0 && route < 6) defaultRoute = Direction.values()[route];
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        filters.serialize(output.child("filters"));
        output.putInt("route", defaultRoute.ordinal());
    }
}
