package ic2.neoforge.item;

import ic2.core.reactor.ReactorHeat;
import ic2.neoforge.component.ModDataComponents;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Component heat is distinct from tool damage; vanilla repairs cannot erase reactor heat. */
public final class ReactorHeatItem extends Item {
    private final int capacity, cooling;

    public ReactorHeatItem(Properties properties, int capacity, int cooling) {
        super(properties.stacksTo(1));
        if (capacity <= 0 || cooling < 0)
            throw new IllegalArgumentException("Invalid heat component");
        this.capacity = capacity;
        this.cooling = cooling;
    }

    public ReactorHeat heat(ItemStack stack) {
        return new ReactorHeat(
                capacity,
                Math.clamp(stack.getOrDefault(ModDataComponents.REACTOR_HEAT, 0), 0, capacity));
    }

    /** The reactor host must resolve positive overflow before continuing the heat pass. */
    public int exchangeHeat(ItemStack stack, int amount) {
        if (!stack.is(this) || stack.getCount() != 1)
            throw new IllegalArgumentException("Heat exchange requires one matching component");
        var exchange = heat(stack).exchange(amount);
        if (exchange.heat().stored() == 0) stack.remove(ModDataComponents.REACTOR_HEAT);
        else stack.set(ModDataComponents.REACTOR_HEAT, exchange.heat().stored());
        return exchange.remainder();
    }

    /** Returns only the heat actually dissipated; the host accounts it as emitted heat. */
    public int dissipate(ItemStack stack) {
        return cooling + exchangeHeat(stack, -cooling);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return heat(stack).stored() > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13f * (capacity - heat(stack).stored()) / capacity);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return net.minecraft.util.Mth.hsvToRgb(
                (1f - (float) heat(stack).stored() / capacity) / 3f, 1, 1);
    }
}
