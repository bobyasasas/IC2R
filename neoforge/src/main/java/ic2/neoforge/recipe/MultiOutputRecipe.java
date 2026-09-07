package ic2.neoforge.recipe;

import ic2.neoforge.registration.ModProcessingRecipes;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

import java.util.List;

/** Common presentation and matching for recipes whose products are all emitted together. */
public interface MultiOutputRecipe extends Recipe<SingleRecipeInput> {
    Ingredient ingredient();

    int inputCount();

    List<ItemStackTemplate> outputs();

    @Override
    default boolean matches(SingleRecipeInput input, Level level) {
        return input.item().getCount() >= inputCount() && ingredient().test(input.item());
    }

    @Override
    default ItemStack assemble(SingleRecipeInput input) {
        return outputs().getFirst().create();
    }

    @Override
    default boolean showNotification() {
        return false;
    }

    @Override
    default boolean isSpecial() {
        return true;
    }

    @Override
    default String group() {
        return "";
    }

    @Override
    default PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }

    @Override
    default RecipeBookCategory recipeBookCategory() {
        return ModProcessingRecipes.CATEGORY.get();
    }
}
