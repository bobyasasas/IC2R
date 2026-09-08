package ic2.neoforge.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import ic2.core.machine.FermentationCycle;
import ic2.neoforge.registration.ModThermalRecipes;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.*;
import net.neoforged.neoforge.fluids.FluidStackTemplate;

/**
 * Datapacks own fluid conversion and heat cost; invalid zero-cost or oversized batches are
 * rejected.
 */
public record FermentingRecipe(
        FluidStackTemplate input, FluidStackTemplate result, int heat, int fertilizerInterval)
        implements FluidRecipe {
    public FermentingRecipe {
        if (input.amount() < 1
                || input.amount() > 10000
                || result.amount() < 1
                || result.amount() > 2000
                || heat < 1
                || fertilizerInterval < 1)
            throw new IllegalArgumentException("Fermentation batch exceeds machine limits");
    }

    public static final MapCodec<FermentingRecipe> CODEC =
            RecordCodecBuilder.mapCodec(
                    i ->
                            i.group(
                                            FluidStackTemplate.CODEC
                                                    .fieldOf("input")
                                                    .forGetter(FermentingRecipe::input),
                                            FluidStackTemplate.CODEC
                                                    .fieldOf("result")
                                                    .forGetter(FermentingRecipe::result),
                                            Codec.intRange(1, Integer.MAX_VALUE)
                                                    .fieldOf("heat")
                                                    .forGetter(FermentingRecipe::heat),
                                            Codec.intRange(1, Integer.MAX_VALUE)
                                                    .fieldOf("fertilizer_interval")
                                                    .forGetter(
                                                            FermentingRecipe::fertilizerInterval))
                                    .apply(i, FermentingRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, FermentingRecipe> STREAM_CODEC =
            StreamCodec.composite(
                    FluidStackTemplate.STREAM_CODEC,
                    FermentingRecipe::input,
                    FluidStackTemplate.STREAM_CODEC,
                    FermentingRecipe::result,
                    ByteBufCodecs.VAR_INT,
                    FermentingRecipe::heat,
                    ByteBufCodecs.VAR_INT,
                    FermentingRecipe::fertilizerInterval,
                    FermentingRecipe::new);

    public FermentationCycle.Batch batch() {
        return new FermentationCycle.Batch(input.amount(), heat, fertilizerInterval);
    }

    @Override
    public RecipeSerializer<FermentingRecipe> getSerializer() {
        return ModThermalRecipes.FERMENTING_SERIALIZER.get();
    }

    @Override
    public RecipeType<FermentingRecipe> getType() {
        return ModThermalRecipes.FERMENTING.get();
    }
}
