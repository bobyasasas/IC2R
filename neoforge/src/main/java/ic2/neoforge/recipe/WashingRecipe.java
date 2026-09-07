package ic2.neoforge.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import ic2.neoforge.registration.ModProcessingRecipes;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Objects;

/** Every result is produced together; water is consumed in the same completion transaction. */
public record WashingRecipe(
        Ingredient ingredient, int inputCount, List<ItemStackTemplate> outputs, int water)
        implements Recipe<SingleRecipeInput> {
    public static final MapCodec<WashingRecipe> CODEC =
            RecordCodecBuilder.mapCodec(
                    instance ->
                            instance.group(
                                            Ingredient.CODEC
                                                    .fieldOf("ingredient")
                                                    .forGetter(WashingRecipe::ingredient),
                                            Codec.intRange(1, 64)
                                                    .optionalFieldOf("input_count", 1)
                                                    .forGetter(WashingRecipe::inputCount),
                                            ItemStackTemplate.CODEC
                                                    .listOf(1, 3)
                                                    .fieldOf("results")
                                                    .forGetter(WashingRecipe::outputs),
                                            Codec.intRange(1, 8000)
                                                    .fieldOf("water")
                                                    .forGetter(WashingRecipe::water))
                                    .apply(instance, WashingRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, WashingRecipe> STREAM_CODEC =
            StreamCodec.composite(
                    Ingredient.CONTENTS_STREAM_CODEC,
                    WashingRecipe::ingredient,
                    ByteBufCodecs.VAR_INT,
                    WashingRecipe::inputCount,
                    ItemStackTemplate.STREAM_CODEC.apply(ByteBufCodecs.list(3)),
                    WashingRecipe::outputs,
                    ByteBufCodecs.VAR_INT,
                    WashingRecipe::water,
                    WashingRecipe::new);

    public WashingRecipe {
        Objects.requireNonNull(ingredient);
        outputs = List.copyOf(outputs);
        if (inputCount < 1
                || inputCount > 64
                || outputs.isEmpty()
                || outputs.size() > 3
                || water < 1
                || water > 8000)
            throw new IllegalArgumentException("Invalid washing recipe bounds");
    }

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return input.item().getCount() >= inputCount && ingredient.test(input.item());
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input) {
        return outputs.getFirst().create();
    }

    @Override
    public boolean showNotification() {
        return false;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public String group() {
        return "";
    }

    @Override
    public RecipeSerializer<WashingRecipe> getSerializer() {
        return ModProcessingRecipes.WASHING_SERIALIZER.get();
    }

    @Override
    public RecipeType<WashingRecipe> getType() {
        return ModProcessingRecipes.WASHING_TYPE.get();
    }

    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return ModProcessingRecipes.CATEGORY.get();
    }
}
