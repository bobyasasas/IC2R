package ic2.neoforge.item;

import ic2.core.energy.ElectricItemSpec;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

/**
 * Legacy ItemArmorNanoSuit: one million EU at tier 3, 90% damage absorption at 5000 EU per point
 * and the charged protection values while enough charge remains. The helmet can toggle night
 * vision (legacy Alt+ModeSwitch; ported to sneak + use like the other electric tools).
 */
public class NanoSuitItem extends ElectricArmorItem {
    public static final int ENERGY_PER_DAMAGE = 5000;
    public static final double DAMAGE_ABSORPTION_RATIO = 0.9;

    private final ArmorType type;

    public NanoSuitItem(
            Properties properties,
            ElectricItemSpec specification,
            ArmorType type,
            ArmorMaterial material,
            int chargedProtection) {
        super(
                properties,
                specification,
                type,
                material,
                chargedProtection,
                ENERGY_PER_DAMAGE,
                DAMAGE_ABSORPTION_RATIO);
        this.type = type;
    }

    /** Legacy ItemArmorNanoSuit.absorbFall: cancel falls of up to seven damage points for EU. */
    @Override
    public boolean absorbFall(ItemStack stack, float distance) {
        int fallDamage = Math.max((int) distance - 3, 0);
        if (fallDamage >= 8) {
            return false;
        }

        double energyCost = this.energyPerDamage() * fallDamage;
        if (energyCost > ElectricItemEnergy.charge(stack)) {
            return false;
        }

        ElectricItemEnergy.discharge(stack, energyCost, Integer.MAX_VALUE, true, false, false);
        return true;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (this.type == ArmorType.HELMET && player.isShiftKeyDown()) {
            NightVisionHelper.toggle(stack, player, level);
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
        if (this.type == ArmorType.HELMET) {
            NightVisionHelper.addToggleTooltip(tooltip);
        }
    }
}
