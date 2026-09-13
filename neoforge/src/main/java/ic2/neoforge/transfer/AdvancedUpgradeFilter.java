package ic2.neoforge.transfer;

import ic2.neoforge.component.AdvancedFilterSettings;
import ic2.neoforge.component.ModDataComponents;
import ic2.neoforge.item.ElectricItem;
import ic2.neoforge.item.ElectricItemEnergy;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import net.neoforged.neoforge.transfer.item.ItemResource;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Legacy ItemUpgradeModule.stackChecker for the advanced ejector/pulling upgrades: active energy
 * comparisons gate electric items by charge, otherwise the nine filter slots govern (item match,
 * NBT mode, charge similarity when the energy flag is on). An advanced upgrade with neither
 * filters nor an active energy flag moves nothing — that inertness is legacy behavior.
 */
public final class AdvancedUpgradeFilter {
    /** |a-b| < 1e-5 — legacy Util.isSimilar on the two stacks' stored charges. */
    private static final double CHARGE_EPSILON = 1.0e-5;

    public static Predicate<ItemResource> of(ItemStack upgrade) {
        List<ItemStack> filters = new ArrayList<>();
        var contents =
                upgrade.getOrDefault(
                        ModDataComponents.ADVANCED_FILTER_ITEMS, ItemContainerContents.EMPTY);
        for (int slot = 0; slot < contents.getSlots(); slot++) {
            ItemStack entry = contents.getStackInSlot(slot);
            if (!entry.isEmpty()
                    && filters.stream().noneMatch(other -> ItemStack.isSameItemSameComponents(other, entry)))
                filters.add(entry);
        }
        int nbtMode = upgrade.getOrDefault(ModDataComponents.ADVANCED_NBT_MODE, 0);
        var energy =
                upgrade.getOrDefault(
                        ModDataComponents.ADVANCED_ENERGY, AdvancedFilterSettings.DEFAULT);
        return resource -> resource.test(stack -> matches(stack, filters, nbtMode, energy));
    }

    public static boolean matches(
            ItemStack stack,
            List<ItemStack> filters,
            int nbtMode,
            AdvancedFilterSettings energy) {
        boolean checkEnergy;
        if (!energy.comparisonType().ignoreFilters()) {
            // COMPARISON/RANGE: only electric items past the configured bound(s) move at all.
            if (!(stack.getItem() instanceof ElectricItem)
                    || !energy.doComparison((int) ElectricItemEnergy.charge(stack))) return false;
            checkEnergy = false;
        } else {
            checkEnergy = energy.active();
            if (checkEnergy && !(stack.getItem() instanceof ElectricItem)) return false;
        }
        for (ItemStack filter : filters) {
            if (filter.getItem() == stack.getItem()
                    && checkNbt(stack, filter, nbtMode)
                    && (!checkEnergy || similarCharge(stack, filter))) return true;
        }
        return filters.isEmpty() && energy.active() && !checkEnergy;
    }

    /**
     * Legacy compares NBT tag maps with a fuzzy/exact pair of flavors; the component model has a
     * single notion of "same item, same components". Legacy never wrote the mode byte anywhere,
     * so both non-ignored modes collapse onto it — faithfully unreachable either way.
     */
    private static boolean checkNbt(ItemStack stack, ItemStack filter, int nbtMode) {
        return nbtMode == 0 || ItemStack.isSameItemSameComponents(stack, filter);
    }

    private static boolean similarCharge(ItemStack stack, ItemStack filter) {
        return Math.abs(ElectricItemEnergy.charge(stack) - ElectricItemEnergy.charge(filter))
                < CHARGE_EPSILON;
    }

    private AdvancedUpgradeFilter() {}
}
