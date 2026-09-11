package ic2.neoforge.item;

import ic2.core.energy.ElectricItemSpec;

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
 * Legacy ItemArmorNightVisionGoggles: a helmet with night vision but no damage absorption
 * (200k EU at tier 1, consumable-style 27 durability, no repair).
 */
public class NightVisionGogglesItem extends ElectricItem {
    public NightVisionGogglesItem(Properties properties, ElectricItemSpec specification) {
        super(properties, specification);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
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
        NightVisionHelper.addToggleTooltip(tooltip);
    }
}
