package ic2.neoforge.client.interop.jei;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;

/** One container operation exposed by the canner's native fluid-item capability. */
record CannerFluidRecipe(
        ItemResource filledContainer,
        ItemResource emptyContainer,
        FluidResource fluid,
        int amount) {
    CannerFluidRecipe {
        if (filledContainer.isEmpty() || fluid.isEmpty() || amount <= 0)
            throw new IllegalArgumentException("Invalid canner fluid recipe");
    }

    ItemStack filledStack() {
        return filledContainer.toStack();
    }

    ItemStack emptyStack() {
        return emptyContainer.isEmpty() ? ItemStack.EMPTY : emptyContainer.toStack();
    }
}
