package ic2.neoforge.machine;

import ic2.core.machine.CentrifugeHeat;
import ic2.neoforge.recipe.CentrifugeRecipe;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.registration.ModProcessingRecipes;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.item.ItemResource;

import java.util.Optional;
import java.util.OptionalInt;

public final class CentrifugeBlockEntity extends ProcessingBlockEntity {
    private final CentrifugeHeat heat = new CentrifugeHeat();
    private final RecipeManager.CachedCheck<SingleRecipeInput, CentrifugeRecipe> recipes =
            RecipeManager.createCheck(ModProcessingRecipes.CENTRIFUGE_TYPE.get());
    private int requiredHeat;

    public CentrifugeBlockEntity(BlockPos pos, BlockState state) {
        super(ModMachines.entityType(MachineKind.CENTRIFUGE), pos, state);
    }

    public int heat() {
        return heat.heat();
    }

    @Override
    protected boolean outputSlot(int slot) {
        return slot == 1 || slot == 3 || slot == 4;
    }

    private Optional<RecipeHolder<CentrifugeRecipe>> recipe(ServerLevel level) {
        return recipes.getRecipeFor(new SingleRecipeInput(inventory.stack(INPUT)), level);
    }

    private Job job(RecipeHolder<CentrifugeRecipe> recipe) {
        return new Job(
                recipe.id().identifier().toString(),
                recipe.value().inputCount(),
                recipe.value().outputs(),
                0);
    }

    @Override
    protected boolean acceptsInput(ItemResource resource, ServerLevel level) {
        return recipes.getRecipeFor(new SingleRecipeInput(resource.toStack(64)), level).isPresent();
    }

    @Override
    protected Job findJob(ServerLevel level) {
        var recipe = recipe(level).orElse(null);
        if (recipe == null) return null;
        requiredHeat = recipe.value().minHeat();
        return job(recipe);
    }

    @Override
    protected boolean readyToProcess(Job job) {
        return heat.heat() >= requiredHeat;
    }

    @Override
    protected void afterProcessing(ServerLevel level) {
        var recipe = recipe(level).orElse(null);
        var target =
                recipe != null && canFitOutputs(job(recipe))
                        ? OptionalInt.of(recipe.value().minHeat())
                        : OptionalInt.empty();
        int before = heat.heat(), oldTarget = heat.target();
        double stored = energy.stored();
        heat.tick(target, level.hasNeighborSignal(worldPosition), energy);
        if (before != heat.heat() || oldTarget != heat.target() || stored != energy.stored())
            setChanged();
    }

    @Override
    public int menuValue(int index) {
        return switch (index) {
            case 0 -> heat.heat();
            case 1 -> heat.target();
            default -> 0;
        };
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        heat.restore(Math.clamp(input.getIntOr("heat", 0), 0, CentrifugeHeat.MAX_TARGET + 1));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("heat", heat.heat());
    }
}
