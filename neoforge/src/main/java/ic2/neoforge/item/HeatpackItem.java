package ic2.neoforge.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Heat pack (legacy ItemReactorHeatpack, 1000 per stack unit / 1 heat per unit): tops up the heat
 * storage of its orthogonal neighbours straight from the core while the core stays below {@code
 * maxPer x stackSize}.
 */
public class HeatpackItem extends Item implements ReactorComponent {
    private final int maxPer, heatPer;

    public HeatpackItem(Properties properties, int maxPer, int heatPer) {
        super(properties);
        this.maxPer = maxPer;
        this.heatPer = heatPer;
    }

    @Override
    public void processChamber(
            ItemStack stack, ReactorHost reactor, int x, int y, boolean heatRun) {
        if (!heatRun) return;
        warm(reactor, stack.getCount(), x + 1, y);
        warm(reactor, stack.getCount(), x - 1, y);
        warm(reactor, stack.getCount(), x, y + 1);
        warm(reactor, stack.getCount(), x, y - 1);
    }

    private void warm(ReactorHost reactor, int size, int x, int y) {
        int want = maxPer * size;
        if (reactor.getHeat() >= want) return;
        var stack = reactor.getItemAt(x, y);
        if (stack == null || !(stack.getItem() instanceof ReactorComponent component)) return;
        if (!component.canStoreHeat(stack, reactor, x, y)) return;
        int add = heatPer * size;
        int current = component.getCurrentHeat(stack, reactor, x, y);
        if (add > want - current) add = want - current;
        int remainder = component.alterHeat(stack, reactor, x, y, add);
        reactor.setItemAt(x, y, stack);
        if (remainder > 0) reactor.setHeat(reactor.getHeat() - (add - remainder));
    }
}
