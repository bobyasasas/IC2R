package ic2.neoforge.menu;

import ic2.neoforge.component.ModDataComponents;
import ic2.neoforge.item.DepletingRodItem;
import ic2.neoforge.item.FuelRodItem;
import ic2.neoforge.item.NuclearResourceItem;

import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/**
 * Legacy ContainerContainmentbox: twelve real slots that only accept nuclear resources and fuel
 * rods, backed directly by the box item's data component so every change persists in place.
 */
public final class ContainmentBoxMenu extends AbstractContainerMenu {
    public static final int SLOTS = 12;
    private static final int PLAYER_INV_Y = 84;
    private static final int HOTBAR_Y = 142;

    private final int boxSlotIndex;
    private final ItemStack box;
    private final boolean client;

    public ContainmentBoxMenu(int id, Inventory inventory, int boxSlotIndex) {
        this(id, inventory, boxSlotIndex, false);
    }

    public ContainmentBoxMenu(int id, Inventory inventory, int boxSlotIndex, boolean client) {
        super(ic2.neoforge.registration.ModTools.CONTAINMENT_BOX_MENU.get(), id);
        this.boxSlotIndex = boxSlotIndex;
        this.box = inventory.getItem(boxSlotIndex);
        this.client = client;
        var contents = new BoxContents();
        for (int index = 0; index < SLOTS; index++) {
            int column = index % 4;
            int row = index / 4;
            addSlot(
                    new Slot(
                            contents,
                            index,
                            53 + column * 18,
                            19 + row * 18) {
                        @Override
                        public boolean mayPlace(ItemStack stack) {
                            return accepts(stack);
                        }
                    });
        }
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

    /**
     * Legacy HandHeldContainmentbox.canPlaceItem: nuclear resources and uranium-family reactor
     * rods only (MoxFuelRodItem subclasses FuelRodItem, so it is covered).
     */
    public static boolean accepts(ItemStack stack) {
        return !stack.isEmpty()
                && (stack.getItem() instanceof NuclearResourceItem
                        || stack.getItem() instanceof FuelRodItem
                        || stack.getItem() instanceof DepletingRodItem);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (!stillValid(player) || index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        var slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        var stack = slot.getItem().copy();
        var original = stack.copy();
        int playerStart = SLOTS;
        int hotbar = playerStart + 27;
        boolean moved;
        if (index < playerStart) {
            moved = moveItemStackTo(stack, playerStart, slots.size(), false);
        } else {
            // Legacy ContainerContainmentbox.transferStackInSlot: player shift-clicks try the
            // twelve radioactive slots first (mayPlace keeps plain items out), then the inventory.
            moved = moveItemStackTo(stack, 0, playerStart, false)
                    || moveItemStackTo(
                            stack,
                            index < hotbar ? hotbar : playerStart,
                            index < hotbar ? slots.size() : hotbar,
                            false);
        }
        if (!moved) return ItemStack.EMPTY;
        slot.setByPlayer(stack);
        slot.onTake(player, original.copyWithCount(original.getCount() - stack.getCount()));
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return client || player.isAlive() && player.getInventory().getItem(boxSlotIndex) == box;
    }

    /** Slot-backed view over the component payload, writing straight back on every change. */
    private final class BoxContents implements Container {
        private final NonNullList<ItemStack> entries =
                NonNullList.withSize(SLOTS, ItemStack.EMPTY);

        private BoxContents() {
            box.getOrDefault(
                            ModDataComponents.CONTAINMENT_BOX_ITEMS,
                            ItemContainerContents.EMPTY)
                    .copyInto(entries);
        }

        private void writeBack() {
            if (!client) box.set(
                    ModDataComponents.CONTAINMENT_BOX_ITEMS,
                    ItemContainerContents.fromItems(entries));
        }

        @Override
        public int getContainerSize() {
            return SLOTS;
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
            var stack = entries.get(slot);
            if (stack.isEmpty()) return ItemStack.EMPTY;
            var taken = stack.split(amount);
            if (stack.isEmpty()) entries.set(slot, ItemStack.EMPTY);
            writeBack();
            return taken;
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
            writeBack();
        }

        @Override
        public void setChanged() {
            writeBack();
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }

        @Override
        public void clearContent() {
            entries.replaceAll(stack -> ItemStack.EMPTY);
            writeBack();
        }
    }
}
