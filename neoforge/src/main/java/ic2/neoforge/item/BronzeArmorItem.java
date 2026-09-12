package ic2.neoforge.item;

import net.minecraft.world.item.Item;

/**
 * Legacy ItemArmorIC2 (bronze set): a plain vanilla armor whose only custom behaviour is
 * IMetalArmor#isMetalArmor returning true, reproduced by the MetalArmorLike marker.
 */
public class BronzeArmorItem extends Item implements MetalArmorLike {
    public BronzeArmorItem(Item.Properties properties) {
        super(properties);
    }
}
