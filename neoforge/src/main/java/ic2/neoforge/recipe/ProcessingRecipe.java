package ic2.neoforge.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.registration.ModProcessingRecipes;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Objects;

/** Immutable counted-input recipe with explicit weighted outcomes. */
public record ProcessingRecipe(
        MachineKind kind, Ingredient ingredient, int inputCount, List<Output> outputs)
        implements Recipe<SingleRecipeInput> {
    public record Output(ItemStackTemplate stack, int weight) {
        public static final Codec<Output> CODEC =
                RecordCodecBuilder.create(
                        instance ->
                                instance.group(
                                                ItemStackTemplate.CODEC
                                                        .fieldOf("stack")
                                                        .forGetter(Output::stack),
                                                Codec.intRange(1, 1000000)
                                                        .optionalFieldOf("weight", 1)
                                                        .forGetter(Output::weight))
                                        .apply(instance, Output::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, Output> STREAM_CODEC =
                StreamCodec.composite(
                        ItemStackTemplate.STREAM_CODEC,
                        Output::stack,
                        ByteBufCodecs.VAR_INT,
                        Output::weight,
                        Output::new);

        public Output {
            Objects.requireNonNull(stack);
            if (weight < 1 || weight > 1000000)
                throw new IllegalArgumentException("Invalid output weight");
        }
    }

    public ProcessingRecipe {
        Objects.requireNonNull(kind);
        Objects.requireNonNull(ingredient);
        outputs = List.copyOf(outputs);
        if (inputCount < 1 || inputCount > 64 || outputs.isEmpty() || outputs.size() > 64)
            throw new IllegalArgumentException("Invalid processing recipe bounds");
    }

    public static MapCodec<ProcessingRecipe> codec(MachineKind kind) {
        return RecordCodecBuilder.mapCodec(
                instance ->
                        instance.group(
                                        Ingredient.CODEC
                                                .fieldOf("ingredient")
                                                .forGetter(ProcessingRecipe::ingredient),
                                        Codec.intRange(1, 64)
                                                .optionalFieldOf("input_count", 1)
                                                .forGetter(ProcessingRecipe::inputCount),
                                        Output.CODEC
                                                .listOf(1, 64)
                                                .fieldOf("results")
                                                .forGetter(ProcessingRecipe::outputs))
                                .apply(
                                        instance,
                                        (ingredient, count, outputs) ->
                                                new ProcessingRecipe(
                                                        kind, ingredient, count, outputs)));
    }

    public static StreamCodec<RegistryFriendlyByteBuf, ProcessingRecipe> streamCodec(
            MachineKind kind) {
        return StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC,
                ProcessingRecipe::ingredient,
                ByteBufCodecs.VAR_INT,
                ProcessingRecipe::inputCount,
                Output.STREAM_CODEC.apply(ByteBufCodecs.list(64)),
                ProcessingRecipe::outputs,
                (ingredient, count, outputs) ->
                        new ProcessingRecipe(kind, ingredient, count, outputs));
    }

    public ItemStackTemplate chooseOutput(RandomSource random) {
        int roll = random.nextInt(outputs.stream().mapToInt(Output::weight).sum());
        for (Output output : outputs) {
            if (roll < output.weight()) return output.stack();
            roll -= output.weight();
        }
        throw new AssertionError("Weighted output selection exceeded its bound");
    }

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return input.item().getCount() >= inputCount && ingredient.test(input.item());
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input) {
        return outputs.getFirst().stack().create();
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
    public RecipeSerializer<ProcessingRecipe> getSerializer() {
        return ModProcessingRecipes.serializer(kind);
    }

    @Override
    public RecipeType<ProcessingRecipe> getType() {
        return ModProcessingRecipes.type(kind);
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
