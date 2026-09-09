package ic2.neoforge.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/**
 * Permanent block cutting blade; a recipe only runs when its hardness is at most the blade's. The
 * three legacy tiers are iron (3), steel (6) and diamond (9).
 */
public final class CuttingBladeItem extends Item {
    private final int hardness;
    private final String infoKey;

    public CuttingBladeItem(Properties properties, int hardness, String infoKey) {
        super(properties);
        this.hardness = hardness;
        this.infoKey = infoKey;
    }

    public int hardness() {
        return hardness;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> tooltip,
            TooltipFlag flag) {
        tooltip.accept(Component.translatable(infoKey));
        tooltip.accept(Component.translatable("ic2.cutting_blade.hardness", hardness));
    }
}
