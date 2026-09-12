package ic2.neoforge.test;

import ic2.neoforge.item.ContainmentBoxItem;
import ic2.neoforge.menu.ContainmentBoxMenu;
import ic2.neoforge.registration.ModItems;
import ic2.neoforge.registration.ModReactorItems;
import ic2.neoforge.registration.ModTools;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.ItemLike;

import net.minecraft.gametest.framework.GameTestHelper;

/** Legacy ItemContainmentbox: nuclear-only handheld storage persisted in the stack itself. */
final class ContainmentBoxTests {

    static void acceptsOnlyNuclearItems(GameTestHelper helper) {
        helper.assertTrue(
                ContainmentBoxMenu.accepts(new ItemStack(ModReactorItems.NEAR_DEPLETED_URANIUM.get()))
                        && ContainmentBoxMenu.accepts(new ItemStack(ModReactorItems.URANIUM_FUEL_ROD.get()))
                        && ContainmentBoxMenu.accepts(new ItemStack(ModReactorItems.MOX_FUEL_ROD.get()))
                        && ContainmentBoxMenu.accepts(
                                new ItemStack(ModReactorItems.DEPLETED_ISOTOPE_FUEL_ROD.get())),
                "Legacy ItemNuclearResource/ItemReactorUranium family items are storable");
        helper.assertTrue(
                !ContainmentBoxMenu.accepts(new ItemStack(Items.IRON_INGOT))
                        && !ContainmentBoxMenu.accepts(new ItemStack(Items.CHEST))
                        && !ContainmentBoxMenu.accepts(ItemStack.EMPTY),
                "Plain items and empty stacks stay out of the containment box");

        var box = ModTools.CONTAINMENT_BOX.get();
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getInventory().setItem(0, new ItemStack(box));
        helper.assertTrue(
                player.getInventory().getItem(0).getItem() instanceof ContainmentBoxItem
                        && player.getInventory()
                                .getItem(0)
                                .is(ic2.neoforge.registration.ModTools.CONTAINMENT_BOX.get()),
                "The box item registers and stacks in the player inventory");

        helper.succeed();
    }

    static void menuPersistsContentsInStack(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getInventory().setItem(0, new ItemStack(ModTools.CONTAINMENT_BOX.get()));
        int slot = 0;
        var menu = new ContainmentBoxMenu(1, player.getInventory(), slot);
        player.containerMenu = menu;

        menu.getSlot(21).set(new ItemStack(ModReactorItems.NEAR_DEPLETED_URANIUM.get(), 4));
        menu.quickMoveStack(player, 21);
        helper.assertTrue(
                !menu.getSlot(0).getItem().isEmpty(),
                "Shift-click must route a nuclear resource into the containment box");

        var stored = player
                .getInventory()
                .getItem(slot)
                .get(ic2.neoforge.component.ModDataComponents.CONTAINMENT_BOX_ITEMS);
        helper.assertTrue(stored != null, "Box payload must persist in the stack's data components");
        var reopened = new ContainmentBoxMenu(2, player.getInventory(), slot);
        helper.assertTrue(
                reopened.getSlot(0).getItem().is(ModReactorItems.NEAR_DEPLETED_URANIUM.get()),
                "Reopening the box restores the stored contents from the data component");

        helper.succeed();
    }

    static void shiftClickRejectsPlainItems(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getInventory().setItem(0, new ItemStack(ModTools.CONTAINMENT_BOX.get()));
        player.getInventory().setItem(9, new ItemStack(Items.IRON_INGOT, 8));
        player.getInventory().setItem(10, new ItemStack(ModReactorItems.NEAR_DEPLETED_URANIUM.get()));
        var menu = new ContainmentBoxMenu(3, player.getInventory(), 0);
        player.containerMenu = menu;

        // Player inventory indices 9/10 map to menu slots 12/13 (playerStart = 12).
        menu.quickMoveStack(player, 12);
        for (int boxSlot = 0; boxSlot < ContainmentBoxMenu.SLOTS; boxSlot++) {
            helper.assertTrue(
                    menu.getSlot(boxSlot).getItem().isEmpty(),
                    "Iron must never reach any containment slot");
        }
        int ironLeft = 0;
        for (int inventorySlot = 0; inventorySlot < player.getInventory().getContainerSize(); inventorySlot++) {
            if (player.getInventory().getItem(inventorySlot).is(Items.IRON_INGOT)) {
                ironLeft += player.getInventory().getItem(inventorySlot).getCount();
            }
        }
        helper.assertTrue(
                ironLeft == 8,
                "Rejected iron stays with the player wherever the shift-click lands it");

        menu.quickMoveStack(player, 13);
        helper.assertTrue(
                menu.getSlot(0).getItem().is(ModReactorItems.NEAR_DEPLETED_URANIUM.get()),
                "The nuclear resource routes into the box on shift-click");
        helper.assertTrue(
                player.getInventory().getItem(10).isEmpty(),
                "The routed stack leaves the player inventory");

        helper.succeed();
    }

    private ContainmentBoxTests() {}
}
