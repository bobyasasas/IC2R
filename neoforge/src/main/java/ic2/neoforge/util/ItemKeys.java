package ic2.neoforge.util;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

/** Registry-id helpers shared by value/index lookups. */
public final class ItemKeys {
    public static String id(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).toString();
    }

    public static String id(Holder<Item> holder) {
        return id(holder.value());
    }

    private ItemKeys() {}
}
