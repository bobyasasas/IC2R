package ic2.neoforge.item;

import ic2.core.energy.ElectricTransfers;
import ic2.neoforge.component.ModDataComponents;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/** Component-backed adapter. Simulation and queries never change a stack. */
public final class ElectricItemEnergy {
    private static final EquipmentSlot[] ARMOR_SLOTS = {
        EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

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

    /**
     * Legacy manager.use: spend one operation from the stack, first topping it up from worn
     * electric armor (batpack family) and topping it up again afterwards, like the manager did.
     */
    public static boolean use(ItemStack stack, double amount, @Nullable LivingEntity entity) {
        if (entity != null) chargeFromArmor(stack, entity);
        double available = discharge(stack, amount, Integer.MAX_VALUE, true, false, true);
        if (available < amount) return false;
        discharge(stack, amount, Integer.MAX_VALUE, true, false, false);
        if (entity != null) chargeFromArmor(stack, entity);
        return true;
    }

    /**
     * Legacy chargeFromArmor: pull the stored energy of every worn electric piece into the target.
     * The armor's tier gates which devices it can feed; transfer limits are ignored both ways.
     */
    private static void chargeFromArmor(ItemStack target, LivingEntity entity) {
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack source = entity.getItemBySlot(slot);
            if (!(source.getItem() instanceof ElectricItem item)) continue;
            double transfer =
                    discharge(source, Double.POSITIVE_INFINITY, Integer.MAX_VALUE, true, true, true);
            if (transfer <= 0) continue;
            transfer = charge(target, transfer, item.specification().tier(), true, false);
            if (transfer > 0) {
                discharge(source, transfer, Integer.MAX_VALUE, true, true, false);
            }
        }
    }

    private static void apply(ItemStack stack, ElectricTransfers.Result result) {
        if (result.stored() == 0) stack.remove(ModDataComponents.CHARGE.get());
        else stack.set(ModDataComponents.CHARGE.get(), result.stored());
    }
}
