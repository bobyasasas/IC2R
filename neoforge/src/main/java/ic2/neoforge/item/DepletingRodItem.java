package ic2.neoforge.item;

import ic2.neoforge.component.ModDataComponents;
import ic2.neoforge.registration.ModItems;
import ic2.neoforge.registration.MaterialDefinition;
import ic2.neoforge.registration.ModReactorItems;

import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Legacy ItemReactorLithiumCell / ItemReactorDepletedUranium: a damage-bar rod that only acts on
 * the heat pass, absorbing {@code extraUse + reactorHeat / 3000} per pulse and swapping to its
 * depleted counterpart at {@code maxUse}. Unlike the shared base class, both rods invert the use
 * fraction, so their bar and tooltip report how full the rod is.
 */
public class DepletingRodItem extends Item implements ReactorComponent {
    private final int maxUse;
    private final int extraUse;
    private final Supplier<ItemStack> depleted;

    public DepletingRodItem(Properties properties, int maxUse, int extraUse, Supplier<ItemStack> depleted) {
        super(properties.component(ModDataComponents.REACTOR_USE.get(), 0));
        this.maxUse = maxUse;
        this.extraUse = extraUse;
        this.depleted = depleted;
    }

    public int use(ItemStack stack) {
        return Math.clamp(stack.getOrDefault(ModDataComponents.REACTOR_USE, 0), 0, maxUse);
    }

    private double useFraction(ItemStack stack) {
        return Mth.clamp((double) use(stack) / maxUse, 0.0, 1.0);
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
        if (heatRun) {
            int myLevel = use(stack) + extraUse + reactor.getHeat() / 3000;
            if (myLevel >= maxUse) {
                reactor.setItemAt(youX, youY, depleted.get());
            } else {
                stack.set(ModDataComponents.REACTOR_USE, myLevel);
            }
        }
        return true;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> tooltip,
            TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        tooltip.accept(Component.translatable(
                "ic2.reactoritem.durability", maxUse - use(stack), maxUse));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return (int) Math.round(useFraction(stack) * 13.0);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        // Legacy inverts getUseFraction for these rods, so width and hue both track fullness.
        return Mth.hsvToRgb((float) (useFraction(stack) / 3.0), 1.0F, 1.0F);
    }

    /** The lithium cell fills toward a tritium rod (legacy swaps via Ic2Items.TRITIUM_FUEL_ROD). */
    public static DepletingRodItem lithium(Properties properties) {
        return new DepletingRodItem(
                properties, 10000, 0, () -> new ItemStack(ModItems.MATERIALS.get(
                        MaterialDefinition.TRITIUM_FUEL_ROD).get()));
    }

    /** The depleted isotope re-enriches toward legacy Ic2Items.RE_ENRICHED_URANIUM. */
    public static DepletingRodItem depletedIsotope(Properties properties) {
        return new DepletingRodItem(
                properties, 10000, 1, () -> new ItemStack(ModReactorItems.RE_ENRICHED_URANIUM.get()));
    }
}
