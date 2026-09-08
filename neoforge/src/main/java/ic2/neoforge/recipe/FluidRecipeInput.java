package ic2.neoforge.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

/** A fluid recipe has no item grid; RecipeInput's item-only emptiness default does not apply. */
public record FluidRecipeInput(FluidResource fluid, int amount) implements RecipeInput {
    @Override
    public ItemStack getItem(int index) {
        throw new IndexOutOfBoundsException(index);
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public boolean isEmpty() {
        return amount <= 0 || fluid.isEmpty();
    }
}
