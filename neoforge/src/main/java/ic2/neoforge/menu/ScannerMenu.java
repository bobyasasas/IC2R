package ic2.neoforge.menu;

import ic2.neoforge.item.ScannerItem;
import ic2.neoforge.registration.ModTools;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.common.Tags;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Legacy ContainerToolScanner / GuiToolScanner: the server sweeps the cube around the player once
 * when the GUI opens and streams the top ore piles to the client through data slots.
 */
public class ScannerMenu extends AbstractContainerMenu {
    public static final int MAX_RESULTS = 10;

    private static final int PLAYER_INV_Y = 152;
    private static final int HOTBAR_Y = 210;

    /** Legacy StackUtil.oreTags: the convention ore tag plus the eight vanilla ore tags. */
    private static final List<TagKey<Item>> ORE_TAGS =
            List.of(
                    Tags.Items.ORES,
                    ItemTags.COAL_ORES,
                    ItemTags.COPPER_ORES,
                    ItemTags.DIAMOND_ORES,
                    ItemTags.GOLD_ORES,
                    ItemTags.IRON_ORES,
                    ItemTags.EMERALD_ORES,
                    ItemTags.LAPIS_ORES,
                    ItemTags.REDSTONE_ORES);

    private final int scannerSlotIndex;
    private final ItemStack scanner;
    private final boolean client;

    /** Packed result rows: [0] row count, then per row the item raw id and its pile total. */
    private final int[] snapshot = new int[1 + MAX_RESULTS * 2];
    private final ContainerData data =
            new ContainerData() {
                @Override
                public int get(int index) {
                    return snapshot[index];
                }

                @Override
                public void set(int index, int value) {
                    snapshot[index] = value;
                }

                @Override
                public int getCount() {
                    return snapshot.length;
                }
            };

    public ScannerMenu(int id, Inventory inventory, int scannerSlotIndex) {
        this(id, inventory, scannerSlotIndex, false);
    }

    public ScannerMenu(int id, Inventory inventory, int scannerSlotIndex, boolean client) {
        super(ModTools.SCANNER_MENU.get(), id);
        this.scannerSlotIndex = scannerSlotIndex;
        this.scanner = inventory.getItem(scannerSlotIndex);
        this.client = client;
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
        addDataSlots(data);
        if (!client) {
            Player player = inventory.player;
            scan(player.level(), player.blockPosition());
        }
    }

    public record ScanEntry(ItemStack stack, int count) {}

    /** Re-runs the legacy ItemScanner.scan sweep around an explicit center. */
    public void scan(Level level, BlockPos center) {
        int range = scanner.getItem() instanceof ScannerItem item ? item.handScanRange() : 0;
        List<ScanEntry> found = scanAround(level, center, range);
        snapshot[0] = Math.min(found.size(), MAX_RESULTS);
        for (int row = 0; row < snapshot[0]; row++) {
            ScanEntry entry = found.get(row);
            snapshot[1 + row * 2] = Item.getId(entry.stack().getItem());
            snapshot[2 + row * 2] = entry.count();
        }
    }

    /**
     * One legacy scan pass: every non-air block in the cube, ore items only, summed per item. The
     * item id tiebreak keeps the ordering deterministic where legacy left it unspecified.
     */
    public static List<ScanEntry> scanAround(Level level, BlockPos center, int range) {
        Object2IntOpenHashMap<Item> counts = new Object2IntOpenHashMap<>();
        for (BlockPos pos :
                BlockPos.betweenClosed(
                        center.offset(-range, -range, -range),
                        center.offset(range, range, range))) {
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) continue;
            // Legacy picked a clone stack per block; 26.1.2 dropped getCloneItemStack, so the
            // block's own item form stands in.
            ItemStack stack = new ItemStack(state.getBlock());
            if (!isOre(stack)) continue;
            counts.addTo(stack.getItem(), stack.getCount());
        }
        List<Object2IntMap.Entry<Item>> entries =
                new ArrayList<>(counts.object2IntEntrySet());
        entries.sort(
                Comparator.comparingInt(Object2IntMap.Entry<Item>::getIntValue)
                        .reversed()
                        .thenComparing(entry -> Item.getId(entry.getKey())));
        List<ScanEntry> found = new ArrayList<>();
        for (Object2IntMap.Entry<Item> entry : entries) {
            if (found.size() == MAX_RESULTS) break;
            found.add(new ScanEntry(new ItemStack(entry.getKey()), entry.getIntValue()));
        }
        return found;
    }

    public static boolean isOre(ItemStack stack) {
        return !stack.isEmpty() && ORE_TAGS.stream().anyMatch(stack::is);
    }

    public int resultCount() {
        return snapshot[0];
    }

    public ItemStack resultStack(int row) {
        return new ItemStack(Item.byId(snapshot[1 + row * 2]));
    }

    public int resultTotal(int row) {
        return snapshot[2 + row * 2];
    }

    public int scannerSlotIndex() {
        return scannerSlotIndex;
    }

    @Override
    public void clicked(int slotIndex, int buttonNum, ContainerInput input, Player player) {
        // Number keys must not swap the scanner out of its hand while the GUI is open.
        if (input == ContainerInput.SWAP && buttonNum == scannerSlotIndex) return;
        super.clicked(slotIndex, buttonNum, input, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (!stillValid(player) || index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        var slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        var stack = slot.getItem().copy();
        var original = stack.copy();
        int hotbar = 27;
        boolean moved =
                moveItemStackTo(
                        stack,
                        index < hotbar ? hotbar : 0,
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
                || player.isAlive() && player.getInventory().getItem(scannerSlotIndex) == scanner;
    }
}
