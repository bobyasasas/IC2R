package ic2.neoforge.item;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Legacy IMetalArmor: IC2 metal armors (bronze and friends) register through this marker, and the
 * eating plant spares wearers its status effects. The hazmat suit is rubber and deliberately does
 * not qualify.
 */
public interface MetalArmorLike {
    /** Legacy IMetalArmor#isMetalArmor: every registered IC2 metal armor item qualifies. */
    default boolean isMetalArmor(ItemStack stack, Player player) {
        return true;
    }
}
