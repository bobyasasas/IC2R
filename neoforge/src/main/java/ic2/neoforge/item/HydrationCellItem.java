package ic2.neoforge.item;

import ic2.neoforge.component.ModDataComponents;
import ic2.neoforge.crop.CropBlockEntity;

import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

/**
 * Hydration cell (legacy ItemHydrationCell): 10000 charges of irrigation for crop tiles. Machine
 * feeds draw at most 180 per pass; a hand use has no such cap.
 */
public class HydrationCellItem extends Item {
    public static final int CHARGES = 10000;

    public HydrationCellItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.getBlockEntity(context.getClickedPos()) instanceof CropBlockEntity crop) {
            ItemStack stack = context.getItemInHand();
            return applyToCrop(stack, crop, true)
                    ? InteractionResult.SUCCESS
                    : InteractionResult.PASS;
        }
        return InteractionResult.PASS;
    }

    /**
     * Legacy applyToCrop: one charge is spent even on a full tile, the tile takes what fits and
     * the cell is consumed once every charge is gone.
     */
    public boolean applyToCrop(ItemStack stack, CropBlockEntity crop, boolean manual) {
        int consumed = uses(stack) + 1;
        int amount = Math.max(0, CHARGES - consumed);
        if (!manual && amount > 180) amount = 180;
        amount = crop.applyHydration(amount, false);
        if (amount <= 0) return false;
        consumed += amount;
        if (consumed >= CHARGES) stack.shrink(1);
        else setUses(stack, consumed);
        return true;
    }

    private static int uses(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.HYDRATION_USES.get(), 0);
    }

    private static void setUses(ItemStack stack, int uses) {
        if (uses <= 0) stack.remove(ModDataComponents.HYDRATION_USES.get());
        else stack.set(ModDataComponents.HYDRATION_USES.get(), uses);
    }

    private static double chargeLevel(ItemStack stack) {
        return (CHARGES - uses(stack)) / (double) CHARGES;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return uses(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return (int) Math.round(chargeLevel(stack) * 13.0);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return Mth.hsvToRgb((float) (chargeLevel(stack) / 3.0), 1.0F, 1.0F);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> tooltip,
            TooltipFlag flag) {
        if (stack.getCount() == 1 && flag.isAdvanced()) {
            tooltip.accept(
                    Component.translatable("item.durability", CHARGES - uses(stack), CHARGES));
        }
    }
}
