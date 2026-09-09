package ic2.neoforge.item;

import ic2.neoforge.component.ModDataComponents;
import ic2.neoforge.menu.MiningFilterMenu;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

/**
 * Holds up to 45 filter entries plus a blacklist/whitelist mode for the advanced miner. Using it
 * opens the handheld editing screen; the machine-side card slot takes priority over the miner's own
 * filter as soon as the card has been edited once.
 */
public class MiningFilterCardItem extends Item {
    public MiningFilterCardItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player instanceof net.minecraft.server.level.ServerPlayer server) {
            int slot =
                    hand == InteractionHand.MAIN_HAND
                            ? player.getInventory().getSelectedSlot()
                            : Inventory.SLOT_OFFHAND;
            server.openMenu(
                    new SimpleMenuProvider(
                            (id, inventory, owner) -> new MiningFilterMenu(id, inventory, slot),
                            Component.translatable("container.ic2.mining_filter")),
                    data -> data.writeVarInt(slot));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> tooltip,
            TooltipFlag flag) {
        if (!stack.has(ModDataComponents.MINING_FILTER_BLACKLIST)) return;
        tooltip.accept(
                Component.translatable(
                        stack.getOrDefault(ModDataComponents.MINING_FILTER_BLACKLIST, true)
                                ? "ic2.MiningFilter.gui.mode.blacklist"
                                : "ic2.MiningFilter.gui.mode.whitelist"));
        var entries =
                stack.getOrDefault(
                        ModDataComponents.MINING_FILTER_ITEMS, ItemContainerContents.EMPTY);
        tooltip.accept(
                Component.translatable(
                        "ic2.MiningFilter.tooltip.entries",
                        entries.nonEmptyItemCopyStream().count()));
    }
}
