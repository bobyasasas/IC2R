package ic2.neoforge.item;

import ic2.core.energy.ElectricItemSpec;
import ic2.neoforge.component.ModDataComponents;

import java.util.Locale;
import java.util.function.Consumer;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * Legacy ItemBatteryChargeHotbar: a rechargeable battery that keeps the other hotbar stacks
 * topped up while carried. Its mode cycles on right click between enabled, disabled and
 * not-in-hand (also enabled, but never feeding the held stack); sibling charging batteries are
 * never drained into.
 */
public class ChargingBatteryItem extends BatteryItem {
    public ChargingBatteryItem(Properties properties, ElectricItemSpec specification) {
        super(properties, specification);
    }

    public enum Mode {
        ENABLED(true),
        DISABLED(false),
        NOT_IN_HAND(true);

        public final boolean enabled;

        Mode(boolean enabled) {
            this.enabled = enabled;
        }
    }

    @Override
    public void inventoryTick(
            ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        super.inventoryTick(stack, level, owner, slot);
        Mode mode = mode(stack);
        if (!mode.enabled
                || !(owner instanceof Player player)
                || level.getGameTime() % 10L >= specification().tier()) {
            return;
        }
        double limit = specification().transferLimit();
        int tier = specification().tier();
        for (int slotIndex = 0; slotIndex < 9 && limit > 0; slotIndex++) {
            ItemStack target = player.getInventory().getItem(slotIndex);
            if (target.isEmpty()
                    || (mode == Mode.NOT_IN_HAND && slotIndex == player.getInventory().getSelectedSlot())
                    || target.getItem() instanceof ChargingBatteryItem) {
                continue;
            }
            double accepted = ElectricItemEnergy.charge(target, limit, tier, false, true);
            accepted = ElectricItemEnergy.discharge(stack, accepted, tier, true, false, false);
            ElectricItemEnergy.charge(target, accepted, tier, true, false);
            limit -= accepted;
        }
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) return InteractionResult.PASS;
        Mode mode = mode(stack);
        mode = Mode.values()[(mode.ordinal() + 1) % Mode.values().length];
        setMode(stack, mode);
        player.sendSystemMessage(
                Component.translatable(
                        "ic2.tooltip.mode",
                        Component.translatable("ic2.tooltip.mode." + mode.name().toLowerCase(Locale.ENGLISH))));
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> tooltip,
            TooltipFlag flag) {
        Mode mode = mode(stack);
        tooltip.accept(
                Component.translatable(
                        "ic2.tooltip.mode",
                        Component.translatable("ic2.tooltip.mode." + mode.name().toLowerCase(Locale.ENGLISH))));
    }

    public static Mode mode(ItemStack stack) {
        int mode = stack.getOrDefault(ModDataComponents.BATTERY_MODE.get(), 0);
        Mode[] values = Mode.values();
        if (mode < 0 || mode >= values.length) return Mode.ENABLED;
        return values[mode];
    }

    private static void setMode(ItemStack stack, Mode mode) {
        stack.set(ModDataComponents.BATTERY_MODE.get(), mode.ordinal());
    }
}
