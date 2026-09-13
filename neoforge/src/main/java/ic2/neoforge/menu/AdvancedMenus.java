package ic2.neoforge.menu;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** Shared open/close plumbing for the advanced upgrade editor screens. */
public final class AdvancedMenus {
    /** Legacy HandHeldAdvancedUpgrade delegate IDs, kept for the port's menu buttons. */
    public static final int TAG_META = 0;
    public static final int TAG_ENERGY = 2;
    public static final int TAG_ORE = 3;

    /** A menu bound to one held stack; implemented by all three advanced upgrade menus. */
    public interface Bound {
        int slotIndex();
    }

    public static void openMain(ServerPlayer player, InteractionHand hand) {
        int slot =
                hand == InteractionHand.MAIN_HAND
                        ? player.getInventory().getSelectedSlot()
                        : Inventory.SLOT_OFFHAND;
        openMain(player, slot);
    }

    public static void openMain(ServerPlayer player, int slotIndex) {
        ItemStack stack = player.getInventory().getItem(slotIndex);
        player.openMenu(
                new SimpleMenuProvider(
                        (id, inventory, owner) -> new AdvancedUpgradeMenu(id, inventory, slotIndex),
                        stack.getHoverName()),
                data -> data.writeVarInt(slotIndex));
    }

    /** Legacy getSubInventory delegate: the dev-only meta/energy config and ore screens. */
    public static void openSub(ServerPlayer player, int slotIndex, int tag) {
        ItemStack stack = player.getInventory().getItem(slotIndex);
        switch (tag) {
            case TAG_META, TAG_ENERGY ->
                player.openMenu(
                        new SimpleMenuProvider(
                                (id, inventory, owner) ->
                                        new AdvancedValueConfigMenu(
                                                id, inventory, slotIndex, tag),
                                Component.translatable(
                                        "container.ic2.advanced_value_config")),
                        data -> {
                            data.writeVarInt(slotIndex);
                            data.writeByte(tag);
                        });
            case TAG_ORE ->
                player.openMenu(
                        new SimpleMenuProvider(
                                (id, inventory, owner) ->
                                        new AdvancedEditOreMenu(id, inventory, slotIndex),
                                Component.translatable("container.ic2.advanced_edit_ore")),
                        data -> data.writeVarInt(slotIndex));
            default -> throw new IllegalArgumentException("Unexpected sub menu tag " + tag);
        }
    }

    /** ModTools menu holder so UpgradeItem can test for any of the three editors. */
    public static boolean isAdvancedMenu(Object menu) {
        return menu instanceof AdvancedUpgradeMenu
                || menu instanceof AdvancedValueConfigMenu
                || menu instanceof AdvancedEditOreMenu;
    }

    private AdvancedMenus() {}
}
