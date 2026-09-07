package ic2.neoforge.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import ic2.neoforge.item.CraftingToolItem;
import ic2.neoforge.item.ElectricItemEnergy;
import ic2.neoforge.registration.ModCraftingRecipes;

import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Reuses vanilla matching and recipe-book displays while preserving IC2 charge and remainder rules.
 */
public record ElectricCraftingRecipe(CraftingRecipe delegate, boolean consuming, boolean hidden)
        implements CraftingRecipe {
    public ElectricCraftingRecipe {
        if (!(delegate instanceof ShapedRecipe || delegate instanceof ShapelessRecipe))
            throw new IllegalArgumentException("Unsupported crafting delegate");
    }

    public static <T extends CraftingRecipe> MapCodec<ElectricCraftingRecipe> codec(
            MapCodec<T> delegateCodec, Class<T> type) {
        return RecordCodecBuilder.mapCodec(
                instance ->
                        instance.group(
                                        delegateCodec.forGetter(
                                                recipe -> type.cast(recipe.delegate())),
                                        Codec.BOOL
                                                .optionalFieldOf("consuming", false)
                                                .forGetter(ElectricCraftingRecipe::consuming),
                                        Codec.BOOL
                                                .optionalFieldOf("hidden", false)
                                                .forGetter(ElectricCraftingRecipe::hidden))
                                .apply(instance, ElectricCraftingRecipe::new));
    }

    public static <T extends CraftingRecipe>
            StreamCodec<RegistryFriendlyByteBuf, ElectricCraftingRecipe> streamCodec(
                    StreamCodec<RegistryFriendlyByteBuf, T> delegateCodec, Class<T> type) {
        return StreamCodec.composite(
                delegateCodec,
                recipe -> type.cast(recipe.delegate()),
                ByteBufCodecs.BOOL,
                ElectricCraftingRecipe::consuming,
                ByteBufCodecs.BOOL,
                ElectricCraftingRecipe::hidden,
                ElectricCraftingRecipe::new);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return delegate.matches(input, level);
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        ItemStack result = delegate.assemble(input);
        double charge = 0;
        for (var stack : input.items()) charge += ElectricItemEnergy.charge(stack);
        ElectricItemEnergy.charge(result, charge, Integer.MAX_VALUE, true, false);
        return result;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        if (consuming) return NonNullList.withSize(input.size(), ItemStack.EMPTY);
        var remaining = delegate.getRemainingItems(input);
        if (delegate instanceof ShapelessRecipe) {
            for (int slot = 0; slot < input.size(); slot++) {
                var stack = input.getItem(slot);
                if (stack.getItem() instanceof CraftingToolItem tool)
                    remaining.set(slot, tool.craftingRemainder(stack));
            }
        }
        return remaining;
    }

    @Override
    public boolean showNotification() {
        return !hidden && delegate.showNotification();
    }

    @Override
    public boolean isSpecial() {
        return hidden;
    }

    @Override
    public String group() {
        return delegate.group();
    }

    @Override
    public CraftingBookCategory category() {
        return delegate.category();
    }

    @Override
    public PlacementInfo placementInfo() {
        return delegate.placementInfo();
    }

    @Override
    public List<RecipeDisplay> display() {
        return hidden ? List.of() : delegate.display();
    }

    @Override
    public RecipeSerializer<ElectricCraftingRecipe> getSerializer() {
        return (delegate instanceof ShapedRecipe
                        ? ModCraftingRecipes.SHAPED
                        : ModCraftingRecipes.SHAPELESS)
                .get();
    }
}
