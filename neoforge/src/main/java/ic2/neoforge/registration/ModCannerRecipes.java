package ic2.neoforge.registration;

import ic2.neoforge.IndustrialCraft;
import ic2.neoforge.recipe.EnrichingRecipe;
import ic2.neoforge.recipe.SolidCanningRecipe;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCannerRecipes {
    private static final DeferredRegister<RecipeType<?>> TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, IndustrialCraft.MOD_ID);
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, IndustrialCraft.MOD_ID);
    public static final DeferredHolder<RecipeType<?>, RecipeType<SolidCanningRecipe>> SOLID =
            TYPES.register(
                    "canner_bottle",
                    () ->
                            new RecipeType<>() {
                                @Override
                                public String toString() {
                                    return "ic2:canner_bottle";
                                }
                            });
    public static final DeferredHolder<RecipeType<?>, RecipeType<EnrichingRecipe>> ENRICH =
            TYPES.register(
                    "canner_enrich",
                    () ->
                            new RecipeType<>() {
                                @Override
                                public String toString() {
                                    return "ic2:canner_enrich";
                                }
                            });
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<SolidCanningRecipe>>
            SOLID_SERIALIZER =
                    SERIALIZERS.register(
                            "canner_bottle",
                            () ->
                                    new RecipeSerializer<>(
                                            SolidCanningRecipe.CODEC,
                                            SolidCanningRecipe.STREAM_CODEC));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<EnrichingRecipe>>
            ENRICH_SERIALIZER =
                    SERIALIZERS.register(
                            "canner_enrich",
                            () ->
                                    new RecipeSerializer<>(
                                            EnrichingRecipe.CODEC, EnrichingRecipe.STREAM_CODEC));

    public static void register(IEventBus bus) {
        TYPES.register(bus);
        SERIALIZERS.register(bus);
    }

    private ModCannerRecipes() {}
}
