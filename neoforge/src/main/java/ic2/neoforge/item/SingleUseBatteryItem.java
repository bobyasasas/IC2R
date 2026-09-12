package ic2.neoforge.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Legacy ItemBatterySU: a disposable cell that dumps its whole capacity into the other hotbar
 * stacks on right click and is consumed only when at least some energy found a home.
 */
public class SingleUseBatteryItem extends Item {
    public static final int CAPACITY = 1200;
    public static final int TIER = 1;

    public SingleUseBatteryItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack source = player.getItemInHand(hand);
        if (level.isClientSide()) return InteractionResult.PASS;
        double energy = CAPACITY;
        for (int slot = 0; slot < 9 && energy > 0; slot++) {
            ItemStack target = player.getInventory().getItem(slot);
            if (target == source) continue;
            energy -= ElectricItemEnergy.charge(target, energy, TIER, true, false);
        }
        if (Math.abs(energy - CAPACITY) >= 1.0E-5) {
            source.shrink(1);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
