package ic2.neoforge.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import ic2.neoforge.registration.ModProcessingRecipes;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.*;

import java.util.List;
import java.util.Objects;

/** Immutable multi-output recipe with an explicit operating temperature. */
public record CentrifugeRecipe(
        Ingredient ingredient, int inputCount, List<ItemStackTemplate> outputs, int minHeat)
        implements MultiOutputRecipe {
    public static final MapCodec<CentrifugeRecipe> CODEC =
            RecordCodecBuilder.mapCodec(
                    instance ->
                            instance.group(
                                            Ingredient.CODEC
                                                    .fieldOf("ingredient")
                                                    .forGetter(CentrifugeRecipe::ingredient),
                                            Codec.intRange(1, 64)
                                                    .optionalFieldOf("input_count", 1)
                                                    .forGetter(CentrifugeRecipe::inputCount),
                                            ItemStackTemplate.CODEC
                                                    .listOf(1, 3)
                                                    .fieldOf("results")
                                                    .forGetter(CentrifugeRecipe::outputs),
                                            Codec.intRange(0, 5000)
                                                    .fieldOf("min_heat")
                                                    .forGetter(CentrifugeRecipe::minHeat))
                                    .apply(instance, CentrifugeRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, CentrifugeRecipe> STREAM_CODEC =
            StreamCodec.composite(
                    Ingredient.CONTENTS_STREAM_CODEC,
                    CentrifugeRecipe::ingredient,
                    ByteBufCodecs.VAR_INT,
                    CentrifugeRecipe::inputCount,
                    ItemStackTemplate.STREAM_CODEC.apply(ByteBufCodecs.list(3)),
                    CentrifugeRecipe::outputs,
                    ByteBufCodecs.VAR_INT,
                    CentrifugeRecipe::minHeat,
                    CentrifugeRecipe::new);

    public CentrifugeRecipe {
        Objects.requireNonNull(ingredient);
        outputs = List.copyOf(outputs);
        if (inputCount < 1
                || inputCount > 64
                || outputs.isEmpty()
                || outputs.size() > 3
                || minHeat < 0
                || minHeat > 5000)
            throw new IllegalArgumentException("Invalid centrifuge recipe bounds");
    }

    @Override
    public RecipeSerializer<CentrifugeRecipe> getSerializer() {
        return ModProcessingRecipes.CENTRIFUGE_SERIALIZER.get();
    }

    @Override
    public RecipeType<CentrifugeRecipe> getType() {
        return ModProcessingRecipes.CENTRIFUGE_TYPE.get();
    }
}
