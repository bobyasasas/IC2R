package ic2.neoforge.machine;

import ic2.core.machine.WorkBuffer;
import ic2.neoforge.api.WorkSource;
import ic2.neoforge.recipe.FluidRecipeInput;
import ic2.neoforge.recipe.HeatingRecipe;
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

import org.jspecify.annotations.Nullable;

/**
 * Heats pumpable fluids with pulled HU and banks the yield as KU: four HU buy twelve KU and
 * whole millibuckets only move once their heat is fully paid.
 */
public final class StirlingKineticGeneratorBlockEntity extends MachineBlockEntity
        implements FluidMachine {
    public static final int TANK_CAPACITY = 2000, HEAT_CAPACITY = 1000, KU_CAPACITY = 2000;
    private static final int HEAT_PER_KU_BATCH = 4;
    private int heat, liquidHeatStored;
    // Legacy lets one tick's conversion overshoot maxKuBuffer (1000 HU -> 3000 KU); only the
    // per-tick extraction budget is capped.
    private final WorkBuffer kinetic = new WorkBuffer(Integer.MAX_VALUE);

    private record Snapshot(int heat, WorkBuffer.State kinetic, int liquidHeatStored) {}

    private final StateJournal<Snapshot> journal =
            new StateJournal<>(
                    () -> new Snapshot(heat, kinetic.state(), liquidHeatStored),
                    this::restore,
                    this::setChanged);
    private final MachineFluidTank inputTank =
            new MachineFluidTank(TANK_CAPACITY, this::setChanged, this::acceptsFluid);
    private final MachineFluidTank outputTank =
            new MachineFluidTank(TANK_CAPACITY, this::setChanged, fluid -> true);
    private final ResourceHandler<FluidResource> fluids =
            new ResourcePort<>(
                    new CombinedResourceHandler<>(inputTank, outputTank),
                    index -> index == 0,
                    index -> index == 1);
    private final RecipeManager.CachedCheck<FluidRecipeInput, HeatingRecipe> recipes =
            RecipeManager.createCheck(ModThermalRecipes.HEATING.get());

    public StirlingKineticGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(
                ModMachines.entityType(MachineKind.STIRLING_KINETIC_GENERATOR),
                pos,
                state,
                MachineKind.STIRLING_KINETIC_GENERATOR.slots());
    }

    private void restore(Snapshot snapshot) {
        heat = snapshot.heat();
        kinetic.restore(snapshot.kinetic());
        liquidHeatStored = snapshot.liquidHeatStored();
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
                        .byType(ModThermalRecipes.HEATING.get())
                        .stream()
                        .anyMatch(holder -> resource.matches(holder.value().input()));
    }

    @Override
    public ResourceHandler<ItemResource> automation(Direction side) {
        return new ResourcePort<>(
                inventory,
                slot -> slot == 0 && side == Direction.UP || slot == 2 && side == Direction.DOWN,
                slot -> slot == 1 || slot == 3,
                (slot, resource) -> FluidContainerPort.accepts(resource));
    }

    @Override
    public ResourceHandler<FluidResource> fluidAutomation(Direction side) {
        return fluids;
    }

    /** KU leaves only through the facing; the pull side for HU is every other face. */
    public WorkSource output(@Nullable Direction side) {
        return new WorkOutput(this, side, kinetic, () -> KU_CAPACITY, journal::updateSnapshots);
    }

    @Override
    public void serverTick(ServerLevel level) {
        if (!inventory.stack(0).isEmpty())
            ResourceHandlerUtil.move(
                    FluidContainerPort.of(inventory, 0, 1),
                    inputTank,
                    this::acceptsFluid,
                    TANK_CAPACITY,
                    null);
        if (!inventory.stack(2).isEmpty())
            ResourceHandlerUtil.move(
                    outputTank,
                    FluidContainerPort.of(inventory, 2, 3),
                    resource -> true,
                    TANK_CAPACITY,
                    null);
        drawHeat(level);
        setActive(convert(level));
        UpgradeTransfers.tick(level, this);
    }

    private void drawHeat(ServerLevel level) {
        if (heat >= HEAT_CAPACITY) return;
        var facing = getBlockState().getValue(MachineBlock.FACING);
        for (var side : Direction.values()) {
            if (side == facing) continue;
            var pos = worldPosition.relative(side);
            if (!level.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) continue;
            var source =
                    level.getCapability(ic2.neoforge.api.WorkCapabilities.HEAT, pos, side.getOpposite());
            if (source == null) continue;
            int request = (int) Math.min(HEAT_CAPACITY - heat, source.available());
            if (request <= 0) continue;
            try (var transaction = Transaction.openRoot()) {
                journal.updateSnapshots(transaction);
                int drawn = source.extract(request, transaction);
                if (drawn <= 0) continue;
                heat += drawn;
                transaction.commit();
            }
            if (heat >= HEAT_CAPACITY) break;
        }
    }

    private boolean convert(ServerLevel level) {
        var holder =
                recipes.getRecipeFor(
                                new FluidRecipeInput(
                                        inputTank.getResource(0), inputTank.getAmountAsInt(0)),
                                level)
                        .orElse(null);
        return holder != null && convert(holder.value());
    }

    private boolean convert(HeatingRecipe recipe) {
        if (kinetic.state().stored() >= KU_CAPACITY || inputTank.getAmountAsInt(0) <= 0)
            return false;
        var output = FluidResource.of(recipe.result());
        if (!outputTank.getResource(0).isEmpty() && !outputTank.getResource(0).equals(output))
            return false;
        int batch =
                Math.min(
                        heat / HEAT_PER_KU_BATCH,
                        (KU_CAPACITY - (int) kinetic.state().stored()) / 3);
        batch =
                Math.min(
                        batch,
                        Math.min(
                                        TANK_CAPACITY - outputTank.getAmountAsInt(0),
                                        inputTank.getAmountAsInt(0))
                                        * recipe.heat()
                                - liquidHeatStored);
        if (batch <= 0) return false;
        int units = batch * 3 * HEAT_PER_KU_BATCH;
        try (var transaction = Transaction.openRoot()) {
            journal.updateSnapshots(transaction);
            if (kinetic.insert(units, Integer.MAX_VALUE) != units) return false;
            heat -= batch * HEAT_PER_KU_BATCH;
            liquidHeatStored += batch;
            if (liquidHeatStored >= recipe.heat()) {
                int millibuckets = liquidHeatStored / recipe.heat();
                if (inputTank.extract(0, inputTank.getResource(0), millibuckets, transaction)
                                != millibuckets
                        || outputTank.insert(0, output, millibuckets, transaction)
                                != millibuckets) return false;
                liquidHeatStored -= millibuckets * recipe.heat();
            }
            transaction.commit();
            return true;
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
    public int fuelRemaining() {
        return (int) kinetic.state().stored();
    }

    @Override
    public int fuelMaximum() {
        return KU_CAPACITY;
    }

    @Override
    public int menuValue(int index) {
        return switch (index) {
            case 0 -> (int) kinetic.state().stored();
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
        heat = Math.clamp(input.getIntOr("heat", 0), 0, HEAT_CAPACITY);
        liquidHeatStored = Math.max(0, input.getIntOr("liquidHeatStored", 0));
        kinetic.restore(
                new WorkBuffer.State(
                        Math.max(0, input.getIntOr("kinetic", 0)),
                        input.getLongOr("kineticTick", Long.MIN_VALUE),
                        Math.clamp(input.getIntOr("kineticExtracted", 0), 0, KU_CAPACITY)));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        inputTank.serialize(output.child("inputTank"));
        outputTank.serialize(output.child("outputTank"));
        output.putInt("heat", heat);
        output.putInt("liquidHeatStored", liquidHeatStored);
        output.putInt("kinetic", (int) kinetic.state().stored());
        output.putLong("kineticTick", kinetic.state().tick());
        output.putInt("kineticExtracted", kinetic.state().extracted());
    }
}
