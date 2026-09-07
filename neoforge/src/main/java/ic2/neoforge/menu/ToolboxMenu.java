package ic2.neoforge.menu;

import ic2.neoforge.component.ModDataComponents;
import ic2.neoforge.item.ToolboxItem;
import ic2.neoforge.registration.ModToolbox;
import ic2.neoforge.transfer.MachineInventory;
import ic2.neoforge.transfer.ToolboxHandler;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.IndexModifier;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;

import java.util.UUID;

public final class ToolboxMenu extends AbstractContainerMenu {
    private final int boundSlot;
    private final UUID identity;
    private final boolean client;

    public ToolboxMenu(int id, Inventory inventory, int boundSlot, UUID identity, boolean client) {
        super(ModToolbox.MENU.get(), id);
        this.boundSlot = boundSlot;
        this.identity = identity;
        this.client = client;
        ResourceHandler<ItemResource> contents;
        IndexModifier<ItemResource> setter;
        if (client) {
            var storage =
                    new MachineInventory(9, () -> {}, (slot, item) -> ToolboxItem.accepts(item));
            contents = storage;
            setter = storage::set;
        } else {
            var storage = new ToolboxHandler(ItemAccess.forPlayerSlot(inventory.player, boundSlot));
            contents = storage;
            setter = storage::set;
        }
        for (int slot = 0; slot < 9; slot++)
            addSlot(new ResourceHandlerSlot(contents, setter, slot, 8 + 18 * slot, 41));
        for (int row = 0; row < 3; row++)
            for (int column = 0; column < 9; column++)
                addPlayerSlot(inventory, 9 + row * 9 + column, 8 + 18 * column, 84 + 18 * row);
        for (int slot = 0; slot < 9; slot++) addPlayerSlot(inventory, slot, 8 + 18 * slot, 142);
    }

    private void addPlayerSlot(Inventory inventory, int index, int x, int y) {
        boolean locked = index == boundSlot;
        addSlot(
                new Slot(inventory, index, x, y) {
                    @Override
                    public boolean mayPickup(Player player) {
                        return !locked;
                    }

                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return !locked;
                    }
                });
    }

    public boolean contains(ItemStack stack) {
        return stack.getItem() instanceof ToolboxItem
                && identity.equals(stack.get(ModDataComponents.TOOLBOX_ID));
    }

    @Override
    public boolean stillValid(Player player) {
        if (client) return true;
        var stack = player.getInventory().getItem(boundSlot);
        return player.isAlive() && contains(stack);
    }

    @Override
    public void clicked(int slot, int button, ContainerInput type, Player player) {
        if (!stillValid(player) || type == ContainerInput.SWAP && button == boundSlot) return;
        super.clicked(slot, button, type, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (!stillValid(player) || index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        var slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        var stack = slot.getItem().copy();
        var original = stack.copy();
        boolean moved =
                index < 9
                        ? moveItemStackTo(stack, 9, slots.size(), true)
                        : moveItemStackTo(stack, 0, 9, false);
        if (!moved) return ItemStack.EMPTY;
        slot.setByPlayer(stack);
        slot.onTake(player, original.copyWithCount(original.getCount() - stack.getCount()));
        return original;
    }
}
