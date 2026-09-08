package ic2.neoforge.machine;

import ic2.core.machine.FermentationCycle;
import ic2.neoforge.api.WorkCapabilities;
import ic2.neoforge.recipe.FermentingRecipe;
import ic2.neoforge.recipe.FluidRecipeInput;
import ic2.neoforge.registration.MaterialDefinition;
import ic2.neoforge.registration.ModItems;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.registration.ModThermalRecipes;
import ic2.neoforge.transfer.FluidContainerPort;
import ic2.neoforge.transfer.MachineFluidTank;
import ic2.neoforge.transfer.ResourcePort;
import ic2.neoforge.transfer.UpgradeTransfers;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.CombinedResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/** HU, fluid conversion and fertilizer share one transaction. Full outputs never consume heat. */
public final class FermenterBlockEntity extends MachineBlockEntity implements FluidMachine {
    public static final int INPUT_CONTAINER = 0,
            INPUT_RETURN = 1,
            OUTPUT_CONTAINER = 2,
            OUTPUT_RETURN = 3,
            FERTILIZER = 4;
    private final FermentationCycle cycle = new FermentationCycle();
    private final StateJournal<FermentationCycle.State> journal =
            new StateJournal<>(cycle::state, cycle::restore, this::setChanged);
    private final MachineFluidTank inputTank =
            new MachineFluidTank(10000, this::setChanged, this::acceptsFluid);
    private final MachineFluidTank outputTank =
            new MachineFluidTank(2000, this::setChanged, fluid -> true);
    private final ResourceHandler<FluidResource> fluids =
            new ResourcePort<>(
                    new CombinedResourceHandler<>(inputTank, outputTank),
                    index -> index == 0,
                    index -> index == 1);
    private final RecipeManager.CachedCheck<FluidRecipeInput, FermentingRecipe> recipes =
            RecipeManager.createCheck(ModThermalRecipes.FERMENTING.get());
    private int heatRequired = 4000, fertilizerInterval = 500;

    public FermenterBlockEntity(BlockPos pos, BlockState state) {
        super(
                ModMachines.entityType(MachineKind.FERMENTER),
                pos,
                state,
                MachineKind.FERMENTER.slots());
    }

    public MachineFluidTank inputTank() {
        return inputTank;
    }

    public MachineFluidTank outputTank() {
        return outputTank;
    }

    private boolean acceptsFluid(FluidResource resource) {
        return level instanceof ServerLevel server
                && server
                        .recipeAccess()
                        .recipeMap()
                        .byType(ModThermalRecipes.FERMENTING.get())
                        .stream()
                        .anyMatch(holder -> resource.matches(holder.value().input()));
    }

    @Override
    public ResourceHandler<ItemResource> automation(Direction side) {
        return new ResourcePort<>(
                inventory,
                slot ->
                        slot == INPUT_CONTAINER && side == Direction.UP
                                || slot == OUTPUT_CONTAINER && side == Direction.DOWN,
                slot -> slot == INPUT_RETURN || slot == OUTPUT_RETURN || slot == FERTILIZER,
                (slot, resource) -> FluidContainerPort.accepts(resource));
    }

    @Override
    public ResourceHandler<FluidResource> fluidAutomation(Direction side) {
        return fluids;
    }

    @Override
    public void serverTick(ServerLevel level) {
        if (!inventory.stack(INPUT_CONTAINER).isEmpty())
            ResourceHandlerUtil.move(
                    FluidContainerPort.of(inventory, INPUT_CONTAINER, INPUT_RETURN),
                    inputTank,
                    this::acceptsFluid,
                    10000,
                    null);
        if (!inventory.stack(OUTPUT_CONTAINER).isEmpty())
            ResourceHandlerUtil.move(
                    outputTank,
                    FluidContainerPort.of(inventory, OUTPUT_CONTAINER, OUTPUT_RETURN),
                    fluid -> true,
                    2000,
                    null);
        boolean active =
                recipes.getRecipeFor(
                                new FluidRecipeInput(
                                        inputTank.getResource(0), inputTank.getAmountAsInt(0)),
                                level)
                        .map(holder -> process(level, holder.value()))
                        .orElse(false);
        setActive(active);
        UpgradeTransfers.tick(level, this);
    }

