package ic2.neoforge.item;

import ic2.core.energy.ElectricTransfers;
import ic2.neoforge.component.ModDataComponents;

import net.minecraft.world.item.ItemStack;

/** Component-backed adapter. Simulation and queries never change a stack. */
public final class ElectricItemEnergy {
    private ElectricItemEnergy() {}

    public static double charge(ItemStack stack) {
        if (!(stack.getItem() instanceof ElectricItem item)) return 0;
        return Math.clamp(
                stack.getOrDefault(ModDataComponents.CHARGE.get(), 0.0),
                0,
                item.specification().capacity());
    }

    public static double charge(
            ItemStack stack, double requested, int tier, boolean ignoreLimit, boolean simulate) {
        if (stack.getCount() != 1 || !(stack.getItem() instanceof ElectricItem item)) return 0;
        var result =
                ElectricTransfers.charge(
                        charge(stack), item.specification(), requested, tier, ignoreLimit);
        if (!simulate) apply(stack, result);
        return result.transferred();
    }

    public static double discharge(
            ItemStack stack,
            double requested,
            int tier,
            boolean ignoreLimit,
            boolean external,
            boolean simulate) {
        if (stack.getCount() != 1 || !(stack.getItem() instanceof ElectricItem item)) return 0;
        var result =
                ElectricTransfers.discharge(
                        charge(stack),
                        item.specification(),
                        requested,
                        tier,
                        ignoreLimit,
                        external);
        if (!simulate) apply(stack, result);
        return result.transferred();
    }

    private static void apply(ItemStack stack, ElectricTransfers.Result result) {
        if (result.stored() == 0) stack.remove(ModDataComponents.CHARGE.get());
        else stack.set(ModDataComponents.CHARGE.get(), result.stored());
    }
}
