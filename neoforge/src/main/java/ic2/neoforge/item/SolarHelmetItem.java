package ic2.neoforge.item;

import net.minecraft.world.item.Item;

/**
 * Legacy ItemArmorSolarHelmet: a plain utility helmet (no own energy storage) whose tick charges
 * whatever electric piece is worn on the chest from sunlight; the logic lives in
 * {@link UtilityArmorHelper} so the item is only the wearable marker with its material defence.
 */
public class SolarHelmetItem extends Item {
    public SolarHelmetItem(Properties properties) {
        super(properties);
    }
}
