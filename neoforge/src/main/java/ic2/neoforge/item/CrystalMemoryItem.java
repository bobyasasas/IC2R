package ic2.neoforge.item;

import ic2.neoforge.component.ModDataComponents;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/**
 * Crystal memory: stores one scanned item pattern as an embedded stack. The UU-matter value display
 * activates once the value index lands with the replicator slice.
 */
public final class CrystalMemoryItem extends Item {
    public CrystalMemoryItem(Properties properties) {
        super(properties);
    }

    /** The recorded pattern, or empty when the memory is blank. */
    public ItemStack readPattern(ItemStack stack) {
        var contents =
                stack.getOrDefault(
                        ModDataComponents.CRYSTAL_MEMORY_PATTERN, ItemContainerContents.EMPTY);
        return contents.copyOne();
    }

    public void writePattern(ItemStack stack, ItemStack pattern) {
        if (pattern == null || pattern.isEmpty()) {
            stack.remove(ModDataComponents.CRYSTAL_MEMORY_PATTERN);
        } else {
            stack.set(
                    ModDataComponents.CRYSTAL_MEMORY_PATTERN,
                    ItemContainerContents.fromItems(java.util.List.of(pattern)));
        }
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> tooltip,
            TooltipFlag flag) {
        var recorded = readPattern(stack);
        if (recorded.isEmpty()) {
            tooltip.accept(Component.translatable("item.ic2.crystal_memory.tooltip.empty"));
        } else {
            tooltip.accept(
                    Component.translatable(
                            "item.ic2.crystal_memory.tooltip.item",
                            recorded.getHoverName()));
        }
    }
}
