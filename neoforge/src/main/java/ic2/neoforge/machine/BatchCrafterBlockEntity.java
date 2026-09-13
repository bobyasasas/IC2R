package ic2.neoforge.machine;

import ic2.core.machine.MachineProcess;
import ic2.neoforge.item.ElectricItem;
import ic2.neoforge.item.ElectricItemEnergy;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.transfer.MachineInventory;
import ic2.neoforge.transfer.ResourcePort;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.ArrayList;
import java.util.List;

/**
 * Legacy TileEntityBatchCrafter: nine hologram templates stock one recipe, nine ingredient rows
 * keep it fed, and the machine crafts continuously while energy and materials last. Hologram
 * cells are edited only through menu buttons (ghost copies, never consumed).
 */
public final class BatchCrafterBlockEntity extends UpgradeableBlockEntity {
    public static final int INGREDIENTS = 0;
    public static final int OUTPUT = 9;
    public static final int CONTAINERS = 10;
    public static final int BATTERY = 19;

    private final MachineInventory hologram =
            new MachineInventory(9, this::setChanged, (slot, resource) -> true, slot -> 1);

    public BatchCrafterBlockEntity(BlockPos pos, BlockState state) {
        super(ModMachines.entityType(MachineKind.BATCH_CRAFTER), pos, state);
    }

    public MachineInventory hologram() {
        return hologram;
    }

    /** Legacy SlotHologramSlot.slotClick at stack size 1: carried sets the template, empty clears. */
    public boolean hologramClick(Player player, int index) {
        if (index < 0 || index >= hologram.size()) return false;
        var carried = player.containerMenu != null ? player.containerMenu.getCarried() : ItemStack.EMPTY;
        if (carried.isEmpty()) {
            if (hologram.getAmountAsInt(index) == 0) return false;
            hologram.set(index, ItemResource.EMPTY, 0);
        } else {
            var current = hologram.getResource(index);
            if (current.equals(ItemResource.of(carried))
                    && hologram.getAmountAsInt(index) >= 1) return false;
            hologram.set(index, ItemResource.of(carried), 1);
        }
        setChanged();
        return true;
    }

