package ic2.neoforge.machine;

import ic2.neoforge.fluid.FluidDefinition;
import ic2.neoforge.registration.ModFluids;
import ic2.neoforge.registration.ModItems;
import ic2.neoforge.registration.ModMaterialBlocks;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.transfer.ResourcePort;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import org.jspecify.annotations.Nullable;

/**
 * Controller of the 3x3x3 coke kiln: input drops through the hatch above the firebox, creosote
 * collects in the grate below. Validation and work run every 20 ticks like the legacy kiln.
 */
public final class CokeKilnBlockEntity extends MachineBlockEntity {
    public static final int TICK_RATE = 20;
    /** Legacy TileEntityCokeKiln: coal chars to coke with a strong creosote byproduct. */
    private static final CokeRecipe COAL_RECIPE =
            new CokeRecipe(Items.COAL, new ItemStack(ModItems.COKE.get()), creosote(500), 1800,
                    false);
    /** A null input item with matchLogs set chars any log tag member into charcoal. */
    private static final CokeRecipe LOG_RECIPE =
            new CokeRecipe(null, new ItemStack(Items.CHARCOAL), creosote(250), 1800, true);
    private static final CokeRecipe[] RECIPES = {COAL_RECIPE, LOG_RECIPE};

    private final int updateTicker = RandomSource.create().nextInt(TICK_RATE);
    private int tickPhase;
    private int progress;
    private int operationLength;
    private @Nullable CokeRecipe currentRecipe;

    private record CokeRecipe(
            @Nullable Item inputItem,
            ItemStack outputItem,
            FluidStack outputFluid,
            int operationDuration,
            boolean matchLogs) {
        boolean matches(ItemStack stack) {
            if (stack.isEmpty()) return false;
            if (matchLogs) return stack.is(ItemTags.LOGS);
            return inputItem != null && stack.is(inputItem);
        }
    }

    public CokeKilnBlockEntity(BlockPos pos, BlockState state) {
        super(ModMachines.entityType(MachineKind.COKE_KILN), pos, state, 1);
    }

    private static FluidStack creosote(int amount) {
        return new FluidStack(
                ModFluids.FAMILIES.get(FluidDefinition.CREOSOTE).source().get(), amount);
    }

    private BlockPos hatchPos() {
        Direction facing = getBlockState().getValue(MachineBlock.FACING);
        return new BlockPos(
                worldPosition.getX() - facing.getStepX(),
                worldPosition.getY() + 1,
                worldPosition.getZ() - facing.getStepZ());
    }

    private BlockPos gratePos() {
        Direction facing = getBlockState().getValue(MachineBlock.FACING);
        return new BlockPos(
                worldPosition.getX() - facing.getStepX(),
                worldPosition.getY() - 1,
                worldPosition.getZ() - facing.getStepZ());
    }

    /** The kiln interior offset one block against the facing, shared by all three layers. */
    private BlockPos ringPos(int x, int y, int z) {
        Direction facing = getBlockState().getValue(MachineBlock.FACING);
        return new BlockPos(
                worldPosition.getX() + x - facing.getStepX(),
                worldPosition.getY() + y,
                worldPosition.getZ() + z - facing.getStepZ());
    }

    public boolean hasValidStructure(ServerLevel level) {
        // Bottom layer (y-1): 3x3 refractory bricks, centre = grate.
        for (int x = -1; x <= 1; x++)
            for (int z = -1; z <= 1; z++) {
                BlockPos cPos = ringPos(x, -1, z);
                if (x == 0 && z == 0) {
                    if (!(level.getBlockEntity(cPos) instanceof CokeKilnGrateBlockEntity))
                        return false;
                } else if (!level.getBlockState(cPos)
                        .is(ModMaterialBlocks.REFRACTORY_BRICKS.get())) return false;
            }
        // Middle layer (y0): 3x3 refractory bricks around a hollow centre, this block on one edge.
        for (int x = -1; x <= 1; x++)
            for (int z = -1; z <= 1; z++) {
                BlockPos cPos = ringPos(x, 0, z);
                if (x == 0 && z == 0) {
                    if (!level.getBlockState(cPos).isAir()) return false;
                } else if (cPos.equals(worldPosition)) {
                    if (level.getBlockEntity(cPos) != this) return false;
                } else if (!level.getBlockState(cPos)
                        .is(ModMaterialBlocks.REFRACTORY_BRICKS.get())) return false;
            }
        // Top layer (y+1): 3x3 refractory bricks, centre = hatch.
        for (int x = -1; x <= 1; x++)
            for (int z = -1; z <= 1; z++) {
                BlockPos cPos = ringPos(x, 1, z);
                if (x == 0 && z == 0) {
                    if (!(level.getBlockEntity(cPos) instanceof CokeKilnHatchBlockEntity))
                        return false;
                } else if (!level.getBlockState(cPos)
                        .is(ModMaterialBlocks.REFRACTORY_BRICKS.get())) return false;
            }
        return true;
    }

