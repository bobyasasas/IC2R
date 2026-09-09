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

/**
 * Heat- and air-driven recipe: consumes {@code fluid} mB of air per tick while hot and finishes
 * after {@code duration} ticks, yielding one or two outputs (ingot plus slag).
 */
public record BlastFurnaceRecipe(
        Ingredient ingredient,
        int inputCount,
        List<ItemStackTemplate> outputs,
        int fluid,
        int duration)
        implements MultiOutputRecipe {
    public static final MapCodec<BlastFurnaceRecipe> CODEC =
            RecordCodecBuilder.mapCodec(
                    instance ->
                            instance.group(
                                            Ingredient.CODEC
                                                    .fieldOf("ingredient")
                                                    .forGetter(BlastFurnaceRecipe::ingredient),
                                            Codec.intRange(1, 64)
                                                    .optionalFieldOf("input_count", 1)
                                                    .forGetter(BlastFurnaceRecipe::inputCount),
                                            ItemStackTemplate.CODEC
                                                    .listOf(1, 2)
                                                    .fieldOf("results")
                                                    .forGetter(BlastFurnaceRecipe::outputs),
                                            Codec.intRange(0, 1000)
                                                    .fieldOf("fluid")
                                                    .forGetter(BlastFurnaceRecipe::fluid),
                                            Codec.intRange(1, 1000000)
                                                    .fieldOf("duration")
                                                    .forGetter(BlastFurnaceRecipe::duration))
                                    .apply(instance, BlastFurnaceRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, BlastFurnaceRecipe> STREAM_CODEC =
            StreamCodec.composite(
                    Ingredient.CONTENTS_STREAM_CODEC,
                    BlastFurnaceRecipe::ingredient,
                    ByteBufCodecs.VAR_INT,
                    BlastFurnaceRecipe::inputCount,
                    ItemStackTemplate.STREAM_CODEC.apply(ByteBufCodecs.list(2)),
                    BlastFurnaceRecipe::outputs,
                    ByteBufCodecs.VAR_INT,
                    BlastFurnaceRecipe::fluid,
                    ByteBufCodecs.VAR_INT,
                    BlastFurnaceRecipe::duration,
                    BlastFurnaceRecipe::new);

    public BlastFurnaceRecipe {
        Objects.requireNonNull(ingredient);
        outputs = List.copyOf(outputs);
        if (inputCount < 1 || inputCount > 64 || outputs.isEmpty() || outputs.size() > 2)
            throw new IllegalArgumentException("Invalid blast furnace recipe bounds");
    }

    @Override
    public RecipeSerializer<BlastFurnaceRecipe> getSerializer() {
        return ModProcessingRecipes.BLAST_FURNACE_SERIALIZER.get();
    }

    @Override
    public RecipeType<BlastFurnaceRecipe> getType() {
        return ModProcessingRecipes.BLAST_FURNACE_TYPE.get();
    }
}
