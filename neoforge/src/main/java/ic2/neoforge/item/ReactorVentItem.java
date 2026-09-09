package ic2.neoforge.item;

import net.minecraft.world.item.ItemStack;

/**
 * Heat vent (legacy ItemReactorVent): pulls up to {@code reactorVent} heat from the core into its
 * own storage, then vents up to {@code selfVent} of its stored heat out of the reactor.
 */
public class ReactorVentItem extends HeatStorageComponent {
    private final int selfVent, reactorVent;

    public ReactorVentItem(Properties properties, int capacity, int selfVent, int reactorVent) {
        super(properties, capacity);
        this.selfVent = selfVent;
        this.reactorVent = reactorVent;
    }

    @Override
    public void processChamber(
            ItemStack stack, ReactorHost reactor, int x, int y, boolean heatRun) {
        if (!heatRun) return;
        if (reactorVent > 0) {
            int coreDrain = Math.min(reactor.getHeat(), reactorVent);
            int remainder = alterHeat(stack, reactor, x, y, coreDrain);
            if (remainder > 0) return;
            reactor.setHeat(reactor.getHeat() - coreDrain);
        }
        int self = alterHeat(stack, reactor, x, y, -selfVent);
        if (self <= 0) reactor.addEmitHeat(self + selfVent);
    }

    /** Vents up to {@code selfVent} stored heat; returns the amount actually vented. */
    public int dissipate(ItemStack stack) {
        return selfVent + alterHeat(stack, null, 0, 0, -selfVent);
    }
}
