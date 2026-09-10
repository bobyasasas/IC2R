package ic2.neoforge.item;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Legacy IHazmatLike: a piece that can contribute to full radiation-suit protection. A wearer
 * counts as protected once every armour slot holds a piece with {@code addsProtection} and any
 * piece reports {@code fullyProtects}.
 */
public interface HazmatLike {
    boolean addsProtection(LivingEntity entity, EquipmentSlot slot, ItemStack stack);

    default boolean fullyProtects(LivingEntity entity, EquipmentSlot slot, ItemStack stack) {
        return false;
    }

    static boolean hasCompleteHazmat(LivingEntity living) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR) continue;
            ItemStack stack = living.getItemBySlot(slot);
            if (stack.isEmpty() || !(stack.getItem() instanceof HazmatLike hazmat)) return false;
            if (!hazmat.addsProtection(living, slot, stack)) return false;
            if (hazmat.fullyProtects(living, slot, stack)) return true;
        }
        return true;
    }
}
