package ic2.neoforge.item;

import ic2.neoforge.component.ModDataComponents;
import ic2.neoforge.fluid.FluidDefinition;
import ic2.neoforge.registration.ModFluids;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStackTemplate;

import java.util.function.Consumer;

/**
 * Legacy ItemArmorJetpack: a 30000 mB biogas tank worn on the chest powering thrust 1.0, a 20%
 * drop ramp, hover multiplier 0.2 and the 1.0 world height divisor; the legacy drainEnergy pays
 * the tick's millibuckets only when the tank still holds them. Like the CF pack, the tank is a
 * data component; world-side filling arrives with the fluid machines.
 */
public class JetpackItem extends Item implements JetpackLike {
    public static final int CAPACITY_MB = 30000;

    public JetpackItem(Properties properties) {
        super(properties);
    }

    public static net.minecraft.world.level.material.Fluid biogas() {
        return ModFluids.FAMILIES.get(FluidDefinition.BIOGAS).source().get();
    }

    public static int getContentsMb(ItemStack stack) {
        FluidStackTemplate stored = stack.get(ModDataComponents.FLUID);
        if (stored == null || stored.fluid().value() != biogas()) return 0;
        return Math.min(CAPACITY_MB, stored.amount());
    }

    /** Legacy drainEnergy: refuse the whole tick unless the tank covers the full amount. */
    public static int drainMb(ItemStack stack, int amount) {
        int contents = getContentsMb(stack);
        if (contents < amount) return 0;
        int remaining = contents - amount;
        if (remaining <= 0) {
            stack.remove(ModDataComponents.FLUID);
        } else {
            stack.set(ModDataComponents.FLUID, new FluidStackTemplate(biogas(), remaining));
        }
        return amount;
    }

    public static void fillMb(ItemStack stack, int amount) {
        stack.set(ModDataComponents.FLUID, new FluidStackTemplate(biogas(), Math.min(amount, CAPACITY_MB)));
    }

    @Override
    public float jetpackPower(ItemStack stack) {
        return 1.0F;
    }

    @Override
    public float dropPercentage() {
        return 0.2F;
    }

    @Override
    public float hoverMultiplier() {
        return 0.2F;
    }

    @Override
    public float worldHeightDivisor() {
        return 1.0F;
    }

    @Override
    public double chargeLevel(ItemStack stack) {
        return (double) getContentsMb(stack) / CAPACITY_MB;
    }

    @Override
    public void drainFlightEnergy(ItemStack stack, int amount) {
        drainMb(stack, amount);
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
        return InteractionResult.PASS;
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
        return net.minecraft.util.Mth.hsvToRgb(level / 3.0F, 1.0F, 1.0F);
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
                            biogas().getFluidType().getDescription(),
                            contents));
        } else {
            tooltip.accept(Component.translatable("ic2.item.fluid_container.empty"));
        }
    }
}
