package ic2.neoforge.client.interop.jei;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Discovers canner fill/drain operations from the same item capability used by the machine. */
final class CannerFluidRecipes {
    record Result(List<CannerFluidRecipe> filling, List<CannerFluidRecipe> emptying) {}

    static Result scan(Collection<ItemStack> ingredients) {
        Set<ItemResource> candidates = new LinkedHashSet<>();
        for (Item item : BuiltInRegistries.ITEM)
            addCandidate(candidates, item.getDefaultInstance());
        for (ItemStack ingredient : ingredients) addCandidate(candidates, ingredient);

        Set<CannerFluidRecipe> filling = new LinkedHashSet<>();
        Set<CannerFluidRecipe> emptying = new LinkedHashSet<>();
        for (ItemResource filled : candidates) {
            ItemStack filledStack = filled.toStack();
            ResourceHandler<FluidResource> handler;
            int slots;
            try {
                handler = fluidHandler(filledStack);
                slots = handler == null ? 0 : handler.size();
            } catch (RuntimeException ignored) {
                // A third-party capability must not prevent the rest of JEI from loading.
                continue;
            }
            for (int slot = 0; slot < slots; slot++) {
                CannerFluidRecipe drained;
                try {
                    drained = drain(filledStack, filled, slot);
                } catch (RuntimeException ignored) {
                    continue;
                }
                if (drained == null || !emptying.add(drained)) continue;
                try {
                    if (canRestoreFilledContainer(drained)) filling.add(drained);
                } catch (RuntimeException ignored) {
                    // Keep the valid drain recipe even when a container cannot be refilled.
                }
            }
        }
        return new Result(List.copyOf(filling), List.copyOf(emptying));
    }

    private static void addCandidate(Set<ItemResource> candidates, ItemStack stack) {
        if (!stack.isEmpty()) candidates.add(ItemResource.of(stack.copyWithCount(1)));
    }

    private static CannerFluidRecipe drain(ItemStack filledStack, ItemResource filled, int slot) {
        ItemAccess access = mutableAccess(filledStack);
        ResourceHandler<FluidResource> handler = access.getCapability(Capabilities.Fluid.ITEM);
        if (handler == null || slot >= handler.size()) return null;
        FluidResource fluid = handler.getResource(slot);
        int amount = handler.getAmountAsInt(slot);
        if (fluid.isEmpty() || amount <= 0) return null;
        try (Transaction transaction = Transaction.openRoot()) {
            if (handler.extract(slot, fluid, amount, transaction) != amount) return null;
            transaction.commit();
        }
        return new CannerFluidRecipe(filled, access.getResource(), fluid, amount);
    }

    private static boolean canRestoreFilledContainer(CannerFluidRecipe recipe) {
        if (recipe.emptyContainer().isEmpty()) return false;
        ItemAccess access = mutableAccess(recipe.emptyStack());
        ResourceHandler<FluidResource> handler = access.getCapability(Capabilities.Fluid.ITEM);
        if (handler == null) return false;
        try (Transaction transaction = Transaction.openRoot()) {
            if (handler.insert(recipe.fluid(), recipe.amount(), transaction) != recipe.amount())
                return false;
            transaction.commit();
        }
        return access.getResource().equals(recipe.filledContainer());
    }

    private static ResourceHandler<FluidResource> fluidHandler(ItemStack stack) {
        return mutableAccess(stack).getCapability(Capabilities.Fluid.ITEM);
    }

    /** A real machine slot can replace a filled container with a different empty-container item. */
    private static ItemAccess mutableAccess(ItemStack stack) {
        SimpleContainer container = new SimpleContainer(1);
        container.setItem(0, stack.copyWithCount(1));
        return ItemAccess.forHandlerIndex(VanillaContainerWrapper.of(container), 0).oneByOne();
    }

    private CannerFluidRecipes() {}
}
