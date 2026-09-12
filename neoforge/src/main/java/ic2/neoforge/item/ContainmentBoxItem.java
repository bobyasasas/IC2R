package ic2.neoforge.item;

import ic2.neoforge.menu.ContainmentBoxMenu;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Legacy ItemContainmentbox: a handheld twelve-slot container that only stores nuclear resources
 * and fuel rods. The stack itself is the storage (data component payload), so contents survive
 * dropping, chests and any other stack transfer.
 */
public class ContainmentBoxItem extends Item {
    public ContainmentBoxItem(Properties properties) {
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
                            (id, inventory, owner) ->
                                    new ContainmentBoxMenu(id, inventory, slot),
                            Component.translatable("container.ic2.containment_box")),
                    data -> data.writeVarInt(slot));
        }
        return InteractionResult.SUCCESS;
    }
}
