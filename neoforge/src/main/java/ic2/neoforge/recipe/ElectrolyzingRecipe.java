package ic2.neoforge.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import ic2.neoforge.registration.ModProcessingRecipes;
import ic2.neoforge.registration.ModThermalRecipes;

import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStackTemplate;

import java.util.List;

/** One fluid input with independently directed outputs; every output participates in one commit. */
public record ElectrolyzingRecipe(
        FluidStackTemplate input, int euPerTick, int ticks, List<Output> outputs)
        implements Recipe<FluidRecipeInput> {
    public record Output(Direction direction, FluidStackTemplate fluid) {
        public static final Codec<Output> CODEC =
                RecordCodecBuilder.create(
                        i ->
                                i.group(
                                                Direction.CODEC
                                                        .fieldOf("direction")
                                                        .forGetter(Output::direction),
                                                FluidStackTemplate.CODEC
                                                        .fieldOf("fluid")
                                                        .forGetter(Output::fluid))
                                        .apply(i, Output::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, Output> STREAM_CODEC =
                StreamCodec.composite(
                        Direction.STREAM_CODEC,
                        Output::direction,
                        FluidStackTemplate.STREAM_CODEC,
                        Output::fluid,
                        Output::new);

        public Output {
            if (direction == null || fluid.amount() < 1 || fluid.amount() > 24000)
                throw new IllegalArgumentException("Invalid electrolysis output");
        }
    }

    public ElectrolyzingRecipe {
        outputs = List.copyOf(outputs);
        if (input.amount() < 1
                || input.amount() > 8000
                || euPerTick < 1
                || euPerTick > 32000
                || ticks < 1
                || outputs.isEmpty()
                || outputs.size() > 6
                || outputs.stream().map(Output::direction).distinct().count() != outputs.size())
            throw new IllegalArgumentException(
                    "Invalid electrolysis batch or repeated output direction");
    }

    public static final MapCodec<ElectrolyzingRecipe> CODEC =
            RecordCodecBuilder.mapCodec(
                    i ->
                            i.group(
                                            FluidStackTemplate.CODEC
                                                    .fieldOf("input")
                                                    .forGetter(ElectrolyzingRecipe::input),
                                            Codec.intRange(1, 32000)
                                                    .fieldOf("eu_per_tick")
                                                    .forGetter(ElectrolyzingRecipe::euPerTick),
                                            Codec.intRange(1, Integer.MAX_VALUE)
                                                    .fieldOf("ticks")
                                                    .forGetter(ElectrolyzingRecipe::ticks),
                                            Output.CODEC
                                                    .listOf(1, 6)
                                                    .fieldOf("outputs")
                                                    .forGetter(ElectrolyzingRecipe::outputs))
                                    .apply(i, ElectrolyzingRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, ElectrolyzingRecipe> STREAM_CODEC =
            StreamCodec.composite(
                    FluidStackTemplate.STREAM_CODEC,
                    ElectrolyzingRecipe::input,
                    ByteBufCodecs.VAR_INT,
                    ElectrolyzingRecipe::euPerTick,
                    ByteBufCodecs.VAR_INT,
                    ElectrolyzingRecipe::ticks,
                    Output.STREAM_CODEC.apply(ByteBufCodecs.list(6)),
                    ElectrolyzingRecipe::outputs,
                    ElectrolyzingRecipe::new);

    @Override
    public boolean matches(FluidRecipeInput contents, Level level) {
        return contents.fluid().matches(input) && contents.amount() >= input.amount();
    }

    @Override
    public ItemStack assemble(FluidRecipeInput input) {
        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<ElectrolyzingRecipe> getSerializer() {
        return ModThermalRecipes.ELECTROLYZING_SERIALIZER.get();
    }

    @Override
    public RecipeType<ElectrolyzingRecipe> getType() {
        return ModThermalRecipes.ELECTROLYZING.get();
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
    public PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return ModProcessingRecipes.CATEGORY.get();
    }
}
