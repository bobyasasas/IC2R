package ic2.neoforge.client.interop.jei;

import ic2.core.recipe.ProcessingMethod;
import ic2.neoforge.IndustrialCraft;
import ic2.neoforge.recipe.ProcessingRecipe;
import ic2.neoforge.registration.ModProcessingRecipes;

import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RecipesReceivedEvent;

import java.util.List;

/**
 * Snapshot of the client recipe map, taken when the server hands its recipes over. The client Level
 * only exposes RecipeAccess (no full map) and JEI builds its recipe lists inside the same
 * RecipesReceivedEvent dispatch, so the snapshot is always in place before our plugin's
 * registerRecipes runs.
 */
public final class ClientRecipeCache {
    private static volatile RecipeMap recipes = RecipeMap.EMPTY;

    public static void onRecipesReceived(RecipesReceivedEvent event) {
        recipes = event.getRecipeMap();
        long ic2Recipes =
                recipes.values().stream()
                        .filter(
                                holder ->
                                        holder.id()
                                                .identifier()
                                                .getNamespace()
                                                .equals(IndustrialCraft.MOD_ID))
                        .count();
        com.mojang.logging.LogUtils.getLogger()
                .info(
                        "IC2 captured {} synchronized recipes ({} IC2) before JEI registration",
                        recipes.values().size(),
                        ic2Recipes);
    }

    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        recipes = RecipeMap.EMPTY;
    }

    public static List<RecipeHolder<ProcessingRecipe>> processing(ProcessingMethod method) {
        RecipeMap map = recipes;
        return List.copyOf(map.byType(ModProcessingRecipes.type(method)));
    }

    public static <I extends RecipeInput, T extends Recipe<I>> List<RecipeHolder<T>> byType(
            RecipeType<T> type) {
        RecipeMap map = recipes;
        return List.copyOf(map.byType(type));
    }

    private ClientRecipeCache() {}
}
