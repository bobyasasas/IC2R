package ic2.neoforge.recipe;

import ic2.core.recipe.ProcessingMethod;
import ic2.neoforge.registration.ModCannerRecipes;
import ic2.neoforge.registration.ModProcessingRecipes;
import ic2.neoforge.registration.ModThermalRecipes;

import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;

import java.util.ArrayList;
import java.util.List;

/** Selects the server-side machine recipes that clients need for recipe viewers. */
public final class RecipeSync {
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        event.sendRecipes(recipeTypes());
    }

    static List<RecipeType<?>> recipeTypes() {
        List<RecipeType<?>> types = new ArrayList<>();
        for (ProcessingMethod method : ProcessingMethod.values()) {
            types.add(ModProcessingRecipes.type(method));
        }
        types.add(ModProcessingRecipes.WASHING_TYPE.get());
        types.add(ModProcessingRecipes.CENTRIFUGE_TYPE.get());
        types.add(ModProcessingRecipes.BLAST_FURNACE_TYPE.get());
        types.add(ModProcessingRecipes.MATTER_FABRICATOR_TYPE.get());
        types.add(ModCannerRecipes.SOLID.get());
        types.add(ModCannerRecipes.ENRICH.get());
        types.add(ModThermalRecipes.FERMENTING.get());
        types.add(ModThermalRecipes.COOLING.get());
        types.add(ModThermalRecipes.HEATING.get());
        types.add(ModThermalRecipes.ELECTROLYZING.get());
        return List.copyOf(types);
    }

    private RecipeSync() {}
}
