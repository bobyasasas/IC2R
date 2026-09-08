package ic2.neoforge.machine;

import ic2.core.machine.HeatExchange;
import ic2.core.machine.WorkBuffer;
import ic2.neoforge.api.WorkSource;
import ic2.neoforge.recipe.CoolingRecipe;
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

import org.jspecify.annotations.Nullable;

/** Whole-mB cooling pays into the shared heat buffer only when both fluid moves succeed. */
public final class LiquidHeatExchangerBlockEntity extends MachineBlockEntity
        implements FluidMachine {
    public static final int PART_START = 4, PART_END = 14;
    private final WorkBuffer work = new WorkBuffer(100);
    private final StateJournal<WorkBuffer.State> journal =
            new StateJournal<>(work::state, work::restore, this::setChanged);
    private final MachineFluidTank inputTank =
            new MachineFluidTank(2000, this::setChanged, this::acceptsFluid);
    private final MachineFluidTank outputTank =
            new MachineFluidTank(2000, this::setChanged, fluid -> true);
    private final ResourceHandler<FluidResource> fluids =
            new ResourcePort<>(
                    new CombinedResourceHandler<>(inputTank, outputTank),
                    index -> index == 0,
                    index -> index == 1);
    private final RecipeManager.CachedCheck<FluidRecipeInput, CoolingRecipe> recipes =
            RecipeManager.createCheck(ModThermalRecipes.COOLING.get());

    public LiquidHeatExchangerBlockEntity(BlockPos pos, BlockState state) {
        super(
                ModMachines.entityType(MachineKind.LIQUID_HEAT_EXCHANGER),
                pos,
                state,
                MachineKind.LIQUID_HEAT_EXCHANGER.slots());
    }

    public MachineFluidTank inputTank() {
        return inputTank;
    }

    public MachineFluidTank outputTank() {
        return outputTank;
    }

    public static boolean conductor(ItemResource resource) {
        return resource.getItem()
                == ModItems.MATERIALS.get(MaterialDefinition.HEAT_CONDUCTOR).get();
    }

    @Override
    protected boolean acceptsInventorySlot(int slot, ItemResource resource) {
        return slot >= PART_START && slot < PART_END
                ? conductor(resource)
                : super.acceptsInventorySlot(slot, resource);
    }

    @Override
    protected int inventorySlotLimit(int slot) {
        return slot >= PART_START && slot < PART_END ? 1 : super.inventorySlotLimit(slot);
    }

    private int bandwidth() {
        int count = 0;
        for (int slot = PART_START; slot < PART_END; slot++)
            if (conductor(inventory.getResource(slot))) count++;
        return count * 10;
    }

    public WorkSource output(@Nullable Direction side) {
        return new WorkOutput(this, side, work, this::bandwidth, journal::updateSnapshots);
    }

    private boolean acceptsFluid(FluidResource resource) {
        return level instanceof ServerLevel server
                && server
                        .recipeAccess()
                        .recipeMap()
                        .byType(ModThermalRecipes.COOLING.get())
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

    @Override
    public void serverTick(ServerLevel level) {
        if (!inventory.stack(0).isEmpty())
            ResourceHandlerUtil.move(
                    FluidContainerPort.of(inventory, 0, 1),
                    inputTank,
                    this::acceptsFluid,
                    2000,
                    null);
        if (!inventory.stack(2).isEmpty())
            ResourceHandlerUtil.move(
                    outputTank,
                    FluidContainerPort.of(inventory, 2, 3),
                    resource -> true,
                    2000,
                    null);
        recipes.getRecipeFor(
                        new FluidRecipeInput(inputTank.getResource(0), inputTank.getAmountAsInt(0)),
                        level)
                .ifPresent(holder -> cool(holder.value()));
        setActive(work.state().stored() > 0);
        UpgradeTransfers.tick(level, this);
    }

    private void cool(CoolingRecipe recipe) {
        var output = FluidResource.of(recipe.result());
        if (!outputTank.getResource(0).isEmpty() && !outputTank.getResource(0).equals(output))
            return;
        int target = bandwidth();
        var exchange =
                HeatExchange.plan(
                        inputTank.getAmountAsInt(0),
                        2000 - outputTank.getAmountAsInt(0),
                        Math.max(0, target - (int) work.state().stored()),
                        recipe.heat());
        if (exchange.amount() == 0) return;
        try (var transaction = Transaction.openRoot()) {
            journal.updateSnapshots(transaction);
            if (inputTank.extract(0, inputTank.getResource(0), exchange.amount(), transaction)
                            != exchange.amount()
                    || outputTank.insert(0, output, exchange.amount(), transaction)
                            != exchange.amount()
                    || work.insert(exchange.heat(), target) != exchange.heat()) return;
            transaction.commit();
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
        return (int) work.state().stored();
    }

    @Override
    public int fuelMaximum() {
        return bandwidth();
    }

    @Override
    public int menuValue(int index) {
        return switch (index) {
            case 0 -> bandwidth();
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
        work.restore(
                new WorkBuffer.State(
                        Math.clamp(input.getIntOr("heat", 0), 0, 100),
                        input.getLongOr("heatTick", Long.MIN_VALUE),
                        Math.clamp(input.getIntOr("heatExtracted", 0), 0, 100)));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        inputTank.serialize(output.child("inputTank"));
        outputTank.serialize(output.child("outputTank"));
        output.putInt("heat", (int) work.state().stored());
        output.putLong("heatTick", work.state().tick());
        output.putInt("heatExtracted", work.state().extracted());
    }
}
