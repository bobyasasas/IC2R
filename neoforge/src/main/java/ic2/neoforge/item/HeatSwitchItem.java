package ic2.neoforge.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;


import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.item.ItemStack;

/**
 * Heat exchanger (legacy ItemReactorHeatSwitch): balances heat between itself, its orthogonal
 * neighbours and the core using the legacy media-percentage pacing.
 */
public class HeatSwitchItem extends HeatStorageComponent {
    private final int switchSide, switchReactor;

    public HeatSwitchItem(Properties properties, int capacity, int switchSide, int switchReactor) {
        super(properties, capacity);
        this.switchSide = switchSide;
        this.switchReactor = switchReactor;
    }

    @Override
    public void processChamber(
            ItemStack stack, ReactorHost reactor, int x, int y, boolean heatRun) {
        if (!heatRun) return;
        int myHeat = currentHeat(stack);
        List<ItemStack> acceptorStacks = new ArrayList<>();
        List<Integer> acceptorXs = new ArrayList<>();
        List<Integer> acceptorYs = new ArrayList<>();
        if (switchSide > 0) {
            collect(reactor, x - 1, y, acceptorStacks, acceptorXs, acceptorYs);
            collect(reactor, x + 1, y, acceptorStacks, acceptorXs, acceptorYs);
            collect(reactor, x, y - 1, acceptorStacks, acceptorXs, acceptorYs);
            collect(reactor, x, y + 1, acceptorStacks, acceptorXs, acceptorYs);
            for (int index = 0; index < acceptorStacks.size(); index++) {
                var acceptor = acceptorStacks.get(index);
                int ax = acceptorXs.get(index);
                int ay = acceptorYs.get(index);
                var component = (ReactorComponent) acceptor.getItem();
                double myMedia = myHeat * 100.0 / capacity;
                double theirMedia = component.getCurrentHeat(acceptor, reactor, ax, ay) * 100.0
                        / component.getMaxHeat(acceptor, reactor, ax, ay);
                int add = pacedTransfer(theirMedia, myMedia, switchSide,
                        component.getMaxHeat(acceptor, reactor, ax, ay));
                myHeat -= add;
                int remainder = component.alterHeat(acceptor, reactor, ax, ay, add);
                myHeat += remainder;
                reactor.setItemAt(ax, ay, acceptor);
            }
        }
        if (switchReactor > 0) {
            double myMedia = myHeat * 100.0 / capacity;
            double coreMedia = reactor.getHeat() * 100.0 / reactor.getMaxHeat();
            int add = pacedTransfer(coreMedia, myMedia, switchReactor, reactor.getMaxHeat());
            myHeat -= add;
            reactor.setHeat(reactor.getHeat() + add);
        }
        alterHeat(stack, reactor, x, y, myHeat - currentHeat(stack));
        reactor.setItemAt(x, y, stack);
    }

    /** Legacy pacing: media percentages halve the transfer down to a single unit. */
    private static int pacedTransfer(double theirMedia, double myMedia, int switchRate,
            int theirMaxHeat) {
        int add = (int) (theirMaxHeat / 100.0 * (theirMedia + myMedia / 2.0));
        if (add > switchRate) add = switchRate;
        if (theirMedia + myMedia / 2.0 < 1.0) add = switchRate / 2;
        if (theirMedia + myMedia / 2.0 < 0.75) add = switchRate / 4;
        if (theirMedia + myMedia / 2.0 < 0.5) add = switchRate / 8;
        if (theirMedia + myMedia / 2.0 < 0.25) add = 1;
        if (Math.round(theirMedia * 10.0) / 10.0 > Math.round(myMedia * 10.0) / 10.0) add -= 2 * add;
        else if (Math.round(theirMedia * 10.0) / 10.0 == Math.round(myMedia * 10.0) / 10.0) add = 0;
        return add;
    }

    private static void collect(ReactorHost reactor, int x, int y,
            List<ItemStack> stacks, List<Integer> xs, List<Integer> ys) {
        var stack = reactor.getItemAt(x, y);
        if (stack != null && stack.getItem() instanceof ReactorComponent component
                && component.canStoreHeat(stack, reactor, x, y)) {
            stacks.add(stack);
            xs.add(x);
            ys.add(y);
        }
    }
}
