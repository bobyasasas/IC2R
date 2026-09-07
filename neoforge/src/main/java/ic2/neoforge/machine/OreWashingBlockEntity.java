package ic2.neoforge.machine;

import ic2.neoforge.recipe.WashingRecipe;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.registration.ModProcessingRecipes;
import ic2.neoforge.transfer.MachineFluidTank;
import ic2.neoforge.transfer.ResourcePort;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

public final class OreWashingBlockEntity extends ProcessingBlockEntity implements FluidMachine {
    public static final int WATER_INPUT = 5, CONTAINER_OUTPUT = 6;
    private static final FluidResource WATER = FluidResource.of(Fluids.WATER);
    private final MachineFluidTank tank =
            new MachineFluidTank(8000, this::setChanged, WATER::equals);
    private final RecipeManager.CachedCheck<SingleRecipeInput, WashingRecipe> recipes =
            RecipeManager.createCheck(ModProcessingRecipes.WASHING_TYPE.get());
    private int selectedWater;

    public OreWashingBlockEntity(BlockPos pos, BlockState state) {
        super(ModMachines.entityType(MachineKind.ORE_WASHING_PLANT), pos, state);
    }

    public MachineFluidTank tank() {
        return tank;
    }

    @Override
    public ResourceHandler<FluidResource> fluidAutomation(Direction side) {
        return new ResourcePort<>(tank, slot -> true, slot -> false);
    }

    @Override
    public ResourceHandler<ItemResource> automation(Direction side) {
        return new ResourcePort<>(
                inventory,
                slot -> side != Direction.DOWN && (slot == INPUT || slot == WATER_INPUT),
                slot -> outputSlot(slot) || slot == CONTAINER_OUTPUT,
                (slot, resource) ->
                        slot == INPUT
                                ? level instanceof ServerLevel server
                                        && acceptsInput(resource, server)
                                : ItemAccess.forStack(resource.toStack())
                                                .getCapability(Capabilities.Fluid.ITEM)
                                        != null);
    }

    @Override
    protected boolean outputSlot(int slot) {
        return slot == 1 || slot == 3 || slot == 4;
    }

    @Override
    protected boolean acceptsInput(ItemResource resource, ServerLevel level) {
        return recipes.getRecipeFor(new SingleRecipeInput(resource.toStack(64)), level).isPresent();
    }

    @Override
    protected Job findJob(ServerLevel level) {
        var recipe =
                recipes.getRecipeFor(new SingleRecipeInput(inventory.stack(INPUT)), level)
                        .orElse(null);
        if (recipe == null
                || !tank.getResource(0).equals(WATER)
                || tank.getAmountAsInt(0) < recipe.value().water()) return null;
        selectedWater = recipe.value().water();
        return new Job(
                recipe.id().identifier().toString(),
                recipe.value().inputCount(),
                recipe.value().outputs(),
                0);
    }

    @Override
    protected boolean consumeInputs(Job job, ItemResource input, Transaction transaction) {
        return super.consumeInputs(job, input, transaction)
                && tank.extract(0, WATER, selectedWater, transaction) == selectedWater;
    }

    @Override
    protected void afterProcessing(ServerLevel level) {
        if (inventory.stack(WATER_INPUT).isEmpty()) return;
        var port =
                new ResourcePort<>(
                        inventory, slot -> slot == CONTAINER_OUTPUT, slot -> slot == WATER_INPUT);
        var container =
                ItemAccess.forHandlerIndex(port, WATER_INPUT)
                        .oneByOne()
                        .getCapability(Capabilities.Fluid.ITEM);
        ResourceHandlerUtil.move(container, tank, WATER::equals, 8000, null);
    }

    @Override
    public int menuValue(int index) {
        return switch (index) {
            case 0 -> tank.getAmountAsInt(0);
            case 1 -> BuiltInRegistries.FLUID.getId(tank.getResource(0).getFluid());
            case 2 -> 8000;
            default -> 0;
        };
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        tank.deserialize(input.childOrEmpty("waterTank"));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        tank.serialize(output.child("waterTank"));
    }
}
