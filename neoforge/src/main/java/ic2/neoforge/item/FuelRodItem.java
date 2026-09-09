package ic2.neoforge.item;

import ic2.neoforge.component.ModDataComponents;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/**
 * Nuclear fuel rod (uranium or MOX, one to four cells): stores depletion in the {@code
 * ic2:reactor_use} component while it runs in a reactor, and crafting recipes that build dual or
 * quad rods only accept fresh (use 0) rods. In-reactor pulses land with the reactor itself.
 */
public final class FuelRodItem extends Item {
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
