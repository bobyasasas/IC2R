package ic2.neoforge.item;

import ic2.neoforge.component.ModDataComponents;

import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.neoforged.neoforge.fluids.FluidStackTemplate;

import java.util.function.Consumer;

/**
 * Wearable construction foam tank for the chest slot; the sprayer drains it first. The legacy armor
 * material loses no durability (multiplier zero) and grants eight armor points on the chest.
 */
public class CFPackItem extends Item {
    public static final int CAPACITY_MB = 80000;

    public CFPackItem(Properties properties) {
        super(properties);
    }

    public static int getContentsMb(ItemStack stack) {
        FluidStackTemplate stored = stack.get(ModDataComponents.FLUID);
        return stored == null ? 0 : Math.min(CAPACITY_MB, stored.amount());
    }

    public static int drainMb(ItemStack stack, int amount) {
        int contents = getContentsMb(stack);
        int drained = Math.min(contents, amount);
        int remaining = contents - drained;
        if (remaining <= 0) {
            stack.remove(ModDataComponents.FLUID);
        } else {
            stack.set(
                    ModDataComponents.FLUID,
                    new FluidStackTemplate(FoamSprayerItem.foamFluid(), remaining));
        }
        return drained;
    }

    public static void fillMb(ItemStack stack) {
        stack.set(
                ModDataComponents.FLUID,
                new FluidStackTemplate(FoamSprayerItem.foamFluid(), CAPACITY_MB));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(getContentsMb(stack) / (float) CAPACITY_MB * 13.0F);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        float level = getContentsMb(stack) / (float) CAPACITY_MB;
        return Mth.hsvToRgb(level / 3.0F, 1.0F, 1.0F);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> tooltip,
            TooltipFlag flag) {
        int contents = getContentsMb(stack);
        if (contents > 0) {
            tooltip.accept(
                    Component.translatable(
                            "ic2.item.fluid_container.with_fluid",
                            FoamSprayerItem.foamFluid().getFluidType().getDescription(),
                            contents));
        } else {
            tooltip.accept(Component.translatable("ic2.item.fluid_container.empty"));
        }
    }
}
