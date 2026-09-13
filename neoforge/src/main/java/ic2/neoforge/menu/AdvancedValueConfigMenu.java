package ic2.neoforge.menu;

import ic2.neoforge.component.AdvancedFilterSettings;
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
 * Legacy HandHeldValueConfig (dev-only meta/energy comparison editor): the same nine hologram
 * filter slots plus the comparison type cycle and its bounds. Legacy edited two number text
 * boxes and downgraded empty ones on save; the port uses steppers, so bounds are always valid
 * and the downgrade ladder has no reachable state. All controls write straight back into the
 * upgrade's settings component.
 */
public final class AdvancedValueConfigMenu extends AbstractContainerMenu
        implements AdvancedMenus.Bound {
    public static final int ENTRIES = 9;

    public static final int BACK = 0;
    public static final int TYPE_CYCLE = 1;
    public static final int NORMAL_OP_CYCLE = 2;
    public static final int EXTRA_OP_TOGGLE = 3;
    public static final int NORMAL_DOWN_10 = 4;
    public static final int NORMAL_DOWN = 5;
    public static final int NORMAL_UP = 6;
    public static final int NORMAL_UP_10 = 7;
    public static final int EXTRA_DOWN_10 = 8;
    public static final int EXTRA_DOWN = 9;
    public static final int EXTRA_UP = 10;
    public static final int EXTRA_UP_10 = 11;

    private static final int PLAYER_INV_Y = 116;
    private static final int HOTBAR_Y = 174;

    private final int slotIndex;
    private final ItemStack upgrade;
    private final int tag;
    private final boolean client;
    private final NonNullList<ItemStack> entries = NonNullList.withSize(ENTRIES, ItemStack.EMPTY);
    private final int[] state = new int[5];
    private final ContainerData data =
            new ContainerData() {
                @Override
                public int get(int index) {
                    return state[index];
                }

                @Override
                public void set(int index, int value) {
                    state[index] = value;
                }

                @Override
                public int getCount() {
                    return state.length;
                }
            };

    public AdvancedValueConfigMenu(int id, Inventory inventory, int slotIndex, int tag) {
        this(id, inventory, slotIndex, tag, false);
    }

    public AdvancedValueConfigMenu(
            int id, Inventory inventory, int slotIndex, int tag, boolean client) {
        super(ModTools.ADVANCED_VALUE_CONFIG_MENU.get(), id);
        this.slotIndex = slotIndex;
        this.upgrade = inventory.getItem(slotIndex);
        this.tag = tag;
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
        if (!client) refreshState();
        addDataSlots(data);
    }

    /** The edited settings component: legacy getNBT() is keyed by the option name. */
    private AdvancedFilterSettings settings() {
        return upgrade.getOrDefault(
                tag == AdvancedMenus.TAG_META
                        ? ModDataComponents.ADVANCED_META
                        : ModDataComponents.ADVANCED_ENERGY,
                AdvancedFilterSettings.DEFAULT);
    }

    private void writeSettings(AdvancedFilterSettings newSettings) {
        upgrade.set(
                tag == AdvancedMenus.TAG_META
                        ? ModDataComponents.ADVANCED_META
                        : ModDataComponents.ADVANCED_ENERGY,
                newSettings);
        refreshState();
    }

    private void refreshState() {
        var settings = settings();
        state[0] = settings.type();
        state[1] = settings.normalBound();
        state[2] = settings.normalOp();
        state[3] = settings.extraBound();
        state[4] = settings.extraOp();
    }

    public int type() {
        return state[0];
    }

    public int normalBound() {
        return state[1];
    }

    public int normalOp() {
        return state[2];
    }

    public int extraBound() {
        return state[3];
    }

    public int extraOp() {
        return state[4];
    }

    public boolean tagIsEnergy() {
        return tag == AdvancedMenus.TAG_ENERGY;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (client || player.containerMenu != this || !stillValid(player)) return false;
        var settings = settings();
        switch (id) {
            case BACK -> {
                // Legacy's back button re-requested the same sub screen (a quirk); the port
                // restores the labeled intent and returns to the main editor.
                if (player instanceof net.minecraft.server.level.ServerPlayer server)
                    AdvancedMenus.openMain(server, slotIndex);
            }
            case TYPE_CYCLE -> {
                var values = AdvancedFilterSettings.ComparisonType.VALUES;
                var next = values[(settings.type() + 1) % values.length];
                writeSettings(settings.withType(next));
            }
            case NORMAL_OP_CYCLE -> {
                var ops = AdvancedFilterSettings.ComparisonSetting.VALUES;
                var next = ops[(settings.normalOp() + 1) % ops.length];
                var updated = settings.withNormalOp(next.ordinal());
                // Legacy linkage: extra must stay inside the same direction class as normal.
                var extra = updated.extraSetting();
                if (next == AdvancedFilterSettings.ComparisonSetting.LESS
                        || next == AdvancedFilterSettings.ComparisonSetting.LESS_OR_EQUAL) {
                    if (extra != AdvancedFilterSettings.ComparisonSetting.LESS
                            && extra != AdvancedFilterSettings.ComparisonSetting.LESS_OR_EQUAL)
                        updated =
                                updated.withExtraOp(
                                        AdvancedFilterSettings.ComparisonSetting.LESS.ordinal());
                } else if (extra != AdvancedFilterSettings.ComparisonSetting.GREATER
                        && extra != AdvancedFilterSettings.ComparisonSetting.GREATER_OR_EQUAL)
                    updated =
                            updated.withExtraOp(
                                    AdvancedFilterSettings.ComparisonSetting.GREATER.ordinal());
                writeSettings(updated);
            }
            case EXTRA_OP_TOGGLE -> {
                var ops = AdvancedFilterSettings.ComparisonSetting.VALUES;
                var current = settings.extraSetting();
                var flipped =
                        switch (current) {
                            case LESS -> AdvancedFilterSettings.ComparisonSetting.LESS_OR_EQUAL;
                            case LESS_OR_EQUAL -> AdvancedFilterSettings.ComparisonSetting.LESS;
                            case GREATER ->
                                    AdvancedFilterSettings.ComparisonSetting.GREATER_OR_EQUAL;
                            case GREATER_OR_EQUAL -> AdvancedFilterSettings.ComparisonSetting.GREATER;
                        };
                writeSettings(settings.withExtraOp(flipped.ordinal()));
            }
            case NORMAL_DOWN_10, NORMAL_DOWN, NORMAL_UP, NORMAL_UP_10 -> writeSettings(
                    settings.withNormalBound(
                            bound(settings.normalBound(), id, NORMAL_DOWN_10, NORMAL_UP_10)));
            case EXTRA_DOWN_10, EXTRA_DOWN, EXTRA_UP, EXTRA_UP_10 -> writeSettings(
                    settings.withExtraBound(
                            bound(settings.extraBound(), id, EXTRA_DOWN_10, EXTRA_UP_10)));
            default -> {
                return false;
            }
        }
        return true;
    }

    private static int bound(int current, int id, int down10, int up10) {
        long delta =
                id == down10 ? -10 : id == down10 + 1 ? -1 : id == up10 - 1 ? 1 : 10;
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0, current + delta));
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
