package ic2.neoforge.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import ic2.neoforge.registration.ModCannerRecipes;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStackTemplate;

public record EnrichingRecipe(
        FluidStackTemplate inputFluid, CountedIngredient additive, FluidStackTemplate result)
        implements CannerRecipe {
    public static final MapCodec<EnrichingRecipe> CODEC =
            RecordCodecBuilder.mapCodec(
                    instance ->
                            instance.group(
                                            FluidStackTemplate.CODEC
                                                    .fieldOf("input_fluid")
                                                    .forGetter(EnrichingRecipe::inputFluid),
                                            CountedIngredient.CODEC
                                                    .fieldOf("additive")
                                                    .forGetter(EnrichingRecipe::additive),
                                            FluidStackTemplate.CODEC
                                                    .fieldOf("result")
                                                    .forGetter(EnrichingRecipe::result))
                                    .apply(instance, EnrichingRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, EnrichingRecipe> STREAM_CODEC =
            StreamCodec.composite(
                    FluidStackTemplate.STREAM_CODEC,
                    EnrichingRecipe::inputFluid,
                    CountedIngredient.STREAM_CODEC,
                    EnrichingRecipe::additive,
                    FluidStackTemplate.STREAM_CODEC,
                    EnrichingRecipe::result,
                    EnrichingRecipe::new);

    @Override
    public boolean matches(CannerInput input, Level level) {
        return input.fluid().matches(inputFluid)
                && input.amount() >= inputFluid.amount()
                && additive.test(input.additive());
    }

    @Override
    public ItemStack assemble(CannerInput input) {
        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<EnrichingRecipe> getSerializer() {
        return ModCannerRecipes.ENRICH_SERIALIZER.get();
    }

    @Override
    public RecipeType<EnrichingRecipe> getType() {
        return ModCannerRecipes.ENRICH.get();
    }
}
