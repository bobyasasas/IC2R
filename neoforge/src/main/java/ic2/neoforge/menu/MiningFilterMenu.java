package ic2.neoforge.menu;

import ic2.neoforge.component.ModDataComponents;
import ic2.neoforge.registration.ModTools;

import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/**
 * Handheld editor for a mining filter card: 45 hologram slots that copy one of the carried item
 * each on left click and clear on left click with an empty hand, plus the blacklist/whitelist
 * toggle. Every change is written straight back into the card's data components, so closing the
 * screen in any way keeps the edits. While the screen is open the card cannot be moved out of its
 * hand slot.
 */
public final class MiningFilterMenu extends AbstractContainerMenu {
    public static final int ENTRIES = 45;
    private static final int PLAYER_INV_Y = 133;
    private static final int HOTBAR_Y = 191;

    private final int cardSlotIndex;
    private final ItemStack card;
    private final boolean client;
    private final NonNullList<ItemStack> entries = NonNullList.withSize(ENTRIES, ItemStack.EMPTY);
    private boolean blacklist = true;

    public MiningFilterMenu(int id, Inventory inventory, int cardSlotIndex) {
        this(id, inventory, cardSlotIndex, false);
    }

    public MiningFilterMenu(int id, Inventory inventory, int cardSlotIndex, boolean client) {
        super(ModTools.MINING_FILTER_MENU.get(), id);
        this.cardSlotIndex = cardSlotIndex;
        this.card = inventory.getItem(cardSlotIndex);
        this.client = client;
        if (!client) {
            // Mirrors the legacy getOrCreateNbtData: opening the editor once switches the card
            // from "defer to the machine filter" to "override with my own entries".
            card.set(ModDataComponents.MINING_FILTER_BLACKLIST, blacklist);
        } else {
            blacklist = card.getOrDefault(ModDataComponents.MINING_FILTER_BLACKLIST, true);
        }
        card.getOrDefault(ModDataComponents.MINING_FILTER_ITEMS, ItemContainerContents.EMPTY)
                .copyInto(entries);
        var filterContainer = new FilterEntries();
        for (int index = 0; index < ENTRIES; index++)
            addSlot(
                    new Slot(filterContainer, index, 8 + index % 9 * 18, 32 + index / 9 * 18) {
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
        addDataSlots(
                new ContainerData() {
                    @Override
                    public int get(int index) {
                        return blacklist ? 1 : 0;
                    }

                    @Override
                    public void set(int index, int value) {
                        blacklist = value != 0;
                    }

                    @Override
                    public int getCount() {
                        return 1;
                    }
                });
    }

    public boolean blacklist() {
        return blacklist;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != 0 || client || player.containerMenu != this || !stillValid(player)) return false;
        blacklist = !blacklist;
        card.set(ModDataComponents.MINING_FILTER_BLACKLIST, blacklist);
        return true;
    }

    @Override
    public void clicked(int slotIndex, int buttonNum, ContainerInput input, Player player) {
        if (slotIndex < 0 || slotIndex >= ENTRIES) {
            // Number keys must not swap the open card out of its hand while editing.
            if (input == ContainerInput.SWAP && buttonNum == cardSlotIndex) return;
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
        if (slot.getItem() == card || !slot.hasItem() || !slot.mayPickup(player))
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
        return client || player.isAlive() && player.getInventory().getItem(cardSlotIndex) == card;
    }

    private void writeBack() {
        card.set(ModDataComponents.MINING_FILTER_ITEMS, ItemContainerContents.fromItems(entries));
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
