package ic2.neoforge.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import ic2.neoforge.registration.ModThermalRecipes;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.fluids.FluidStackTemplate;

/** Heating exchanges equal fluid volumes; heat is specified per millibucket. */
public record HeatingRecipe(FluidStackTemplate input, FluidStackTemplate result, int heat)
        implements FluidRecipe {
    public HeatingRecipe {
        if (input.amount() != 1 || result.amount() != 1 || heat <= 0)
            throw new IllegalArgumentException(
                    "Heating requires one-millibucket templates and positive heat");
    }

    public static final MapCodec<HeatingRecipe> CODEC =
            RecordCodecBuilder.mapCodec(
                    i ->
                            i.group(
                                            FluidStackTemplate.CODEC
                                                    .fieldOf("input")
                                                    .forGetter(HeatingRecipe::input),
                                            FluidStackTemplate.CODEC
                                                    .fieldOf("result")
                                                    .forGetter(HeatingRecipe::result),
                                            Codec.intRange(1, Integer.MAX_VALUE)
                                                    .fieldOf("heat")
                                                    .forGetter(HeatingRecipe::heat))
                                    .apply(i, HeatingRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, HeatingRecipe> STREAM_CODEC =
            StreamCodec.composite(
                    FluidStackTemplate.STREAM_CODEC,
                    HeatingRecipe::input,
                    FluidStackTemplate.STREAM_CODEC,
                    HeatingRecipe::result,
                    ByteBufCodecs.VAR_INT,
                    HeatingRecipe::heat,
                    HeatingRecipe::new);

    @Override
    public RecipeSerializer<HeatingRecipe> getSerializer() {
        return ModThermalRecipes.HEATING_SERIALIZER.get();
    }

    @Override
    public RecipeType<HeatingRecipe> getType() {
        return ModThermalRecipes.HEATING.get();
    }
}
