package ic2.neoforge.item;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

public final class TreetapItem extends Item {
    public TreetapItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!TreetapActions.extract(context, false)) return InteractionResult.PASS;
        if (!context.getLevel().isClientSide())
            context.getItemInHand().hurtAndBreak(1, context.getPlayer(), context.getHand());
        return InteractionResult.SUCCESS;
    }
}
