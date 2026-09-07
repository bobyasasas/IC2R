package ic2.neoforge.registration;

import ic2.neoforge.IndustrialCraft;
import ic2.neoforge.recipe.ElectricCraftingRecipe;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCraftingRecipes {
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, IndustrialCraft.MOD_ID);
    public static final DeferredHolder<
                    RecipeSerializer<?>, RecipeSerializer<ElectricCraftingRecipe>>
            SHAPED =
                    SERIALIZERS.register(
                            "shaped",
                            () ->
                                    new RecipeSerializer<>(
                                            ElectricCraftingRecipe.codec(
                                                    ShapedRecipe.MAP_CODEC, ShapedRecipe.class),
                                            ElectricCraftingRecipe.streamCodec(
                                                    ShapedRecipe.STREAM_CODEC,
                                                    ShapedRecipe.class)));
    public static final DeferredHolder<
                    RecipeSerializer<?>, RecipeSerializer<ElectricCraftingRecipe>>
            SHAPELESS =
                    SERIALIZERS.register(
                            "shapeless",
                            () ->
                                    new RecipeSerializer<>(
                                            ElectricCraftingRecipe.codec(
                                                    ShapelessRecipe.MAP_CODEC,
                                                    ShapelessRecipe.class),
                                            ElectricCraftingRecipe.streamCodec(
                                                    ShapelessRecipe.STREAM_CODEC,
                                                    ShapelessRecipe.class)));

    public static void register(IEventBus bus) {
        SERIALIZERS.register(bus);
    }

    private ModCraftingRecipes() {}
}
