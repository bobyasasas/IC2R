package ic2.neoforge.item;

import ic2.neoforge.entity.DynamiteEntity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Shared throw path for the plain and sticky dynamite items (legacy ItemDynamite.use). */
final class ThrownDynamite {
    static InteractionResult throwCharge(
            Level level, Player player, InteractionHand hand, Item item, boolean sticky) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.ARROW_SHOOT,
                SoundSource.PLAYERS,
                0.5F,
                0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.addFreshEntity(DynamiteEntity.thrown(serverLevel, player, sticky));
        }

        player.awardStat(Stats.ITEM_USED.get(item));
        stack.consume(1, player);
        return InteractionResult.SUCCESS;
    }

    private ThrownDynamite() {}
}
