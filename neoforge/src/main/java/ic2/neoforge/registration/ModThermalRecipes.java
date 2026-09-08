package ic2.neoforge.registration;

import ic2.neoforge.IndustrialCraft;
import ic2.neoforge.recipe.FermentingRecipe;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModThermalRecipes {
    private static final DeferredRegister<RecipeType<?>> TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, IndustrialCraft.MOD_ID);
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, IndustrialCraft.MOD_ID);
    public static final DeferredHolder<RecipeType<?>, RecipeType<FermentingRecipe>> FERMENTING =
            TYPES.register(
                    "fermenting",
                    () ->
                            new RecipeType<>() {
                                @Override
                                public String toString() {
                                    return "ic2:fermenting";
                                }
                            });
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<FermentingRecipe>>
            FERMENTING_SERIALIZER =
                    SERIALIZERS.register(
                            "fermenting",
                            () ->
                                    new RecipeSerializer<>(
                                            FermentingRecipe.CODEC, FermentingRecipe.STREAM_CODEC));

    public static void register(IEventBus bus) {
        TYPES.register(bus);
        SERIALIZERS.register(bus);
    }

    private ModThermalRecipes() {}
}
