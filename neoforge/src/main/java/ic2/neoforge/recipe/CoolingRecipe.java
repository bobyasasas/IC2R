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

/** Cooling exchanges equal fluid volumes; heat is specified per millibucket. */
public record CoolingRecipe(FluidStackTemplate input, FluidStackTemplate result, int heat)
        implements FluidRecipe {
    public CoolingRecipe {
        if (input.amount() != 1 || result.amount() != 1 || heat <= 0)
            throw new IllegalArgumentException(
                    "Cooling requires one-millibucket templates and positive heat");
    }

    public static final MapCodec<CoolingRecipe> CODEC =
            RecordCodecBuilder.mapCodec(
                    i ->
                            i.group(
                                            FluidStackTemplate.CODEC
                                                    .fieldOf("input")
                                                    .forGetter(CoolingRecipe::input),
                                            FluidStackTemplate.CODEC
                                                    .fieldOf("result")
                                                    .forGetter(CoolingRecipe::result),
                                            Codec.intRange(1, Integer.MAX_VALUE)
                                                    .fieldOf("heat")
                                                    .forGetter(CoolingRecipe::heat))
                                    .apply(i, CoolingRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, CoolingRecipe> STREAM_CODEC =
            StreamCodec.composite(
                    FluidStackTemplate.STREAM_CODEC,
                    CoolingRecipe::input,
                    FluidStackTemplate.STREAM_CODEC,
                    CoolingRecipe::result,
                    ByteBufCodecs.VAR_INT,
                    CoolingRecipe::heat,
                    CoolingRecipe::new);

    @Override
    public RecipeSerializer<CoolingRecipe> getSerializer() {
        return ModThermalRecipes.COOLING_SERIALIZER.get();
    }

    @Override
    public RecipeType<CoolingRecipe> getType() {
        return ModThermalRecipes.COOLING.get();
    }
}
