package ic2.neoforge.registration;

import ic2.core.recipe.ProcessingMethod;
import ic2.neoforge.IndustrialCraft;
import ic2.neoforge.recipe.CentrifugeRecipe;
import ic2.neoforge.recipe.ProcessingRecipe;
import ic2.neoforge.recipe.WashingRecipe;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.Map;

public final class ModProcessingRecipes {
    private static final DeferredRegister<RecipeType<?>> TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, IndustrialCraft.MOD_ID);
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, IndustrialCraft.MOD_ID);
    private static final DeferredRegister<RecipeBookCategory> CATEGORIES =
            DeferredRegister.create(Registries.RECIPE_BOOK_CATEGORY, IndustrialCraft.MOD_ID);
    public static final DeferredHolder<RecipeBookCategory, RecipeBookCategory> CATEGORY =
            CATEGORIES.register("processing", RecipeBookCategory::new);
    public static final DeferredHolder<RecipeType<?>, RecipeType<WashingRecipe>> WASHING_TYPE =
            TYPES.register(
                    "ore_washer",
                    () ->
                            new RecipeType<WashingRecipe>() {
                                @Override
                                public String toString() {
                                    return "ic2:ore_washer";
                                }
                            });
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<WashingRecipe>>
            WASHING_SERIALIZER =
                    SERIALIZERS.register(
                            "ore_washer",
                            () ->
                                    new RecipeSerializer<>(
                                            WashingRecipe.CODEC, WashingRecipe.STREAM_CODEC));
    public static final DeferredHolder<RecipeType<?>, RecipeType<CentrifugeRecipe>>
            CENTRIFUGE_TYPE =
                    TYPES.register(
                            "centrifuge",
                            () ->
                                    new RecipeType<CentrifugeRecipe>() {
                                        @Override
                                        public String toString() {
                                            return "ic2:centrifuge";
                                        }
                                    });
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CentrifugeRecipe>>
            CENTRIFUGE_SERIALIZER =
                    SERIALIZERS.register(
                            "centrifuge",
                            () ->
                                    new RecipeSerializer<>(
                                            CentrifugeRecipe.CODEC, CentrifugeRecipe.STREAM_CODEC));
    private static final Map<
                    ProcessingMethod, DeferredHolder<RecipeType<?>, RecipeType<ProcessingRecipe>>>
            RECIPE_TYPES = new EnumMap<>(ProcessingMethod.class);
    private static final Map<
                    ProcessingMethod,
                    DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ProcessingRecipe>>>
            RECIPE_SERIALIZERS = new EnumMap<>(ProcessingMethod.class);

    static {
        for (var kind : ProcessingMethod.values()) {
            RECIPE_TYPES.put(
                    kind,
                    TYPES.register(
                            kind.id(),
                            () ->
                                    new RecipeType<ProcessingRecipe>() {
                                        @Override
                                        public String toString() {
                                            return "ic2:" + kind.id();
                                        }
                                    }));
            RECIPE_SERIALIZERS.put(
                    kind,
                    SERIALIZERS.register(
                            kind.id(),
                            () ->
                                    new RecipeSerializer<>(
                                            ProcessingRecipe.codec(kind),
                                            ProcessingRecipe.streamCodec(kind))));
        }
    }

    public static RecipeType<ProcessingRecipe> type(ProcessingMethod kind) {
        return RECIPE_TYPES.get(kind).get();
    }

    public static RecipeSerializer<ProcessingRecipe> serializer(ProcessingMethod kind) {
        return RECIPE_SERIALIZERS.get(kind).get();
    }

    public static void register(IEventBus bus) {
        TYPES.register(bus);
        SERIALIZERS.register(bus);
        CATEGORIES.register(bus);
    }

    private ModProcessingRecipes() {}
}
