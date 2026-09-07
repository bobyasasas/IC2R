package ic2.neoforge.machine;

import ic2.neoforge.recipe.ProcessingRecipe;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.registration.ModProcessingRecipes;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Weighted results are selected once and saved, including while output is blocked. */
public final class SingleInputBlockEntity extends ProcessingBlockEntity {
    private final RecipeManager.CachedCheck<SingleRecipeInput, ProcessingRecipe> recipes;
    private String selectedRecipe = "";
    private ItemStackTemplate selectedOutput;

    public SingleInputBlockEntity(BlockPos pos, BlockState state) {
        super(ModMachines.entityType(((MachineBlock) state.getBlock()).kind()), pos, state);
        recipes = RecipeManager.createCheck(ModProcessingRecipes.type(kind()));
    }

    @Override
    protected Job findJob(ServerLevel level) {
        var recipe =
                recipes.getRecipeFor(new SingleRecipeInput(inventory.stack(INPUT)), level)
                        .orElse(null);
        if (recipe == null) {
            clearSelection();
            return null;
        }
        String id = recipe.id().identifier().toString();
        boolean validOutput =
                selectedOutput != null
                        && recipe.value().outputs().stream()
                                .anyMatch(output -> output.stack().equals(selectedOutput));
        if (!id.equals(selectedRecipe) || !validOutput) {
            selectedRecipe = id;
            selectedOutput = recipe.value().chooseOutput(level.getRandom());
            setChanged();
        }
        return new Job(id, recipe.value().inputCount(), selectedOutput, 0);
    }

    @Override
    protected void completed() {
        clearSelection();
    }

    private void clearSelection() {
        if (selectedOutput != null || !selectedRecipe.isEmpty()) {
            selectedOutput = null;
            selectedRecipe = "";
            setChanged();
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        selectedRecipe = input.getStringOr("selectedRecipe", "");
        selectedOutput = input.read("selectedOutput", ItemStackTemplate.CODEC).orElse(null);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (selectedOutput != null) {
            output.putString("selectedRecipe", selectedRecipe);
            output.store("selectedOutput", ItemStackTemplate.CODEC, selectedOutput);
        }
    }
}
