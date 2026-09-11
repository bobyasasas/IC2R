package ic2.neoforge.recipe;

import ic2.neoforge.component.ModDataComponents;
import ic2.neoforge.item.ElectricItem;
import ic2.neoforge.item.ElectricItemEnergy;
import ic2.neoforge.item.JetpackAttachmentHelper;
import ic2.neoforge.item.JetpackElectricItem;
import ic2.neoforge.registration.ModArmor;
import ic2.neoforge.registration.ModItems;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.level.Level;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Legacy JetpackAttachmentRecipe: one electric jetpack, one non-blacklisted chest armor and one
 * attachment plate yield the armor carrying a virtual jetpack; the jetpack's charge moves into
 * the armor (its own battery when electric, the component battery otherwise). No pattern — the
 * three parts may sit anywhere in the grid.
 */
public final class JetpackAttachmentRecipe implements CraftingRecipe {
    /** Legacy blacklist: the jetpacks themselves, the quantum suit and the elytra. */
    private static Set<Item> blacklist() {
        Set<Item> items = new HashSet<>();
        items.add(ModArmor.JETPACK.get());
        items.add(ModArmor.JETPACK_ELECTRIC.get());
        items.add(ModArmor.QUANTUM_CHESTPLATE.get());
        items.add(Items.ELYTRA);
        return items;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return !assemble(input).isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        ItemStack jetpack = ItemStack.EMPTY;
        ItemStack armor = ItemStack.EMPTY;
        boolean plate = false;
        for (ItemStack current : input.items()) {
            if (current.isEmpty()) continue;
            if (current.is(ModArmor.JETPACK_ELECTRIC.get())) {
                if (!jetpack.isEmpty()) return ItemStack.EMPTY;
                jetpack = current;
            } else {
                Equippable equippable = current.get(DataComponents.EQUIPPABLE);
                if (equippable != null && equippable.slot() == EquipmentSlot.CHEST
                        && !blacklist().contains(current.getItem())) {
                    if (!armor.isEmpty()) return ItemStack.EMPTY;
                    armor = current;
                } else {
                    if (!current.is(ModItems.JETPACK_ATTACHMENT_PLATE.get()) || plate)
                        return ItemStack.EMPTY;
                    plate = true;
                }
            }
        }
        if (jetpack.isEmpty() || armor.isEmpty() || !plate || JetpackAttachmentHelper.hasAttached(armor))
            return ItemStack.EMPTY;
        ItemStack result = armor.copy();
        result.set(ModDataComponents.JETPACK_ATTACHED.get(), true);
        double charge = ElectricItemEnergy.charge(jetpack);
        double capacity = ((JetpackElectricItem) ModArmor.JETPACK_ELECTRIC.get()).specification().capacity();
        if (result.getItem() instanceof ElectricItem) {
            ElectricItemEnergy.charge(result, charge, Integer.MAX_VALUE, true, false);
        } else if (charge > 0) {
            result.set(ModDataComponents.JETPACK_CHARGE.get(), Math.min(charge, capacity));
        }
        return result;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        return NonNullList.withSize(input.size(), ItemStack.EMPTY);
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public boolean showNotification() {
        return false;
    }

    @Override
    public String group() {
        return "";
    }

    @Override
    public CraftingBookCategory category() {
        return CraftingBookCategory.MISC;
    }

    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }

    @Override
    public List<RecipeDisplay> display() {
        return List.of();
    }

    @Override
    public RecipeSerializer<JetpackAttachmentRecipe> getSerializer() {
        return ic2.neoforge.registration.ModCraftingRecipes.JETPACK_ATTACHMENT.get();
    }
}
