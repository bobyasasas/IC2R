package ic2.neoforge.item;


import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * Dynamite stick item (legacy ItemDynamite): placing behaves like a normal block item, using it in
 * the air throws a primed charge with a hundred-tick fuse. Stacks to sixteen.
 */
public class DynamiteItem extends BlockItem {
    public DynamiteItem(Block block, Properties properties) {
        super(block, properties.stacksTo(16));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        return ThrownDynamite.throwCharge(level, player, hand, this, false);
    }
}
