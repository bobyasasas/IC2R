package ic2.neoforge.machine;

import ic2.neoforge.fluid.FluidDefinition;
import ic2.neoforge.item.UpgradeItem;
import ic2.neoforge.recipe.BlastFurnaceRecipe;
import ic2.neoforge.registration.ModFluids;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.registration.ModProcessingRecipes;
import ic2.neoforge.transfer.FluidContainerPort;
import ic2.neoforge.transfer.MachineFluidTank;
import ic2.neoforge.transfer.ResourcePort;
import ic2.neoforge.transfer.UpgradeTransfers;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * Heat-driven iron smelter: draws HU through its front face until 50,000 heat, consumes air from an
 * 8,000 mB tank (refilled from air cells) at each recipe's per-tick rate, and turns iron into steel
 * plus slag. Mirrors the legacy heat request/cooling pacing exactly.
 */
public final class BlastFurnaceBlockEntity extends MachineBlockEntity {
    public static final int MAX_HEAT = 50000;
    public static final int TANK_CAPACITY = 8000;
    public static final int INPUT = 0, OUTPUT_START = 1, OUTPUT_END = 3, CELL_IN = 3, CELL_OUT = 4;
    private static final int DEFAULT_DURATION = 300;

    private final RecipeManager.CachedCheck<SingleRecipeInput, BlastFurnaceRecipe> recipes =
            RecipeManager.createCheck(ModProcessingRecipes.BLAST_FURNACE_TYPE.get());
    private final MachineFluidTank tank =
            new MachineFluidTank(
                    TANK_CAPACITY,
                    this::setChanged,
                    resource ->
                            resource.is(
                                    ModFluids.FAMILIES.get(FluidDefinition.AIR).source().get()));
    private int heat;
    private int progress;
    private int progressNeeded = DEFAULT_DURATION;

    public BlastFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(
                ModMachines.entityType(((MachineBlock) state.getBlock()).kind()),
                pos,
                state,
                ((MachineBlock) state.getBlock()).kind().slots());
    }

    @Override
    protected boolean acceptsInventorySlot(int slot, ItemResource resource) {
        if (slot == CELL_IN) return FluidContainerPort.accepts(resource);
        // The machine itself writes spent cells and results into the output slots.
        if (slot == CELL_OUT) return resource.is(ic2.neoforge.registration.ModCells.EMPTY.get());
        return true;
    }

    @Override
    public ResourceHandler<ItemResource> automation(Direction side) {
        return new ResourcePort<>(
                inventory,
                slot -> slot == INPUT || slot == CELL_IN,
                slot -> slot >= OUTPUT_START && slot < OUTPUT_END || slot == CELL_OUT,
                (slot, resource) -> slot != CELL_IN || FluidContainerPort.accepts(resource));
    }

    @Override
    public void serverTick(ServerLevel level) {
        heatUp(level);
        var recipe = recipe(level);
        boolean running = false;
        if (recipe != null && heat >= MAX_HEAT && fits(recipe)) {
            running = true;
            progressNeeded = recipe.duration();
            if (tank.getAmountAsInt(0) >= recipe.fluid()) {
                // The legacy drain is immediate and unchecked; progress counts paid ticks only.
                try (var transaction = Transaction.openRoot()) {
                    tank.extract(0, air(), recipe.fluid(), transaction);
                    transaction.commit();
                }
                progress++;
            }
            if (progress >= recipe.duration() && consumeAndEmit(recipe)) {
                progress = 0;
                setChanged();
            }
        }
        setActive(running);
        gainFluid();
        UpgradeTransfers.tick(level, this);
    }

    private BlastFurnaceRecipe recipe(ServerLevel level) {
        var stack = inventory.stack(INPUT);
        if (stack.isEmpty()) return null;
        return recipes.getRecipeFor(new SingleRecipeInput(stack), level)
                .map(holder -> holder.value())
                .orElse(null);
    }

    /** Legacy drains air and emits only inside a committed tick; returns false to roll back. */
    /** Consumes the input and emits every output atomically; any failure rolls the tick back. */
    private boolean consumeAndEmit(BlastFurnaceRecipe recipe) {
        try (var transaction = Transaction.openRoot()) {
            var input = inventory.stack(INPUT);
            if (inventory.extract(INPUT, ItemResource.of(input), recipe.inputCount(), transaction)
                    != recipe.inputCount()) return false;
            var port =
                    new ResourcePort<>(
                            inventory,
                            slot -> slot >= OUTPUT_START && slot < OUTPUT_END,
                            slot -> false);
            for (var output : recipe.outputs()) {
                if (ResourceHandlerUtil.insertStacking(
                                port, ItemResource.of(output), output.count(), transaction)
                        != output.count()) return false;
            }
            transaction.commit();
            return true;
        }
    }

    private boolean fits(BlastFurnaceRecipe recipe) {
        try (var simulation = Transaction.openRoot()) {
            var port =
                    new ResourcePort<>(
                            inventory,
                            slot -> slot >= OUTPUT_START && slot < OUTPUT_END,
                            slot -> false);
            for (var output : recipe.outputs()) {
                if (ResourceHandlerUtil.insertStacking(
                                port, ItemResource.of(output), output.count(), simulation)
                        != output.count()) return false;
            }
            return true;
        }
    }

    private void heatUp(ServerLevel level) {
        boolean wantsHeat =
                !inventory.stack(INPUT).isEmpty()
                        || progress >= 1
                        || UpgradeItem.invertedSignal(kind(), inventory, level, worldPosition);
        if (!wantsHeat || heat > MAX_HEAT) {
            cool();
            return;
        }
        var facing = getBlockState().getValue(MachineBlock.FACING);
        var target = worldPosition.relative(facing);
        int gained = 0;
        if (level.getChunkSource().hasChunk(target.getX() >> 4, target.getZ() >> 4)) {
            var source =
                    level.getCapability(
                            ic2.neoforge.api.WorkCapabilities.HEAT, target, facing.getOpposite());
            if (source != null) {
                try (var transaction = Transaction.openRoot()) {
                    gained = source.extract(MAX_HEAT - heat + 100, transaction);
                    transaction.commit();
                }
            }
        }
        if (gained == 0) cool();
        else heat += gained;
    }

    private void cool() {
        heat -= Math.min(heat, 1);
    }

    private void gainFluid() {
        if (tank.getAmountAsInt(0) >= TANK_CAPACITY) return;
        ResourceHandlerUtil.move(
                FluidContainerPort.of(inventory, CELL_IN, CELL_OUT),
                tank,
                resource -> resource.is(air().getFluid()),
                1000,
                null);
    }

    private FluidResource air() {
        return FluidResource.of(ModFluids.FAMILIES.get(FluidDefinition.AIR).source().get());
    }

    public int heat() {
        return heat;
    }

    public int tankAmount() {
        return tank.getAmountAsInt(0);
    }

    @Override
    public int menuValue(int index) {
        return switch (index) {
            case 0 -> Float.floatToIntBits((float) heat / MAX_HEAT);
            case 1 -> Float.floatToIntBits((float) tank.getAmountAsInt(0) / TANK_CAPACITY);
            default -> 0;
        };
    }

    @Override
    public int progress() {
        return progress;
    }

    @Override
    public int progressMaximum() {
        return Math.max(1, progressNeeded);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        heat = Math.clamp(input.getIntOr("heat", 0), 0, MAX_HEAT);
        progress = Math.max(0, input.getIntOr("progress", 0));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("heat", heat);
        output.putInt("progress", progress);
    }
}
