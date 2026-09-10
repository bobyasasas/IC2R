package ic2.neoforge.item;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * A legacy ItemArmorHazmat piece: contributes protection in its slot without fully protecting on
 * its own. The four-piece set is the hazmat helmet, chestplate and leggings plus the rubber boots
 * (legacy registers the boots with the same armour class).
 */
public class HazmatArmorItem extends Item implements HazmatLike {
    public HazmatArmorItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean addsProtection(LivingEntity entity, EquipmentSlot slot, ItemStack stack) {
        return true;
    }
}
