package ic2.neoforge.client.interop.jei;

import ic2.core.recipe.ProcessingMethod;
import ic2.neoforge.recipe.ProcessingRecipe;
import ic2.neoforge.registration.ModProcessingRecipes;

import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;
import net.neoforged.neoforge.client.event.RecipesReceivedEvent;

import java.util.List;

/**
 * Snapshot of the client recipe map, taken when the server hands its recipes over.
 * The client Level only exposes RecipeAccess (no full map) and JEI builds its recipe
 * lists inside the same RecipesReceivedEvent dispatch, so the snapshot is always in
 * place before our plugin's registerRecipes runs.
 */
public final class ClientRecipeCache {
    private static volatile RecipeMap recipes = RecipeMap.EMPTY;

    public static void onRecipesReceived(RecipesReceivedEvent event) {
        recipes = event.getRecipeMap();
    }

    public static List<RecipeHolder<ProcessingRecipe>> processing(ProcessingMethod method) {
        RecipeMap map = recipes;
        return List.copyOf(map.byType(ModProcessingRecipes.type(method)));
    }

    private ClientRecipeCache() {}
}
