package ic2.neoforge.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Component heat vent (legacy ItemReactorVentSpread): cools each orthogonal neighbour. */
public class VentSpreadItem extends Item implements ReactorComponent {
    private final int sideVent;

    public VentSpreadItem(Properties properties, int sideVent) {
        super(properties);
        this.sideVent = sideVent;
    }

    @Override
    public void processChamber(
            ItemStack stack, ReactorHost reactor, int x, int y, boolean heatRun) {
        if (!heatRun) return;
        cool(reactor, x - 1, y);
        cool(reactor, x + 1, y);
        cool(reactor, x, y - 1);
        cool(reactor, x, y + 1);
    }

    private void cool(ReactorHost reactor, int x, int y) {
        var stack = reactor.getItemAt(x, y);
        if (stack != null
                && stack.getItem() instanceof ReactorComponent component
                && component.canStoreHeat(stack, reactor, x, y)) {
            int self = component.alterHeat(stack, reactor, x, y, -sideVent);
            if (self <= 0) reactor.addEmitHeat(self + sideVent);
            // The neighbour stack is a working copy; store its cooled state back.
            reactor.setItemAt(x, y, stack);
        }
    }
}
