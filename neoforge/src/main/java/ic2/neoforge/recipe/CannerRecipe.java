package ic2.neoforge.recipe;

import ic2.neoforge.registration.ModProcessingRecipes;

import net.minecraft.world.item.crafting.*;

public interface CannerRecipe extends Recipe<CannerInput> {
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
