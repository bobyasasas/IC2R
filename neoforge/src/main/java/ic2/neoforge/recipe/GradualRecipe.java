package ic2.neoforge.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import ic2.neoforge.component.ModDataComponents;
import ic2.neoforge.item.CondensatorItem;
import ic2.neoforge.registration.ModProcessingRecipes;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import java.util.Objects;

/**
 * Gradual recharging: a heat-storing condensator vents {@code amount} of stored heat per charge
 * material, one material per occupied crafting slot. An already-empty condensator is rejected.
 */
public final class GradualRecipe implements CraftingRecipe {
    public static final MapCodec<GradualRecipe> CODEC =
            RecordCodecBuilder.mapCodec(
                    instance ->
                            instance.group(
                                            BuiltInRegistries.ITEM
                                                    .byNameCodec()
                                                    .fieldOf("item")
                                                    .forGetter(GradualRecipe::component),
                                            BuiltInRegistries.ITEM
                                                    .byNameCodec()
                                                    .fieldOf("charge_material")
                                                    .forGetter(GradualRecipe::chargeMaterial),
                                            Codec.intRange(1, 1000000)
                                                    .fieldOf("amount")
                                                    .forGetter(GradualRecipe::amount),
                                            Codec.BOOL
                                                    .optionalFieldOf("hidden", false)
                                                    .forGetter(GradualRecipe::hidden))
                                    .apply(instance, GradualRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, GradualRecipe> STREAM_CODEC =
            StreamCodec.ofMember(GradualRecipe::write, GradualRecipe::read);

    private final CondensatorItem component;
    private final Item chargeMaterial;
    private final int amount;
    private final boolean hidden;

    public GradualRecipe(Item component, Item chargeMaterial, int amount, boolean hidden) {
        if (!(component instanceof CondensatorItem condensator))
            throw new IllegalArgumentException(
                    "Gradual recipe item must be a condensator: " + component);
        this.component = condensator;
        this.chargeMaterial = Objects.requireNonNull(chargeMaterial);
        this.amount = amount;
        this.hidden = hidden;
    }

    public CondensatorItem component() {
        return component;
    }

    public Item chargeMaterial() {
        return chargeMaterial;
    }

    public int amount() {
        return amount;
    }

    public boolean hidden() {
        return hidden;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return !assemble(input).isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        ItemStack componentStack = null;
        int materials = 0;
        for (ItemStack stack : input.items()) {
            if (stack.isEmpty()) continue;
            if (componentStack == null && stack.is(component)) {
                componentStack = stack;
                continue;
            }
            if (!stack.is(chargeMaterial)) return ItemStack.EMPTY;
            materials++;
        }
        if (componentStack == null || materials == 0) return ItemStack.EMPTY;
        int stored = componentStack.getOrDefault(ModDataComponents.REACTOR_HEAT, 0);
        if (stored <= 0) return ItemStack.EMPTY;
        int remaining = Math.clamp(stored - (long) amount * materials, 0, component.maxUse());
        ItemStack result = componentStack.copyWithCount(1);
        result.set(ModDataComponents.REACTOR_HEAT, remaining);
        return result;
    }

    @Override
    public String group() {
        return "";
    }

    @Override
    public boolean showNotification() {
        return false;
    }

    @Override
    public CraftingBookCategory category() {
        return CraftingBookCategory.MISC;
    }

    @Override
    public RecipeSerializer<? extends CraftingRecipe> getSerializer() {
        return ModProcessingRecipes.GRADUAL_SERIALIZER.get();
    }

    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(BuiltInRegistries.ITEM.getId(component));
        buf.writeVarInt(BuiltInRegistries.ITEM.getId(chargeMaterial));
        buf.writeVarInt(amount);
        buf.writeBoolean(hidden);
    }

    private static GradualRecipe read(RegistryFriendlyByteBuf buf) {
        return new GradualRecipe(
                BuiltInRegistries.ITEM.byId(buf.readVarInt()),
                BuiltInRegistries.ITEM.byId(buf.readVarInt()),
                buf.readVarInt(),
                buf.readBoolean());
    }
}
