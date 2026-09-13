package ic2.neoforge.item;

import ic2.neoforge.component.ModDataComponents;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/**
 * Reactor heat storage (RSH 20,000 / LZH 100,000). In the grid it soaks heat from neighbouring
 * rods but never discharges any; when full it simply refuses more. Gradual crafting recipes vent
 * the stored heat with redstone or lapis.
 */
public final class CondensatorItem extends Item implements ReactorComponent {
    private final int maxUse;

    public CondensatorItem(Properties properties, int maxUse) {
        super(properties);
        this.maxUse = maxUse;
    }

    public int maxUse() {
        return maxUse;
    }

    public int storedHeat(ItemStack stack) {
        return Math.clamp(stack.getOrDefault(ModDataComponents.REACTOR_HEAT, 0), 0, maxUse);
    }

    @Override
    public boolean canStoreHeat(ItemStack stack, ReactorHost reactor, int x, int y) {
        return storedHeat(stack) < maxUse;
    }

    @Override
    public int getCurrentHeat(ItemStack stack, ReactorHost reactor, int x, int y) {
        return storedHeat(stack);
    }

    @Override
    public int getMaxHeat(ItemStack stack, ReactorHost reactor, int x, int y) {
        return maxUse;
    }

    @Override
    public int alterHeat(ItemStack stack, ReactorHost reactor, int x, int y, int heat) {
        // Legacy ItemReactorCondensator: a condensator only soaks, a discharge passes through.
        if (heat < 0) return heat;
        int absorbed = Math.min(heat, maxUse - storedHeat(stack));
        stack.set(ModDataComponents.REACTOR_HEAT, storedHeat(stack) + absorbed);
        return heat - absorbed;
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
                        "ic2.reactoritem.durability", maxUse - storedHeat(stack), maxUse));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        double fraction = (double) storedHeat(stack) / maxUse;
        return (int) Math.round(Math.clamp(fraction, 0.0, 1.0) * 13.0);
    }
}
