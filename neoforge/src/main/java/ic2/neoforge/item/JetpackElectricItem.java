package ic2.neoforge.item;

import ic2.core.energy.ElectricItemSpec;
import ic2.neoforge.component.ModDataComponents;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

/**
 * Legacy ItemArmorJetpackElectric: 30000 EU at tier 1 powering thrust 0.7, a 5% drop ramp, hover
 * multiplier 0.1 and the 1.28 world height divisor; each airborne flight tick pays the amount plus
 * the legacy +6 quirk. Flight arms through sneak + use (legacy read the client jump/hover keys).
 */
public class JetpackElectricItem extends ElectricItem implements JetpackLike {
    public JetpackElectricItem(Properties properties, ElectricItemSpec specification) {
        super(properties, specification);
    }

    @Override
    public float jetpackPower(ItemStack stack) {
        return 0.7F;
    }

    @Override
    public float dropPercentage() {
        return 0.05F;
    }

    @Override
    public float hoverMultiplier() {
        return 0.1F;
    }

    @Override
    public float worldHeightDivisor() {
        return 1.28F;
    }

    @Override
    public double chargeLevel(ItemStack stack) {
        return ElectricItemEnergy.charge(stack) / this.specification().capacity();
    }

    @Override
    public void drainFlightEnergy(ItemStack stack, int amount) {
        ElectricItemEnergy.discharge(stack, amount + 6, Integer.MAX_VALUE, true, false, false);
    }

    @Override
    public boolean isJetpackActive(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.JETPACK_ACTIVE.get(), false);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide()) {
                boolean enabled = !this.isJetpackActive(stack);
                stack.set(ModDataComponents.JETPACK_ACTIVE.get(), enabled);
                player.sendSystemMessage(
                        Component.translatable(
                                enabled
                                        ? "ic2.hover_mode.enabled"
                                        : "ic2.hover_mode.disabled"));
            }
            return InteractionResult.SUCCESS;
        }
        return super.use(level, player, hand);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> tooltip,
            TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        tooltip.accept(
                Component.translatable(
                        "item.ic2.tooltip.jetpack.toggle",
                        Component.literal("Sneak"),
                        Component.literal("Use")));
    }
}
