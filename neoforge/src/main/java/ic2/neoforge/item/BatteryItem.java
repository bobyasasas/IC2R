package ic2.neoforge.item;

import ic2.core.energy.ElectricItemSpec;
import ic2.neoforge.registration.ModSounds;

import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class BatteryItem extends ElectricItem {
    public BatteryItem(Properties properties, ElectricItemSpec specification) {
        super(properties, specification);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack source = player.getItemInHand(hand);
        if (level.isClientSide() || source.getCount() != 1) return InteractionResult.PASS;
        boolean changed = false;
        for (int slot = 0; slot < 9; slot++) {
            ItemStack target = player.getInventory().getItem(slot);
            if (source == target
                    || ElectricItemEnergy.discharge(
                                    target,
                                    Double.POSITIVE_INFINITY,
                                    Integer.MAX_VALUE,
                                    true,
                                    true,
                                    true)
                            > 0) continue;
            double available =
                    ElectricItemEnergy.discharge(
                            source,
                            2 * specification().transferLimit(),
                            Integer.MAX_VALUE,
                            true,
                            true,
                            true);
            double accepted =
                    ElectricItemEnergy.charge(
                            target, available, specification().tier(), true, false);
            ElectricItemEnergy.discharge(source, accepted, Integer.MAX_VALUE, true, true, false);
            changed |= accepted > 0;
        }
        if (changed) {
            player.containerMenu.broadcastChanges();
            level.playSound(
                    null,
                    player.blockPosition(),
                    ModSounds.ITEM_BATTERY_USE.get(),
                    SoundSource.PLAYERS,
                    1,
                    1);
        }
        return InteractionResult.SUCCESS;
    }
}
