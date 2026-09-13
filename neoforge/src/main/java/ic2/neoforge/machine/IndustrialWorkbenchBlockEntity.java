package ic2.neoforge.machine;

import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.registration.ModTools;
import ic2.neoforge.transfer.ResourcePort;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.ArrayList;
import java.util.List;

/**
 * Legacy TileEntityIndustrialWorkbench: a 3x3 grid with an 18 slot buffer, plus two tool combos
 * (hammer and cutter) that each craft a 1x2 [tool, ingredient] recipe. Taking any output consumes
 * its inputs and then tops the grid back up from the buffer (legacy rebalance).
 */
public final class IndustrialWorkbenchBlockEntity extends MachineBlockEntity {
    public static final int GRID = 0;
    public static final int BUFFER = 9;
    public static final int BUFFER_SIZE = 18;
    public static final int HAMMER_TOOL = 27;
    public static final int HAMMER_INPUT = 28;
    public static final int CUTTER_TOOL = 29;
    public static final int CUTTER_INPUT = 30;

    public IndustrialWorkbenchBlockEntity(BlockPos pos, BlockState state) {
        super(
                ModMachines.entityType(MachineKind.INDUSTRIAL_WORKBENCH),
                pos,
                state,
                MachineKind.INDUSTRIAL_WORKBENCH.slots());
    }

    /** One assembled output: 0 = the grid, 1 = hammer combo, 2 = cutter combo. */
    public ItemStack preview(int combo) {
        if (!(level instanceof ServerLevel server)) return ItemStack.EMPTY;
        return switch (combo) {
            case 0 -> assemble(server, CraftingInput.of(3, 3, gridStacks()));
            case 1 -> comboOutput(server, HAMMER_TOOL, HAMMER_INPUT);
            case 2 -> comboOutput(server, CUTTER_TOOL, CUTTER_INPUT);
            default -> throw new IllegalArgumentException("Unknown combo: " + combo);
        };
    }

    /** Legacy onContainerEvent("craft"): consume the inputs behind the taken preview. */
    public void consumeCraft(Player player, int combo) {
        switch (combo) {
            case 0 -> {
                consumeInputs(player, positioned(GRID, 3, 3), GRID, 3);
                rebalance();
            }
            case 1 -> consumeInputs(player, positioned(HAMMER_TOOL, 2, 1), HAMMER_TOOL, 2);
            case 2 -> consumeInputs(player, positioned(CUTTER_TOOL, 2, 1), CUTTER_TOOL, 2);
            default -> throw new IllegalArgumentException("Unknown combo: " + combo);
        }
    }

    /** Legacy onContainerEvent("clear"): move grid contents into the buffer, overflow to the player. */
    public boolean clearGrid(Player player) {
        boolean cleared = false;
        var leftovers = new ArrayList<ItemStack>();
        try (var transaction = Transaction.openRoot()) {
            for (int slot = GRID; slot < GRID + 9; slot++) {
                var stack = inventory.stack(slot);
                if (stack.isEmpty()) continue;
                long remaining = stack.getCount();
                for (int buffer = BUFFER;
                        buffer < BUFFER + BUFFER_SIZE && remaining > 0;
                        buffer++) {
                    remaining -= inventory.insert(
                            buffer, ItemResource.of(stack), (int) remaining, transaction);
                }
                if (remaining < stack.getCount()) {
                    inventory.extract(
                            slot,
                            ItemResource.of(stack),
                            (int) (stack.getCount() - remaining),
                            transaction);
                    cleared = true;
                }
                if (remaining > 0) leftovers.add(stack.copyWithCount((int) remaining));
            }
            transaction.commit();
        }
        for (var leftover : leftovers) player.getInventory().placeItemBackInInventory(leftover);
        return cleared;
    }

    /** Legacy rebalance(): top every grid stack up to the largest count present, pulling from the buffer. */
    public void rebalance() {
        int target = 0;
        for (int slot = GRID; slot < GRID + 9; slot++)
            target = Math.max(target, inventory.getAmountAsInt(slot));
        if (target <= 1) return;
        for (int slot = GRID; slot < GRID + 9; slot++) {
            long need = target - inventory.getAmountAsInt(slot);
            var resource = inventory.getResource(slot);
            if (need <= 0 || inventory.getAmountAsInt(slot) == 0) continue;
            for (int buffer = BUFFER; buffer < BUFFER + BUFFER_SIZE && need > 0; buffer++) {
                if (!inventory.getResource(buffer).equals(resource)) continue;
                long moved = Math.min(need, inventory.getAmountAsInt(buffer));
                if (moved <= 0) continue;
                try (var transaction = Transaction.openRoot()) {
                    long extracted =
                            inventory.extract(buffer, resource, (int) moved, transaction);
                    long inserted = extracted > 0
                            ? inventory.insert(slot, resource, (int) extracted, transaction)
                            : 0;
                    if (inserted < extracted)
                        inventory.insert(
                                buffer, resource, (int) (extracted - inserted), transaction);
                    else need -= inserted;
                    transaction.commit();
                }
            }
        }
    }

