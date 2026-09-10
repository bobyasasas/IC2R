package ic2.neoforge.menu;

import ic2.neoforge.component.ModDataComponents;
import ic2.neoforge.item.CropAnalyzerItem;
import ic2.neoforge.item.ElectricItem;
import ic2.neoforge.item.ElectricItemEnergy;
import ic2.neoforge.registration.ModCrops;
import ic2.neoforge.registration.ModTools;

import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/**
 * Handheld crop analyzer (legacy ContainerAnalyzer): input slot takes seed bags, the output slot
 * only releases them and the battery slot is a discharge placement gate at the analyzer's tier.
 * Every broadcast the server tries one scan step; the three slots live on in the analyzer stack's
 * data component so the GUI keeps its contents between openings.
 */
public final class CropAnalyzerMenu extends AbstractContainerMenu {
    public static final int ANALYZER_SLOTS = 3;
    private static final int PLAYER_INV_Y = 141;
    private static final int HOTBAR_Y = 199;

    private final int analyzerSlotIndex;
    private final ItemStack analyzer;
    private final boolean client;
    private final NonNullList<ItemStack> contents =
            NonNullList.withSize(ANALYZER_SLOTS, ItemStack.EMPTY);
    private final Container analyzerSlots = new AnalyzerSlots();

    public CropAnalyzerMenu(int id, Inventory inventory, int analyzerSlotIndex) {
        this(id, inventory, analyzerSlotIndex, false);
    }

    public CropAnalyzerMenu(int id, Inventory inventory, int analyzerSlotIndex, boolean client) {
        super(ModTools.CROP_ANALYZER_MENU.get(), id);
        this.analyzerSlotIndex = analyzerSlotIndex;
        this.analyzer = inventory.getItem(analyzerSlotIndex);
        this.client = client;
        analyzer
                .getOrDefault(ModDataComponents.ANALYZER_CONTENTS.get(), ItemContainerContents.EMPTY)
                .copyInto(contents);
        addSlot(
                new Slot(analyzerSlots, 0, 8, 7) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return stack.is(ModCrops.CROP_SEED_BAG.get());
                    }
                });
        addSlot(
                new Slot(analyzerSlots, 1, 41, 7) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false;
                    }
                });
        addSlot(
                new Slot(analyzerSlots, 2, 152, 7) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        // Legacy SlotDischarge: anything that can give up charge at this tier.
                        return ElectricItemEnergy.discharge(
                                        stack,
                                        Double.POSITIVE_INFINITY,
                                        analyzerTier(),
                                        true,
                                        true,
                                        true)
                                > 0;
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

    private int analyzerTier() {
        return analyzer.getItem() instanceof ElectricItem electric
                ? electric.specification().tier()
                : Integer.MAX_VALUE;
    }

    public ItemStack analyzedStack() {
        ItemStack output = contents.get(1);
        if (!output.isEmpty()) return output;
        return contents.get(0);
    }

    @Override
    public void broadcastChanges() {
        if (!client && analyzer.getItem() instanceof CropAnalyzerItem item) {
            if (item.tryScan(analyzer, contents)) writeBack();
        }
        super.broadcastChanges();
    }

    @Override
    public void clicked(int slotIndex, int buttonNum, ContainerInput input, Player player) {
        // Number keys must not swap the analyzer out of its hand while the GUI is open.
        if (input == ContainerInput.SWAP && buttonNum == analyzerSlotIndex) return;
        super.clicked(slotIndex, buttonNum, input, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (!stillValid(player) || index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        var slot = slots.get(index);
        if (slot.getItem() == analyzer || !slot.hasItem() || !slot.mayPickup(player))
            return ItemStack.EMPTY;
        var stack = slot.getItem().copy();
        var original = stack.copy();
        int playerStart = ANALYZER_SLOTS;
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
        return client
                || player.isAlive() && player.getInventory().getItem(analyzerSlotIndex) == analyzer;
    }

    private void writeBack() {
        analyzer.set(
                ModDataComponents.ANALYZER_CONTENTS.get(), ItemContainerContents.fromItems(contents));
    }

    /** Slot-backed view over the three analyzer slots so vanilla sync can move real items. */
    private final class AnalyzerSlots implements Container {
        @Override
        public int getContainerSize() {
            return ANALYZER_SLOTS;
        }

        @Override
        public boolean isEmpty() {
            return contents.stream().allMatch(ItemStack::isEmpty);
        }

        @Override
        public ItemStack getItem(int slot) {
            return contents.get(slot);
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            var stack = ContainerHelper.removeItem(contents, slot, amount);
            if (!stack.isEmpty()) setChanged();
            return stack;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            return ContainerHelper.takeItem(contents, slot);
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            contents.set(slot, stack);
            setChanged();
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
            contents.clear();
        }
    }
}