    private boolean canWork(ServerLevel level) {
        if (!(level.getBlockEntity(hatchPos()) instanceof CokeKilnHatchBlockEntity hatch))
            return false;
        ItemStack input = hatch.input();
        if (input.isEmpty()) return false;
        if (currentRecipe != null && !currentRecipe.matches(input)) reset();
        CokeRecipe recipe = currentRecipe;
        if (recipe == null) {
            recipe = findRecipe(input);
            if (recipe == null) return false;
            currentRecipe = recipe;
            operationLength = recipe.operationDuration;
        }
        try (Transaction transaction = Transaction.openRoot()) {
            if (inventory.insert(
                            0,
                            ItemResource.of(recipe.outputItem),
                            recipe.outputItem.getCount(),
                            transaction)
                    != recipe.outputItem.getCount()) return false;
            if (!(level.getBlockEntity(gratePos()) instanceof CokeKilnGrateBlockEntity grate))
                return false;
            FluidResource fluid = FluidResource.of(recipe.outputFluid);
            if (grate.tank().insert(0, fluid, recipe.outputFluid.getAmount(), transaction)
                    != recipe.outputFluid.getAmount()) return false;
        }
        return true;
    }

    private CokeRecipe findRecipe(ItemStack input) {
        for (CokeRecipe recipe : RECIPES) if (recipe.matches(input)) return recipe;
        return null;
    }

    private void finishWork(ServerLevel level) {
        if (!(level.getBlockEntity(hatchPos()) instanceof CokeKilnHatchBlockEntity hatch)
                || hatch.input().isEmpty() || currentRecipe == null) return;
        hatch.consumeInput(1);
        try (Transaction transaction = Transaction.openRoot()) {
            inventory.insert(
                    0,
                    ItemResource.of(currentRecipe.outputItem),
                    currentRecipe.outputItem.getCount(),
                    transaction);
            if (level.getBlockEntity(gratePos()) instanceof CokeKilnGrateBlockEntity grate)
                grate.tank()
                        .insert(
                                0,
                                FluidResource.of(currentRecipe.outputFluid),
                                currentRecipe.outputFluid.getAmount(),
                                transaction);
            transaction.commit();
        }
        progress = 0;
    }

    private void reset() {
        progress = 0;
        operationLength = 0;
        currentRecipe = null;
    }

    @Override
    public ResourceHandler<ItemResource> automation(Direction side) {
        // Only the output slot exists and it accepts nothing; every face may extract.
        return new ResourcePort<>(inventory, slot -> false, slot -> true);
    }

    @Override
    public void serverTick(ServerLevel level) {
        // One validation/production pass every 20 ticks at a per-kiln random phase.
        if (tickPhase++ % TICK_RATE != updateTicker % TICK_RATE) return;
        if (!hasValidStructure(level)) {
            reset();
            setActive(false);
            return;
        }
        if (canWork(level)) {
            setActive(true);
            progress += TICK_RATE;
            if (progress >= operationLength) {
                finishWork(level);
                setChanged();
            }
        } else {
            setActive(false);
        }
    }

    @Override
    public int progress() {
        return progress;
    }

    @Override
    public int progressMaximum() {
        return operationLength;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        // currentRecipe is re-derived on the next canWork, exactly like the legacy kiln.
        progress = Math.max(0, input.getIntOr("progress", 0));
        operationLength = Math.max(0, input.getIntOr("operationLength", 0));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("progress", progress);
        output.putInt("operationLength", operationLength);
    }
}
