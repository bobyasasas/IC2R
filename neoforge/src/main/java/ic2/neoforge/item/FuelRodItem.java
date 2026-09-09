package ic2.neoforge.item;

import ic2.core.reactor.ReactorMath;
import ic2.neoforge.component.ModDataComponents;
import ic2.neoforge.registration.ModReactorItems;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Nuclear fuel rod (uranium; MOX overrides the pulse maths separately). Implements the legacy pulse
 * algorithm: a pulse pass adds EU per pulse and counts neighbouring pulsable components, the heat
 * pass turns pulse counts into triangular heat distributed to adjacent heat acceptors, and each
 * cycle depletes the rod until it swaps to its depleted counterpart.
 */
public class FuelRodItem extends Item implements ReactorComponent {
    private final int cells;
    private final int duration;

    public FuelRodItem(Properties properties, int cells, int duration) {
        super(properties);
        this.cells = cells;
        this.duration = duration;
    }

    public int cells() {
        return cells;
    }

    public int duration() {
        return duration;
    }

    public int use(ItemStack stack) {
        return Math.clamp(stack.getOrDefault(ModDataComponents.REACTOR_USE, 0), 0, duration);
    }

    @Override
    public void processChamber(
            ItemStack stack, ReactorHost reactor, int x, int y, boolean heatRun) {
        if (!reactor.produceEnergy()) return;
        int basePulses = 1 + cells / 2;
        for (int iteration = 0; iteration < cells; iteration++) {
            int pulses = basePulses;
            if (!heatRun) {
                for (int pulse = 0; pulse < pulses; pulse++)
                    acceptUraniumPulse(stack, reactor, stack, x, y, x, y, heatRun);
                pulses += pulsableNeighbours(reactor, stack, x, y, heatRun);
            } else {
                pulses += pulsableNeighbours(reactor, stack, x, y, heatRun);
                int heat = ReactorMath.uraniumHeat(pulses);
                heat = getFinalHeat(stack, reactor, heat);
                List<ItemStack> acceptorStacks = new ArrayList<>();
                List<Integer> acceptorXs = new ArrayList<>();
                List<Integer> acceptorYs = new ArrayList<>();
                collectHeatAcceptors(reactor, x - 1, y, acceptorStacks, acceptorXs, acceptorYs);
                collectHeatAcceptors(reactor, x + 1, y, acceptorStacks, acceptorXs, acceptorYs);
                collectHeatAcceptors(reactor, x, y - 1, acceptorStacks, acceptorXs, acceptorYs);
                collectHeatAcceptors(reactor, x, y + 1, acceptorStacks, acceptorXs, acceptorYs);
                while (!acceptorStacks.isEmpty() && heat > 0) {
                    int share = heat / acceptorStacks.size();
                    heat -= share;
                    var acceptor = acceptorStacks.remove(acceptorStacks.size() - 1);
                    int ax = acceptorXs.remove(acceptorXs.size() - 1);
                    int ay = acceptorYs.remove(acceptorYs.size() - 1);
                    share = acceptorComponent(acceptor).alterHeat(acceptor, reactor, ax, ay, share);
                    heat += share;
                    // The acceptor stack is a working copy; store its absorbed heat back.
                    reactor.setItemAt(ax, ay, acceptor);
                }
                if (heat > 0) reactor.addHeat(heat);
            }
        }
        if (!heatRun) {
            if (use(stack) >= duration - 1) {
                reactor.setItemAt(x, y, depletedStack());
            } else {
                stack.set(ModDataComponents.REACTOR_USE, use(stack) + 1);
            }
        }
    }

    @Override
    public boolean acceptUraniumPulse(
            ItemStack stack,
            ReactorHost reactor,
            ItemStack pulsingStack,
            int youX,
            int youY,
            int pulseX,
            int pulseY,
            boolean heatRun) {
        if (!heatRun) reactor.addOutput(1.0F);
        return true;
    }

    /** MOX rods multiply heat when the reactor runs hot; uranium keeps the plain formula. */
    protected int getFinalHeat(ItemStack stack, ReactorHost reactor, int heat) {
        return heat;
    }

    protected ItemStack depletedStack() {
        return switch (cells) {
            case 1 -> ModReactorItems.DEPLETED_URANIUM_FUEL_ROD.get().getDefaultInstance();
            case 2 -> ModReactorItems.DEPLETED_DUAL_URANIUM_FUEL_ROD.get().getDefaultInstance();
            case 4 -> ModReactorItems.DEPLETED_QUAD_URANIUM_FUEL_ROD.get().getDefaultInstance();
            default -> throw new IllegalStateException("Invalid fuel rod cell count: " + cells);
        };
    }

    private int pulsableNeighbours(
            ReactorHost reactor, ItemStack rod, int x, int y, boolean heatRun) {
        return pulsable(reactor, rod, x - 1, y, x, y, heatRun)
                + pulsable(reactor, rod, x + 1, y, x, y, heatRun)
                + pulsable(reactor, rod, x, y - 1, x, y, heatRun)
                + pulsable(reactor, rod, x, y + 1, x, y, heatRun);
    }

    private int pulsable(
            ReactorHost reactor, ItemStack rod, int nx, int ny, int px, int py, boolean heatRun) {
        var other = reactor.getItemAt(nx, ny);
        if (other == null || !(other.getItem() instanceof ReactorComponent component)) return 0;
        if (!component.acceptUraniumPulse(other, reactor, rod, nx, ny, px, py, heatRun)) return 0;
        // Persist the neighbour's mutation (depleting reflectors) unless it consumed itself.
        var current = reactor.getItemAt(nx, ny);
        if (current.getItem() == other.getItem()
                && !net.minecraft.world.item.ItemStack.matches(current, other))
            reactor.setItemAt(nx, ny, other);
        return 1;
    }

    private static void collectHeatAcceptors(
            ReactorHost reactor,
            int x,
            int y,
            List<ItemStack> stacks,
            List<Integer> xs,
            List<Integer> ys) {
        var stack = reactor.getItemAt(x, y);
        if (stack != null
                && stack.getItem() instanceof ReactorComponent component
                && component.canStoreHeat(stack, reactor, x, y)) {
            stacks.add(stack);
            xs.add(x);
            ys.add(y);
        }
    }

    private ReactorComponent acceptorComponent(ItemStack stack) {
        return (ReactorComponent) stack.getItem();
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> tooltip,
            TooltipFlag flag) {
        tooltip.accept(
                Component.translatable(
                        "ic2.reactoritem.durability", duration - use(stack), duration));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        double fraction = (double) use(stack) / duration;
        return (int) Math.round(Math.clamp(fraction, 0.0, 1.0) * 13.0);
    }
}
