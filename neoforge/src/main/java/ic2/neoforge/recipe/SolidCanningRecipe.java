package ic2.neoforge.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import ic2.neoforge.registration.ModCannerRecipes;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

public record SolidCanningRecipe(
        CountedIngredient container, CountedIngredient additive, ItemStackTemplate result)
        implements CannerRecipe {
    public static final MapCodec<SolidCanningRecipe> CODEC =
            RecordCodecBuilder.mapCodec(
                    instance ->
                            instance.group(
                                            CountedIngredient.CODEC
                                                    .fieldOf("container")
                                                    .forGetter(SolidCanningRecipe::container),
                                            CountedIngredient.CODEC
                                                    .fieldOf("additive")
                                                    .forGetter(SolidCanningRecipe::additive),
                                            ItemStackTemplate.CODEC
                                                    .fieldOf("result")
                                                    .forGetter(SolidCanningRecipe::result))
                                    .apply(instance, SolidCanningRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, SolidCanningRecipe> STREAM_CODEC =
            StreamCodec.composite(
                    CountedIngredient.STREAM_CODEC,
                    SolidCanningRecipe::container,
                    CountedIngredient.STREAM_CODEC,
                    SolidCanningRecipe::additive,
                    ItemStackTemplate.STREAM_CODEC,
                    SolidCanningRecipe::result,
                    SolidCanningRecipe::new);

    @Override
    public boolean matches(CannerInput input, Level level) {
        return container.test(input.container()) && additive.test(input.additive());
    }

    @Override
    public ItemStack assemble(CannerInput input) {
        return result.create();
    }

    @Override
    public RecipeSerializer<SolidCanningRecipe> getSerializer() {
        return ModCannerRecipes.SOLID_SERIALIZER.get();
    }

    @Override
    public RecipeType<SolidCanningRecipe> getType() {
        return ModCannerRecipes.SOLID.get();
    }
}
