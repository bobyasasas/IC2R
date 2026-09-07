package ic2.neoforge.test;

import com.mojang.serialization.JsonOps;

import ic2.neoforge.component.ModDataComponents;
import ic2.neoforge.menu.ToolboxMenu;
import ic2.neoforge.registration.ModToolbox;
import ic2.neoforge.registration.ModTools;
import ic2.neoforge.transfer.ToolboxHandler;

import io.netty.buffer.Unpooled;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.List;
import java.util.UUID;

final class ToolboxTests {
    static void storage(GameTestHelper helper) {
        var access = ItemAccess.forStack(ModToolbox.ITEM.toStack());
        var handler = new ToolboxHandler(access);
        var wrench = ModTools.ELECTRIC_WRENCH.toStack();
        wrench.set(ModDataComponents.CHARGE, 1234.5);
        var tool = ItemResource.of(wrench);
        try (var tx = Transaction.openRoot()) {
            helper.assertTrue(handler.insert(0, tool, 1, tx) == 1, "Tool must fit in toolbox");
            helper.assertTrue(
                    handler.insert(1, ItemResource.of(Items.DIRT), 1, tx) == 0,
                    "Non-tools must be rejected");
            helper.assertTrue(
                    handler.insert(1, ItemResource.of(ModToolbox.ITEM.get()), 1, tx) == 0,
                    "Nested toolboxes must be rejected");
        }
        helper.assertTrue(
                handler.getAmountAsInt(0) == 0, "Aborted transaction must preserve empty box");
        handler.set(8, tool, 1);
        var original = access.getResource().toStack();
        var ops = helper.getLevel().registryAccess().createSerializationContext(JsonOps.INSTANCE);
        var restored =
                ItemStack.CODEC
                        .parse(ops, ItemStack.CODEC.encodeStart(ops, original).getOrThrow())
                        .getOrThrow();
        helper.assertTrue(
                ItemStack.matches(original, restored),
                "Nine-slot contents and charge must survive saving");
        var buffer =
                new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        try {
            ItemStack.STREAM_CODEC.encode(buffer, restored);
            var decoded = ItemStack.STREAM_CODEC.decode(buffer);
            var reloaded = new ToolboxHandler(ItemAccess.forStack(decoded));
            helper.assertTrue(
                    reloaded.getResource(8).equals(tool),
                    "Last slot must survive network serialization");
            try (var tx = Transaction.openRoot()) {
                helper.assertTrue(
                        reloaded.extract(8, tool, 1, tx) == 1,
                        "Saved tool must remain extractable");
                tx.commit();
            }
            helper.assertTrue(
                    reloaded.getAmountAsInt(8) == 0,
                    "Committed extraction removes the tool exactly once");
        } finally {
            buffer.release();
        }
        helper.succeed();
    }

    static void menuBinding(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var inventory = player.getInventory();
        var box = ModToolbox.ITEM.toStack();
        var identity = UUID.randomUUID();
        box.set(ModDataComponents.TOOLBOX_ID, identity);
        inventory.setItem(0, box);
        var hammer = ModTools.FORGE_HAMMER.toStack();
        hammer.setDamageValue(47);
        inventory.setItem(1, hammer);
        var menu = new ToolboxMenu(1, inventory, 0, identity, false);
        player.containerMenu = menu;
        menu.quickMoveStack(player, 37);
        helper.assertTrue(
                inventory.getItem(1).isEmpty() && menu.getSlot(0).getItem().getDamageValue() == 47,
                "Shift-click must immediately store a damaged tool");
        menu.clicked(36, 0, ContainerInput.THROW, player);
        menu.clicked(0, 0, ContainerInput.SWAP, player);
        helper.assertTrue(
                menu.stillValid(player) && inventory.getItem(0).is(ModToolbox.ITEM.get()),
                "Opened box cannot be dropped or swapped into itself");
        menu.quickMoveStack(player, 0);
        helper.assertTrue(
                menu.getSlot(0).getItem().isEmpty(), "Shift-click must retrieve stored tools");
        var stale = new ToolboxHandler(ItemAccess.forPlayerSlot(player, 0));
        stale.set(0, ItemResource.of(hammer), 1);
        var removed = inventory.getItem(0).copy();
        var replacement = ModToolbox.ITEM.toStack();
        replacement.set(ModDataComponents.TOOLBOX_ID, UUID.randomUUID());
        inventory.setItem(0, replacement);
        new ToolboxHandler(ItemAccess.forPlayerSlot(player, 0)).set(0, ItemResource.of(hammer), 1);
        try (var tx = Transaction.openRoot()) {
            helper.assertTrue(
                    stale.extract(0, ItemResource.of(hammer), 1, tx) == 0,
                    "Stale handler cannot extract from replacement box");
        }
        helper.assertTrue(
                !menu.stillValid(player) && stale.getAmountAsInt(0) == 0,
                "Replacement box must invalidate menu and cached access");
        helper.assertTrue(
                new ToolboxHandler(ItemAccess.forStack(removed)).getAmountAsInt(0) == 1,
                "Contents must follow the removed item without a menu-close save");
        int offhand = net.minecraft.world.entity.player.Inventory.SLOT_OFFHAND;
        inventory.setItem(offhand, removed);
        var offhandMenu = new ToolboxMenu(2, inventory, offhand, identity, false);
        player.containerMenu = offhandMenu;
        offhandMenu.clicked(0, offhand, ContainerInput.SWAP, player);
        helper.assertTrue(
                offhandMenu.stillValid(player) && offhandMenu.getSlot(0).hasItem(),
                "Offhand swap must preserve its opened toolbox and contents");
        helper.succeed();
    }

    static void crafting(GameTestHelper helper) {
        var key =
                ResourceKey.<Recipe<?>>create(
                        Registries.RECIPE, Identifier.parse("ic2:shaped/metal_former"));
        var recipe =
                (CraftingRecipe) helper.getLevel().recipeAccess().byKey(key).orElseThrow().value();
        var box = ModToolbox.ITEM.toStack();
        box.set(ModDataComponents.TOOLBOX_ID, UUID.randomUUID());
        var input =
                CraftingInput.of(
                        3,
                        3,
                        List.of(
                                ItemStack.EMPTY,
                                item("circuit"),
                                ItemStack.EMPTY,
                                box,
                                item("machine"),
                                ModToolbox.ITEM.toStack(),
                                item("coil"),
                                item("coil"),
                                item("coil")));
        helper.assertTrue(
                recipe.matches(input, helper.getLevel()),
                "Previously opened empty boxes must craft the metal former");
        var access = ItemAccess.forStack(box);
        new ToolboxHandler(access).set(0, ItemResource.of(ModTools.WRENCH.get()), 1);
        var occupied =
                CraftingInput.of(
                        3,
                        3,
                        List.of(
                                ItemStack.EMPTY,
                                item("circuit"),
                                ItemStack.EMPTY,
                                access.getResource().toStack(),
                                item("machine"),
                                ModToolbox.ITEM.toStack(),
                                item("coil"),
                                item("coil"),
                                item("coil")));
        helper.assertTrue(
                !recipe.matches(occupied, helper.getLevel()),
                "Crafting must never consume a box containing tools");
        helper.succeed();
    }

    private static ItemStack item(String name) {
        return new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse("ic2:" + name)));
    }

    private ToolboxTests() {}
}
