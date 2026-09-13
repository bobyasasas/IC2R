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
 * Legacy HandHeldAdvancedUpgrade / advanced_upgrade.xml: nine hologram filter slots plus the
 * meta/energy toggles (and the dev-only config buttons, gated by !FMLLoader.isProduction() the
 * way legacy gates them behind Util.inDev()). Every change is written straight back into the
 * upgrade's data components, so closing the screen in any way keeps the edits.
 */
public final class AdvancedUpgradeMenu extends AbstractContainerMenu
        implements AdvancedMenus.Bound {
    public static final int ENTRIES = 9;

    // Legacy onEvent ids: meta/energy toggles in production, M/E/O dev buttons open sub screens.
    public static final int META_TOGGLE = 0;
    public static final int ENERGY_TOGGLE = 1;
    public static final int META_CONFIG = 2;
    public static final int ENERGY_CONFIG = 3;
    public static final int ORE_CONFIG = 4;

    private static final int PLAYER_INV_Y = 92;
    private static final int HOTBAR_Y = 150;

    private final int slotIndex;
    private final ItemStack upgrade;
    private final boolean client;
    private final NonNullList<ItemStack> entries = NonNullList.withSize(ENTRIES, ItemStack.EMPTY);
    /** [0] meta, [1] energy (or exact-NBT) checkmark, [2] NBT mode indicator. */
    private final int[] guiState = new int[3];
    private final ContainerData data =
            new ContainerData() {
                @Override
                public int get(int index) {
                    return guiState[index];
                }

                @Override
                public void set(int index, int value) {
                    guiState[index] = value;
                }

                @Override
                public int getCount() {
                    return guiState.length;
                }
            };

    public AdvancedUpgradeMenu(int id, Inventory inventory, int slotIndex) {
        this(id, inventory, slotIndex, false);
    }

    public AdvancedUpgradeMenu(int id, Inventory inventory, int slotIndex, boolean client) {
        super(ModTools.ADVANCED_UPGRADE_MENU.get(), id);
        this.slotIndex = slotIndex;
        this.upgrade = inventory.getItem(slotIndex);
        this.client = client;
        upgrade.getOrDefault(ModDataComponents.ADVANCED_FILTER_ITEMS, ItemContainerContents.EMPTY)
                .copyInto(entries);
        var filterContainer = new FilterEntries();
        for (int index = 0; index < ENTRIES; index++)
            addSlot(
                    new Slot(
                            filterContainer,
                            index,
                            97 + index % 3 * 18,
                            20 + index / 3 * 18) {
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
        if (!client) refreshGuiState();
        addDataSlots(data);
    }

    /** Legacy getGuiState: the energy checkmark also lights for exact NBT matching. */
    private void refreshGuiState() {
        var meta =
                upgrade.getOrDefault(ModDataComponents.ADVANCED_META, AdvancedFilterSettings.DEFAULT);
        var energy =
                upgrade.getOrDefault(
                        ModDataComponents.ADVANCED_ENERGY, AdvancedFilterSettings.DEFAULT);
        int nbtMode = upgrade.getOrDefault(ModDataComponents.ADVANCED_NBT_MODE, 0);
        guiState[0] = meta.active() ? 1 : 0;
        guiState[1] = energy.active() || nbtMode == 2 ? 1 : 0;
        guiState[2] = nbtMode != 0 ? 1 : 0;
    }

    /** Legacy getGuiState("meta"): the toggle checkmark, synced through the data slots. */
    public boolean meta() {
        return guiState[0] != 0;
    }

    /** Legacy getGuiState("energy"): the energy flag, also lit for exact NBT matching. */
    public boolean energy() {
        return guiState[1] != 0;
    }

    public boolean nbtIndicator() {
        return guiState[2] != 0;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (client || player.containerMenu != this || !stillValid(player)) return false;
        switch (id) {
            case META_TOGGLE -> {
                var settings =
                        upgrade.getOrDefault(
                                ModDataComponents.ADVANCED_META, AdvancedFilterSettings.DEFAULT);
                upgrade.set(ModDataComponents.ADVANCED_META, settings.withActive(!settings.active()));
                refreshGuiState();
            }
            case ENERGY_TOGGLE -> {
                var settings =
                        upgrade.getOrDefault(
                                ModDataComponents.ADVANCED_ENERGY, AdvancedFilterSettings.DEFAULT);
                upgrade.set(ModDataComponents.ADVANCED_ENERGY, settings.withActive(!settings.active()));
                refreshGuiState();
            }
            case META_CONFIG, ENERGY_CONFIG, ORE_CONFIG -> {
                if (net.neoforged.fml.loading.FMLLoader.getCurrent().isProduction()) return false;
                int tag =
                        id == META_CONFIG
                                ? AdvancedMenus.TAG_META
                                : id == ENERGY_CONFIG
                                        ? AdvancedMenus.TAG_ENERGY
                                        : AdvancedMenus.TAG_ORE;
                if (player instanceof net.minecraft.server.level.ServerPlayer server)
                    AdvancedMenus.openSub(server, slotIndex, tag);
            }
            default -> {
                return false;
            }
        }
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
