package ic2.core.recipe;

import com.google.gson.JsonObject;

import ic2.core.item.reactor.AbstractDamageableReactorComponent;
import ic2.core.ref.Ic2RecipeSerializers;
import ic2.core.util.StackUtil;

import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.NotNull;

public class GradualRecipe implements CraftingRecipe {
    private final ResourceLocation id;
    private final AbstractDamageableReactorComponent item;
    private final ItemStack chargeMaterial;
    private final int amount;
    private final boolean hidden;

    public GradualRecipe(
            ResourceLocation id,
            AbstractDamageableReactorComponent item,
            ItemStack chargeMaterial,
            int amount,
            boolean hidden) {
        this.id = id;
        this.item = item;
        this.chargeMaterial = chargeMaterial;
        this.amount = amount;
        this.hidden = hidden;
    }

    @Override
    public boolean matches(@NotNull CraftingContainer inv, @NotNull Level world) {
        return !this.assemble(inv, null).isEmpty();
    }

    @NotNull
    @Override
    public ItemStack assemble(@NotNull CraftingContainer inv, RegistryAccess registryAccess) {
        ItemStack componentStack = null;
        int chargeMaterialSlots = 0;

        for (int slot = 0; slot < inv.getContainerSize(); slot++) {
            ItemStack stack = inv.getItem(slot);
            if (StackUtil.isEmpty(stack)) {
                continue;
            }
            if (componentStack == null && stack.getItem() == this.item) {
                componentStack = stack;
                continue;
            }
            if (!StackUtil.checkItemEquality(stack, this.chargeMaterial)) {
                return ItemStack.EMPTY;
            }
            // A crafting operation consumes one material per occupied slot, not the whole stack.
            chargeMaterialSlots++;
        }

        if (componentStack == null || chargeMaterialSlots <= 0) {
            return ItemStack.EMPTY;
        }
        int currentUse = this.item.getUse(componentStack);
        if (currentUse <= 0) {
            return ItemStack.EMPTY;
        }

        ItemStack result = componentStack.copy();
        result.setCount(1);
        int remainingUse = currentUse - this.amount * chargeMaterialSlots;
        if (remainingUse > this.item.getMaxUse()) {
            remainingUse = this.item.getMaxUse();
        } else if (remainingUse < 0) {
            remainingUse = 0;
        }

        this.item.setUse(result, remainingUse);
        return result;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @NotNull
    @Override
    public ItemStack getResultItem(@NotNull RegistryAccess registryAccess) {
        return new ItemStack(this.item);
    }

    @NotNull
    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        if (!this.hidden) {
            list.add(Ingredient.of(this.item));
            list.add(Ingredient.of(this.chargeMaterial));
        }

        return list;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @NotNull
    @Override
    public ResourceLocation getId() {
        return this.id;
    }

    @NotNull
    @Override
    public RecipeSerializer<?> getSerializer() {
        return Ic2RecipeSerializers.GRADUAL;
    }

    @NotNull
    @Override
    public CraftingBookCategory category() {
        return CraftingBookCategory.MISC;
    }

    public AbstractDamageableReactorComponent getItem() {
        return this.item;
    }

    public ItemStack getChargeMaterial() {
        return this.chargeMaterial;
    }

    public int getAmount() {
        return this.amount;
    }

    public static class Serializer implements RecipeSerializer<GradualRecipe> {
        public GradualRecipe fromJson(ResourceLocation id, JsonObject json) {
            Item item = GsonHelper.getAsItem(json, "item");
            if (item instanceof AbstractDamageableReactorComponent component) {
                ItemStack chargeMaterial =
                        ShapedRecipe.itemStackFromJson(
                                GsonHelper.getAsJsonObject(json, "charge_material"));
                int amount = GsonHelper.getAsInt(json, "amount");
                boolean hidden = GsonHelper.getAsBoolean(json, "hidden", false);
                return new GradualRecipe(id, component, chargeMaterial, amount, hidden);
            } else {
                throw new IllegalArgumentException(
                        "Gradual recipe item must be a damageable reactor component: " + item);
            }
        }

        public GradualRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            Item item = BuiltInRegistries.ITEM.byId(buf.readVarInt());
            if (item instanceof AbstractDamageableReactorComponent component) {
                ItemStack chargeMaterial = buf.readItem();
                int amount = buf.readVarInt();
                boolean hidden = buf.readBoolean();
                return new GradualRecipe(id, component, chargeMaterial, amount, hidden);
            } else {
                throw new IllegalStateException(
                        "Gradual recipe item is not a damageable reactor component: " + item);
            }
        }

        public void toNetwork(FriendlyByteBuf buf, GradualRecipe recipe) {
            buf.writeVarInt(BuiltInRegistries.ITEM.getId(recipe.item));
            buf.writeItem(recipe.chargeMaterial);
            buf.writeVarInt(recipe.amount);
            buf.writeBoolean(recipe.hidden);
        }
    }
}
