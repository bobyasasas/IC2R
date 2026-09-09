package ic2.neoforge.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import ic2.neoforge.registration.ModProcessingRecipes;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

import java.util.Objects;

/** UU amplification: one input yields {@code result} units of scrap value for the generator. */
public record MatterFabricatorRecipe(Ingredient ingredient, int inputCount, int result)
        implements Recipe<SingleRecipeInput> {
    public static final MapCodec<MatterFabricatorRecipe> CODEC =
            RecordCodecBuilder.mapCodec(
                    instance ->
                            instance.group(
                                            Ingredient.CODEC
                                                    .fieldOf("ingredient")
                                                    .forGetter(MatterFabricatorRecipe::ingredient),
                                            Codec.intRange(1, 64)
                                                    .optionalFieldOf("input_count", 1)
                                                    .forGetter(MatterFabricatorRecipe::inputCount),
                                            Codec.intRange(1, Integer.MAX_VALUE)
                                                    .fieldOf("result")
                                                    .forGetter(MatterFabricatorRecipe::result))
                                    .apply(instance, MatterFabricatorRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, MatterFabricatorRecipe> STREAM_CODEC =
            StreamCodec.composite(
                    Ingredient.CONTENTS_STREAM_CODEC,
                    MatterFabricatorRecipe::ingredient,
                    ByteBufCodecs.VAR_INT,
                    MatterFabricatorRecipe::inputCount,
                    ByteBufCodecs.VAR_INT,
                    MatterFabricatorRecipe::result,
                    MatterFabricatorRecipe::new);

    public MatterFabricatorRecipe {
        Objects.requireNonNull(ingredient);
        if (inputCount < 1 || inputCount > 64)
            throw new IllegalArgumentException("Invalid matter fabricator recipe bounds");
    }

    @Override
    public String group() {
        return "";
    }

    @Override
    public boolean showNotification() {
        return false;
    }

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return input.item().getCount() >= inputCount && ingredient.test(input.item());
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input) {
        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<? extends MatterFabricatorRecipe> getSerializer() {
        return ModProcessingRecipes.MATTER_FABRICATOR_SERIALIZER.get();
    }

    @Override
    public RecipeType<? extends MatterFabricatorRecipe> getType() {
        return ModProcessingRecipes.MATTER_FABRICATOR_TYPE.get();
    }

    @Override
    public net.minecraft.world.item.crafting.PlacementInfo placementInfo() {
        return net.minecraft.world.item.crafting.PlacementInfo.NOT_PLACEABLE;
    }

    @Override
    public net.minecraft.world.item.crafting.RecipeBookCategory recipeBookCategory() {
        return ic2.neoforge.registration.ModProcessingRecipes.CATEGORY.get();
    }
}
