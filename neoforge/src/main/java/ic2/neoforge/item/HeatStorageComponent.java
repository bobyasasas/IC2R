package ic2.neoforge.item;

import ic2.neoforge.component.ModDataComponents;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/**
 * Heat-storing reactor component (coolant cells, vents, heat switches): the stored heat lives in
 * the {@code ic2:reactor_heat} component and {@link #alterHeat} returns the unabsorbed remainder.
 */
public class HeatStorageComponent extends Item implements ReactorComponent {
    protected final int capacity;

    public HeatStorageComponent(Properties properties, int capacity) {
        super(properties.stacksTo(1));
        this.capacity = capacity;
    }

    public int capacity() {
        return capacity;
    }

    public int currentHeat(ItemStack stack) {
        return Math.clamp(stack.getOrDefault(ModDataComponents.REACTOR_HEAT, 0), 0, capacity);
    }

    @Override
    public boolean canStoreHeat(ItemStack stack, ReactorHost reactor, int x, int y) {
        return currentHeat(stack) < capacity;
    }

    @Override
    public int getCurrentHeat(ItemStack stack, ReactorHost reactor, int x, int y) {
        return currentHeat(stack);
    }

    @Override
    public int getMaxHeat(ItemStack stack, ReactorHost reactor, int x, int y) {
        return capacity;
    }

    /** Legacy-name alias of {@link #alterHeat}: returns the unabsorbed remainder. */
    public int exchangeHeat(ItemStack stack, int amount) {
        return alterHeat(stack, null, 0, 0, amount);
    }

    /** Legacy-name record of the stored heat for tooltip/box reads. */
    public ic2.core.reactor.ReactorHeat heat(ItemStack stack) {
        return new ic2.core.reactor.ReactorHeat(capacity, currentHeat(stack));
    }

    @Override
    public int alterHeat(ItemStack stack, ReactorHost reactor, int x, int y, int heat) {
        int current = currentHeat(stack);
        int next = Math.clamp(current + heat, 0, capacity);
        if (next == 0) stack.remove(ModDataComponents.REACTOR_HEAT);
        else stack.set(ModDataComponents.REACTOR_HEAT, next);
        return heat - (next - current);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> tooltip,
            TooltipFlag flag) {
        tooltip.accept(
                Component.translatable("ic2.reactoritem.durability",
                        capacity - currentHeat(stack), capacity));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        double fraction = (double) currentHeat(stack) / capacity;
        return (int) Math.round(Math.clamp(fraction, 0.0, 1.0) * 13.0);
    }
}
