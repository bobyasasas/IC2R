package ic2.neoforge.menu;

import ic2.neoforge.machine.NukeBlockEntity;
import ic2.neoforge.registration.ModNuke;
import ic2.neoforge.transfer.MachineInventory;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;

import org.jspecify.annotations.Nullable;

/**
 * The nuke gui (legacy guidef/nuke.xml): one radioactive payload slot and one industrial TNT slot
 * drawn at all eight surrounding positions, above the player inventory.
 */
public final class NukeMenu extends AbstractContainerMenu {
    /** Machine slot count: the inside slot plus eight views of the single outside slot. */
    public static final int MACHINE_SLOTS = 9;

    private static final int[][] OUTSIDE_POSITIONS = {
        {51, 7}, {105, 7}, {25, 34}, {132, 34}, {25, 88}, {132, 88}, {51, 115}, {105, 115}
    };
    private static final int INSIDE_X = 78;
    private static final int INSIDE_Y = 61;
    private static final int INVENTORY_X = 7;
    private static final int INVENTORY_Y = 136;

    private final BlockPos position;
    private final @Nullable NukeBlockEntity nuke;

    public NukeMenu(int id, Inventory playerInventory, NukeBlockEntity nuke) {
        super(ModNuke.NUKE_MENU.get(), id);
        this.position = nuke.getBlockPos().immutable();
        this.nuke = nuke;
        this.addMachineSlots(nuke.inventory());
        this.addPlayerSlots(playerInventory);
    }

    /** Client-side menu: the charge contents stay server authoritative. */
    public NukeMenu(int id, Inventory playerInventory, BlockPos position) {
        super(ModNuke.NUKE_MENU.get(), id);
        this.position = position.immutable();
        this.nuke = null;
        this.addMachineSlots(new MachineInventory(2, () -> {}, NukeBlockEntity::accepts));
        this.addPlayerSlots(playerInventory);
    }

    private void addMachineSlots(MachineInventory inventory) {
        addSlot(
                new ResourceHandlerSlot(
                        inventory,
                        inventory::set,
                        NukeBlockEntity.INSIDE_SLOT,
                        INSIDE_X,
                        INSIDE_Y) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return NukeBlockEntity.accepts(
                                NukeBlockEntity.INSIDE_SLOT, ItemResource.of(stack));
                    }
                });
        for (int[] entry : OUTSIDE_POSITIONS) {
            addSlot(
                    new ResourceHandlerSlot(
                            inventory,
                            inventory::set,
                            NukeBlockEntity.OUTSIDE_SLOT,
                            entry[0],
                            entry[1]) {
                        @Override
                        public boolean mayPlace(ItemStack stack) {
                            return NukeBlockEntity.accepts(
                                    NukeBlockEntity.OUTSIDE_SLOT, ItemResource.of(stack));
                        }
                    });
        }
    }

    private void addPlayerSlots(Inventory playerInventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(
                        new Slot(
                                playerInventory,
                                9 + row * 9 + column,
                                INVENTORY_X + column * 18,
                                INVENTORY_Y + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, INVENTORY_X + column * 18, INVENTORY_Y + 58));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return this.nuke == player.level().getBlockEntity(this.position)
                && stillValid(
                        ContainerLevelAccess.create(player.level(), this.position),
                        player,
                        ModNuke.NUKE.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        var slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        var stack = slot.getItem().copy();
        var original = stack.copy();
        boolean moved =
                index < MACHINE_SLOTS
                        ? moveItemStackTo(stack, MACHINE_SLOTS, slots.size(), true)
                        : moveItemStackTo(stack, 0, MACHINE_SLOTS, false);
        if (!moved) return ItemStack.EMPTY;
        slot.setByPlayer(stack);
        slot.onTake(player, original.copyWithCount(original.getCount() - stack.getCount()));
        return original;
    }
}