    @Override
    protected boolean acceptsInventorySlot(int slot, ItemResource resource) {
        if (slot >= kind().upgradeStart()) return super.acceptsInventorySlot(slot, resource);
        if (slot == BATTERY) return resource.getItem() instanceof ElectricItem;
        // Output and container rows are machine-managed; ingredient substitution checks only
        // guard the nine template-fed slots below.
        if (slot >= INGREDIENTS + 9) return true;
        if (!(level instanceof ServerLevel server) || server.getServer() == null) return true;
        var template = templateRecipe(server);
        if (template == null) return false;
        var grid = templateStacks();
        grid.set(slot, resource.toStack(1));
        var substituted = CraftingInput.of(3, 3, grid);
        return server.getServer()
                .getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, substituted, server)
                .map(holder -> holder.id().equals(template.id()))
                .orElse(false);
    }

    @Override
    public void serverTick(ServerLevel level) {
        refreshUpgrades();
        dischargeBattery();

        var template = templateRecipe(level);
        var order =
                template == null
                        ? null
                        : new MachineProcess.WorkOrder(
                                template.id().toString(),
                                upgradeProfile().ticks(),
                                upgradeProfile().euPerTick());
        var drops = new ArrayList<ItemStack>();
        // Fits checks open their own probe transactions, so they stay outside the work transaction.
        boolean fits = template != null
                && ingredientsMatch(level, template)
                && outputFits(craftResult(template));
        MachineProcess.Outcome outcome;
        try (var transaction = Transaction.openRoot()) {
            journal.updateSnapshots(transaction);
            outcome = process.tick(order, fits, energy);
            if (outcome == MachineProcess.Outcome.COMPLETED) {
                var crafted = craft(template, transaction);
                // A null craft means the fits probes raced reality; roll this tick back.
                if (crafted == null) return;
                drops.addAll(crafted);
            }
            transaction.commit();
        }
        if (outcome == MachineProcess.Outcome.COMPLETED && template != null) {
            for (int operation = 1;
                    operation < Math.min(64, upgradeProfile().operations());
                    operation++) {
                if (!ingredientsMatch(level, template)
                        || !outputFits(craftResult(template))) break;
                try (var transaction = Transaction.openRoot()) {
                    var crafted = craft(template, transaction);
                    if (crafted == null) break;
                    drops.addAll(crafted);
                    transaction.commit();
                }
            }
        }
        for (var drop : drops) Containers.dropItemStack(
                        level,
                        worldPosition.getX() + 0.5,
                        worldPosition.getY() + 0.5,
                        worldPosition.getZ() + 0.5,
                        drop);
        finishProcessingTick(level);
        setActive(
                outcome == MachineProcess.Outcome.RUNNING
                        || outcome == MachineProcess.Outcome.COMPLETED);
    }

    private void dischargeBattery() {
        var battery = inventory.stack(BATTERY);
        double charge =
                ElectricItemEnergy.discharge(
                        battery, energy.free(), upgradeProfile().itemTier(), false, true, false);
        if (charge > 0) {
            inventory.set(BATTERY, ItemResource.of(battery), battery.getCount());
            energy.insert(charge);
        }
    }

    /**
     * One templated craft, atomically within the caller's transaction. Vanilla ResultSlot.take
     * slot mapping: the recipe input is trimmed to the occupied bounding box, so consumed slots
     * are re-derived from the left/top offsets. Returns the overflow drops, or null (rolled
     * back) when the craft does not apply.
     */
    private List<ItemStack> craft(RecipeHolder<CraftingRecipe> template, Transaction transaction) {
        var drops = new ArrayList<ItemStack>();
        var positioned = CraftingInput.ofPositioned(3, 3, ingredientStacks());
        var ingredients = positioned.input();
        var result = template.value().assemble(ingredients);
        if (result.isEmpty()
                || inventory.insert(OUTPUT, ItemResource.of(result), result.getCount(), transaction)
                        != result.getCount()) {
            return null;
        }
        NonNullList<ItemStack> remaining = template.value().getRemainingItems(ingredients);
        for (int y = 0; y < ingredients.height(); y++) {
            for (int x = 0; x < ingredients.width(); x++) {
                int index = x + y * ingredients.width();
                int slot = INGREDIENTS + x + positioned.left() + (y + positioned.top()) * 3;
                var stack = inventory.stack(slot);
                if (stack.isEmpty()) continue;
                if (inventory.extract(slot, ItemResource.of(stack), 1, transaction) != 1)
                    return null;
                var remainder = index < remaining.size() ? remaining.get(index) : ItemStack.EMPTY;
                if (remainder.isEmpty()) continue;
                long left =
                        remainder.getCount()
                                - inventory.insert(
                                        slot,
                                        ItemResource.of(remainder),
                                        remainder.getCount(),
                                        transaction);
                for (int out = CONTAINERS; out < CONTAINERS + 9 && left > 0; out++)
                    left -= inventory.insert(out, ItemResource.of(remainder), (int) left, transaction);
                if (left > 0) drops.add(remainder.copyWithCount((int) left));
            }
        }
        return drops;
    }

    private RecipeHolder<CraftingRecipe> templateRecipe(ServerLevel server) {
        var grid = CraftingInput.of(3, 3, templateStacks());
        if (grid.isEmpty()) return null;
        return server.getServer()
                .getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, grid, server)
                .orElse(null);
    }

    private boolean ingredientsMatch(ServerLevel server, RecipeHolder<CraftingRecipe> template) {
        var ingredients = CraftingInput.of(3, 3, ingredientStacks());
        if (ingredients.isEmpty()) return false;
        var matched =
                server.getServer()
                        .getRecipeManager()
                        .getRecipeFor(RecipeType.CRAFTING, ingredients, server)
                        .orElse(null);
        return matched != null && matched.id().equals(template.id());
    }

    private ItemStack craftResult(RecipeHolder<CraftingRecipe> template) {
        var ingredients = CraftingInput.of(3, 3, ingredientStacks());
        return ingredients.isEmpty() ? ItemStack.EMPTY : template.value().assemble(ingredients);
    }

    private boolean outputFits(ItemStack result) {
        if (result.isEmpty()) return false;
        try (var transaction = Transaction.openRoot()) {
            return inventory.insert(
                            OUTPUT, ItemResource.of(result), result.getCount(), transaction)
                    == result.getCount();
        }
    }

    private List<ItemStack> templateStacks() {
        var stacks = new ArrayList<ItemStack>(9);
        for (int slot = 0; slot < 9; slot++) stacks.add(hologram.stack(slot));
        return stacks;
    }

    private List<ItemStack> ingredientStacks() {
        var stacks = new ArrayList<ItemStack>(9);
        for (int slot = INGREDIENTS; slot < INGREDIENTS + 9; slot++)
            stacks.add(inventory.stack(slot));
        return stacks;
    }

    @Override
    public ResourceHandler<ItemResource> automation(Direction side) {
        return new ResourcePort<>(
                inventory,
                slot -> slot < INGREDIENTS + 9 || slot == BATTERY,
                this::outputSlot);
    }

    private boolean outputSlot(int slot) {
        return slot == OUTPUT || (slot >= CONTAINERS && slot < CONTAINERS + 9);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hologram.deserialize(input.childOrEmpty("hologram"));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        hologram.serialize(output.child("hologram"));
    }
}
