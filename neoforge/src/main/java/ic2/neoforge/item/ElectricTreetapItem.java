package ic2.neoforge.item;

import ic2.core.energy.ElectricItemSpec;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;

public final class ElectricTreetapItem extends ElectricItem {
    public ElectricTreetapItem(Properties properties) {
        super(properties, new ElectricItemSpec(10000, 100, 1, false));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (ElectricItemEnergy.charge(context.getItemInHand()) < 50
                || !TreetapActions.extract(context, true)) return InteractionResult.PASS;
        if (!context.getLevel().isClientSide())
            ElectricItemEnergy.use(context.getItemInHand(), 50, context.getPlayer());
        return InteractionResult.SUCCESS;
    }
}
