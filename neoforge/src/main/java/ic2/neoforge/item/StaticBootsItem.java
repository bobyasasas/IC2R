package ic2.neoforge.item;

import net.minecraft.world.item.Item;

/**
 * Legacy ItemArmorStaticBoots: a plain utility boots piece (no own energy storage) whose tick
 * charges whatever electric piece is worn on the chest from walking; the logic lives in
 * {@link UtilityArmorHelper} with the walk markers on the {@code static_boots_x/z} components.
 */
public class StaticBootsItem extends Item {
    public StaticBootsItem(Properties properties) {
        super(properties);
    }
}
