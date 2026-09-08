package ic2.neoforge.recipe;

import ic2.neoforge.registration.ModProcessingRecipes;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStackTemplate;

/** Common native recipe contract for fluid conversions that have no crafting-grid placement. */
public interface FluidRecipe extends Recipe<FluidRecipeInput> {
    FluidStackTemplate input();

    FluidStackTemplate result();

    @Override
    default boolean matches(FluidRecipeInput contents, Level level) {
        return contents.fluid().matches(input()) && contents.amount() >= input().amount();
    }

    @Override
    default ItemStack assemble(FluidRecipeInput input) {
        return ItemStack.EMPTY;
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
