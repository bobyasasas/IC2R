package ic2.neoforge.item;

import net.minecraft.world.item.Item;

/**
 * Legacy ItemArmorIC2 (alloy set): a plain vanilla armor whose only custom behaviour is
 * IMetalArmor#isMetalArmor returning true, reproduced by the MetalArmorLike marker.
 */
public class AlloyArmorItem extends Item implements MetalArmorLike {
    public AlloyArmorItem(Item.Properties properties) {
        super(properties);
    }
}