    @Override
    protected boolean acceptsInventorySlot(int slot, ItemResource resource) {
        if (slot == HAMMER_TOOL)
            return resource.getItem().builtInRegistryHolder().is(ModTools.FORGE_HAMMERS);
        if (slot == CUTTER_TOOL)
            return resource.getItem().builtInRegistryHolder().is(ModTools.WIRE_CUTTERS);
        return true;
    }

    private ItemStack comboOutput(ServerLevel server, int toolSlot, int inputSlot) {
        var input = positioned(toolSlot, 2, 1).input();
        var holder =
                server.getServer()
                        .getRecipeManager()
                        .getRecipeFor(RecipeType.CRAFTING, input, server)
                        .orElse(null);
        return holder == null ? ItemStack.EMPTY : holder.value().assemble(input);
    }

    /** Trimmed crafting view of a machine region; left/top map input indices back to slots. */
    private CraftingInput.Positioned positioned(int base, int width, int height) {
        var stacks = new ArrayList<ItemStack>(width * height);
        for (int slot = base; slot < base + width * height; slot++)
            stacks.add(inventory.stack(slot));
        return CraftingInput.ofPositioned(width, height, stacks);
    }

    private ItemStack assemble(ServerLevel server, CraftingInput input) {
        if (input.isEmpty()) return ItemStack.EMPTY;
        var holder =
                server.getServer()
                        .getRecipeManager()
                        .getRecipeFor(RecipeType.CRAFTING, input, server)
                        .orElse(null);
        return holder == null ? ItemStack.EMPTY : holder.value().assemble(input);
    }

    /**
     * Vanilla ResultSlot.take semantics on a machine region: the recipe input is trimmed to the
     * occupied bounding box, so every consumed slot is re-derived from left/top offsets.
     */
    private void consumeInputs(
            Player player, CraftingInput.Positioned positioned, int base, int width) {
        var input = positioned.input();
        var holder =
                level.getServer()
                        .getRecipeManager()
                        .getRecipeFor(RecipeType.CRAFTING, input, level)
                        .orElse(null);
        if (holder == null) return;
        NonNullList<ItemStack> remaining = holder.value().getRemainingItems(input);
        try (var transaction = Transaction.openRoot()) {
            for (int y = 0; y < input.height(); y++) {
                for (int x = 0; x < input.width(); x++) {
                    int index = x + y * input.width();
                    int slot = base + x + positioned.left() + (y + positioned.top()) * width;
                    var stack = inventory.stack(slot);
                    if (!stack.isEmpty()
                            && inventory.extract(slot, ItemResource.of(stack), 1, transaction)
                                    != 1) return;
                    var replacement =
                            index < remaining.size() ? remaining.get(index) : ItemStack.EMPTY;
                    if (replacement.isEmpty()) continue;
                    var current = inventory.stack(slot);
                    if (!current.isEmpty()
                            && !ItemStack.isSameItemSameComponents(current, replacement)) {
                        player.getInventory().placeItemBackInInventory(replacement);
                        continue;
                    }
                    long moved =
                            inventory.insert(
                                    slot,
                                    ItemResource.of(replacement),
                                    replacement.getCount(),
                                    transaction);
                    if (moved < replacement.getCount())
                        player.getInventory()
                                .placeItemBackInInventory(
                                        replacement.copyWithCount(
                                                (int) (replacement.getCount() - moved)));
                }
            }
            transaction.commit();
        }
    }

    private List<ItemStack> gridStacks() {
        var stacks = new ArrayList<ItemStack>(9);
        for (int slot = GRID; slot < GRID + 9; slot++) stacks.add(inventory.stack(slot));
        return stacks;
    }

    /** Automation inserts into buffer and combo inputs only; nothing is extractable (legacy Access.I). */
    @Override
    public ResourceHandler<ItemResource> automation(Direction side) {
        return new ResourcePort<>(
                inventory,
                slot -> slot >= BUFFER,
                slot -> false,
                (slot, resource) -> {
                    if (slot == HAMMER_TOOL)
                        return resource.getItem()
                                .builtInRegistryHolder()
                                .is(ModTools.FORGE_HAMMERS);
                    if (slot == CUTTER_TOOL)
                        return resource.getItem()
                                .builtInRegistryHolder()
                                .is(ModTools.WIRE_CUTTERS);
                    return true;
                });
    }

    @Override
    public void serverTick(ServerLevel level) {}

    @Override
    public int progress() {
        return 0;
    }

    @Override
    public int progressMaximum() {
        return 1;
    }
}