    private boolean process(ServerLevel level, FermentingRecipe recipe) {
        var batch = recipe.batch();
        heatRequired = batch.heat();
        fertilizerInterval = batch.fertilizerInterval();
        long fertilizer = cycle.fertilizer(batch);
        if (fertilizer > 64) return false;
        try (var transaction = Transaction.openRoot()) {
            // Probe the full conversion before drawing heat, then reserve only on completion.
            try (var probe = Transaction.open(transaction)) {
                if (!convert(recipe, (int) fertilizer, probe)) return false;
            }
            journal.updateSnapshots(transaction);
            int needed = cycle.neededHeat(batch), drawn = 0;
            if (needed > 0) {
                var facing = getBlockState().getValue(MachineBlock.FACING);
                var sourcePos = worldPosition.relative(facing);
                if (level.getChunkSource().hasChunk(sourcePos.getX() >> 4, sourcePos.getZ() >> 4)) {
                    var source =
                            level.getCapability(
                                    WorkCapabilities.HEAT, sourcePos, facing.getOpposite());
                    if (source != null) {
                        drawn = source.extract(Math.min(100, needed), transaction);
                        if (drawn < 0 || drawn > Math.min(100, needed)) return false;
                        cycle.addHeat(drawn, batch);
                    }
                }
            }
            boolean finished = cycle.neededHeat(batch) == 0;
            if (finished
                    && (!convert(recipe, (int) fertilizer, transaction) || !cycle.complete(batch)))
                return false;
            transaction.commit();
            return drawn > 0 || finished;
        }
    }

    private boolean convert(
            FermentingRecipe recipe, int fertilizer, TransactionContext transaction) {
        return inputTank.extract(0, inputTank.getResource(0), recipe.input().amount(), transaction)
                        == recipe.input().amount()
                && outputTank.insert(
                                0,
                                FluidResource.of(recipe.result()),
                                recipe.result().amount(),
                                transaction)
                        == recipe.result().amount()
                && (fertilizer == 0
                        || inventory.insert(
                                        FERTILIZER,
                                        ItemResource.of(
                                                ModItems.MATERIALS
                                                        .get(MaterialDefinition.FERTILIZER)
                                                        .get()),
                                        fertilizer,
                                        transaction)
                                == fertilizer);
    }

    @Override
    public int progress() {
        return cycle.state().processed();
    }

    @Override
    public int progressMaximum() {
        return fertilizerInterval;
    }

    @Override
    public int fuelRemaining() {
        return cycle.state().heat();
    }

    @Override
    public int fuelMaximum() {
        return heatRequired;
    }

    @Override
    public int menuValue(int index) {
        return switch (index) {
            case 1 -> inputTank.getAmountAsInt(0);
            case 2 -> outputTank.getAmountAsInt(0);
            case 3 -> BuiltInRegistries.FLUID.getId(inputTank.getResource(0).getFluid());
            case 4 -> BuiltInRegistries.FLUID.getId(outputTank.getResource(0).getFluid());
            default -> 0;
        };
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        inputTank.deserialize(input.childOrEmpty("inputTank"));
        outputTank.deserialize(input.childOrEmpty("outputTank"));
        cycle.restore(
                new FermentationCycle.State(
                        Math.max(0, input.getIntOr("heat", 0)),
                        Math.max(0, input.getIntOr("processed", 0))));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        inputTank.serialize(output.child("inputTank"));
        outputTank.serialize(output.child("outputTank"));
        output.putInt("heat", cycle.state().heat());
        output.putInt("processed", cycle.state().processed());
    }
}
