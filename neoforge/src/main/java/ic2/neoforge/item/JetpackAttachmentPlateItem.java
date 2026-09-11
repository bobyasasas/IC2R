package ic2.neoforge.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/**
 * Legacy Ic2Items.JETPACK_ATTACHMENT_PLATE: a plain crafting part whose tooltip explains the
 * attachment (craft, attach, irreversible warning).
 */
public class JetpackAttachmentPlateItem extends Item {
    public JetpackAttachmentPlateItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> tooltip,
            TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        tooltip.accept(Component.translatable("ic2.tooltip.jetpack_attachment.craft"));
        tooltip.accept(Component.translatable("ic2.tooltip.jetpack_attachment.attach"));
        tooltip.accept(Component.translatable("ic2.tooltip.jetpack_attachment.warning"));
    }
}
