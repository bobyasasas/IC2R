package ic2.neoforge.item;

import ic2.core.util.SiString;
import ic2.neoforge.component.ModDataComponents;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

/**
 * Crystal memory: stores one scanned item pattern as an embedded stack plus the UU-matter value in
 * buckets snapshotted at record time (legacy ItemCrystalMemory shows the live graph value; the
 * port value graph only exists server-side, so the scanner bakes the value into the stack).
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
            // Legacy blank wipes pattern and value together (one NBT key in ItemCrystalMemory).
            stack.remove(ModDataComponents.CRYSTAL_MEMORY_VALUE);
        } else {
            stack.set(
                    ModDataComponents.CRYSTAL_MEMORY_PATTERN,
                    ItemContainerContents.fromItems(java.util.List.of(pattern)));
        }
    }

    /** The recorded UU-matter value in buckets, or null for memories recorded without one. */
    public @Nullable Double readValue(ItemStack stack) {
        return stack.get(ModDataComponents.CRYSTAL_MEMORY_VALUE);
    }

    public void writeValue(ItemStack stack, double buckets) {
        if (Double.isFinite(buckets)) {
            stack.set(ModDataComponents.CRYSTAL_MEMORY_VALUE, buckets);
        } else {
            stack.remove(ModDataComponents.CRYSTAL_MEMORY_VALUE);
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
            var value = readValue(stack);
            if (value != null) {
                tooltip.accept(
                        Component.translatable(
                                "item.ic2.crystal_memory.tooltip.uu_matter",
                                SiString.format(value, 4)));
            }
        }
    }
}
