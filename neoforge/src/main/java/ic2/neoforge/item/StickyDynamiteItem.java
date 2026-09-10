package ic2.neoforge.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * Sticky dynamite item (legacy ItemDynamite with sticky=true): it cannot be placed as a block, so
 * every use throws the anchoring charge instead. Stacks to sixteen.
 */
public class StickyDynamiteItem extends Item {
    public StickyDynamiteItem(Properties properties) {
        super(properties.stacksTo(16));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        return ThrownDynamite.throwCharge(level, player, hand, this, true);
    }
}
