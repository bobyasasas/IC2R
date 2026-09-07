package ic2.neoforge.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** The IC2 shapeless recipe adapter returns this tool with one additional damage point. */
public class CraftingToolItem extends Item {
    public CraftingToolItem(Properties properties) {
        super(properties);
    }

    public ItemStack craftingRemainder(ItemStack input) {
        if (input.getDamageValue() + 1 >= input.getMaxDamage()) return ItemStack.EMPTY;
        var result = input.copyWithCount(1);
        result.setDamageValue(result.getDamageValue() + 1);
        return result;
    }
}
