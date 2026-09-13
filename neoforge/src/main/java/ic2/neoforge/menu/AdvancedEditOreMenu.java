package ic2.neoforge.menu;

import ic2.neoforge.component.ModDataComponents;
import ic2.neoforge.registration.ModTools;

import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/**
 * Legacy HandHeldOre / GuiEditOre: a dev-only ore-dictionary placeholder that never left the
 * drawing board — its "Thing One..Ten" list only printed to stdout and its container saved
 * nothing. The port keeps the reachable surface: the nine hologram filter slots and the back
 * button; the dead placeholder list stays dead.
 */
public final class AdvancedEditOreMenu extends AbstractContainerMenu
        implements AdvancedMenus.Bound {
    public static final int ENTRIES = 9;

    public static final int BACK = 0;

    private static final int PLAYER_INV_Y = 116;
    private static final int HOTBAR_Y = 174;

    private final int slotIndex;
    private final ItemStack upgrade;
    private final boolean client;
    private final NonNullList<ItemStack> entries = NonNullList.withSize(ENTRIES, ItemStack.EMPTY);

    public AdvancedEditOreMenu(int id, Inventory inventory, int slotIndex) {
        this(id, inventory, slotIndex, false);
    }

    public AdvancedEditOreMenu(int id, Inventory inventory, int slotIndex, boolean client) {
        super(ModTools.ADVANCED_EDIT_ORE_MENU.get(), id);
        this.slotIndex = slotIndex;
        this.upgrade = inventory.getItem(slotIndex);
        this.client = client;
        upgrade.getOrDefault(ModDataComponents.ADVANCED_FILTER_ITEMS, ItemContainerContents.EMPTY)
                .copyInto(entries);
        var filterContainer = new FilterEntries();
        for (int index = 0; index < ENTRIES; index++)
            addSlot(new Slot(filterContainer, index, 8 + 18 * index, 8) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }

                @Override
                public boolean mayPickup(Player player) {
                    return false;
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            });
        for (int row = 0; row < 3; row++)
            for (int column = 0; column < 9; column++)
                addSlot(
                        new Slot(
                                inventory,
                                9 + row * 9 + column,
                                8 + 18 * column,
                                PLAYER_INV_Y + 18 * row));
        for (int column = 0; column < 9; column++)
            addSlot(new Slot(inventory, column, 8 + 18 * column, HOTBAR_Y));
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != BACK || client || player.containerMenu != this || !stillValid(player))
            return false;
        if (player instanceof net.minecraft.server.level.ServerPlayer server)
            AdvancedMenus.openMain(server, slotIndex);
        return true;
    }

    @Override
    public void clicked(int slotIndex, int buttonNum, ContainerInput input, Player player) {
        if (slotIndex < 0 || slotIndex >= ENTRIES) {
            // Number keys must not swap the open upgrade out of its hand while editing.
            if (input == ContainerInput.SWAP && buttonNum == this.slotIndex) return;
            super.clicked(slotIndex, buttonNum, input, player);
            return;
        }
        if (client || input != ContainerInput.PICKUP) return;
        var carried = getCarried();
        entries.set(slotIndex, carried.isEmpty() ? ItemStack.EMPTY : carried.copyWithCount(1));
        writeBack();
        broadcastFullState();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (!stillValid(player) || index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        var slot = slots.get(index);
        if (slot.getItem() == upgrade || !slot.hasItem() || !slot.mayPickup(player))
            return ItemStack.EMPTY;
        var stack = slot.getItem().copy();
        var original = stack.copy();
        int playerStart = ENTRIES;
        int hotbar = playerStart + 27;
        boolean moved =
                moveItemStackTo(
                        stack,
                        index < hotbar ? hotbar : playerStart,
                        index < hotbar ? slots.size() : hotbar,
                        false);
        if (!moved) return ItemStack.EMPTY;
        slot.setByPlayer(stack);
        slot.onTake(player, original.copyWithCount(original.getCount() - stack.getCount()));
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return client || player.isAlive() && player.getInventory().getItem(slotIndex) == upgrade;
    }

    @Override
    public int slotIndex() {
        return slotIndex;
    }

    private void writeBack() {
        upgrade.set(ModDataComponents.ADVANCED_FILTER_ITEMS, ItemContainerContents.fromItems(entries));
    }

    /** Slot-backed view over the ghost entries so vanilla slot sync can update them. */
    private final class FilterEntries implements Container {
        @Override
        public int getContainerSize() {
            return ENTRIES;
        }

        @Override
        public boolean isEmpty() {
            return entries.stream().allMatch(ItemStack::isEmpty);
        }

        @Override
        public ItemStack getItem(int slot) {
            return entries.get(slot);
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            var stack = entries.get(slot);
            entries.set(slot, ItemStack.EMPTY);
            return stack;
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            entries.set(slot, stack);
        }

        @Override
        public void setChanged() {
            if (!client) writeBack();
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }

        @Override
        public void clearContent() {
            entries.replaceAll(stack -> ItemStack.EMPTY);
        }
    }
}
