package ic2.neoforge.test;

import ic2.neoforge.machine.AdvMinerBlockEntity;
import ic2.neoforge.machine.ChunkLoaderBlockEntity;
import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.menu.AdvancedEditOreMenu;
import ic2.neoforge.menu.AdvancedUpgradeMenu;
import ic2.neoforge.menu.AdvancedValueConfigMenu;
import ic2.neoforge.menu.ContainmentBoxMenu;
import ic2.neoforge.menu.CropAnalyzerMenu;
import ic2.neoforge.menu.MachineMenu;
import ic2.neoforge.menu.MeterMenu;
import ic2.neoforge.menu.MiningFilterMenu;
import ic2.neoforge.menu.ScannerMenu;
import ic2.neoforge.menu.ToolboxMenu;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.registration.ModToolbox;
import ic2.neoforge.registration.ModTools;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.GameType;

import io.netty.buffer.Unpooled;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/** Menu-catalog audit: every machine menu must bind its inventory exactly once. */
final class MenuAuditTests {
    private static final int PLAYER_SLOTS = 36;

    /** Menu slot totals that diverge from kind.slots(); each divergence is a recorded gap. */
    private static int auditedMachineSlots(MachineKind kind) {
        return switch (kind) {
            // The filter grid (42 editor slots) sits beside the machine inventory.
            case SORTING_MACHINE -> 6 * 7 + kind.slots();
            // Three computed crafting previews beside the 31 real slots.
            case INDUSTRIAL_WORKBENCH -> 34;
            // Nine hologram templates live outside the machine inventory.
            case BATCH_CRAFTER -> 33;
            // Legacy ContainerElectricBlock row: the four worn-armor slots.
            case BATBOX, CESU, MFE, MFSU -> kind.slots() + 4;
            // Legacy ContainerMagnetizer shows the worn feet slot beside the discharge slot.
            case MAGNETIZER -> kind.slots() + 1;
            default -> kind.slots();
        };
    }

    static void everyMachineMenuBindsInventorySlotsExactlyOnce(GameTestHelper helper) {
        for (MachineKind kind : MachineKind.values()) {
            var player = helper.makeMockPlayer(GameType.SURVIVAL);
            var menu = new MachineMenu(1, player.getInventory(), BlockPos.ZERO, kind);
            int playerSlotCount = kind == MachineKind.STEAM_GENERATOR ? 0 : PLAYER_SLOTS;
            int machineSlotCount = menu.slots.size() - playerSlotCount;
            helper.assertTrue(
                    machineSlotCount == auditedMachineSlots(kind),
                    kind + " menu must expose exactly its inventory slots: expected "
                            + auditedMachineSlots(kind) + ", got " + machineSlotCount);
            if (kind == MachineKind.STEAM_GENERATOR)
                helper.assertTrue(
                        menu.slots.isEmpty(),
                        "The legacy steam-generator control panel must not overlap player slots");
            Set<Long> bound = new HashSet<>();
            for (int slot = 0; slot < machineSlotCount; slot++) {
                var menuSlot = menu.slots.get(slot);
                long key;
                if (menuSlot
                        instanceof net.neoforged.neoforge.transfer.item.ResourceHandlerSlot) {
                    // StackCopySlot shares one static empty container, so slots are identified
                    // by their backing handler plus the handler slot index.
                    key = ((long) System.identityHashCode(handlerOf(menuSlot)) << 32)
                            | menuSlot.getContainerSlot();
                } else {
                    // Computed and hologram slots own a private container; coordinates are unique.
                    key = Long.MIN_VALUE | ((long) menuSlot.x << 16) | menuSlot.y;
                }
                helper.assertTrue(
                        bound.add(key),
                        kind + " machine slot " + slot + " must not overlap another slot binding"
                                + " (handler slot " + menuSlot.getContainerSlot() + ")");
            }
        }
        helper.succeed();
    }

    private static Object handlerOf(net.minecraft.world.inventory.Slot slot) {
        try {
            var field = net.neoforged.neoforge.transfer.item.ResourceHandlerSlot.class
                    .getDeclaredField("handler");
            field.setAccessible(true);
            return field.get(slot);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot identify slot handler", exception);
        }
    }

    static void chunkLoaderMenuMatchesSingleDischargeLayout(GameTestHelper helper) {
        helper.setBlock(
                new BlockPos(2, 2, 2),
                ModMachines.block(MachineKind.CHUNK_LOADER).defaultBlockState());
        ChunkLoaderBlockEntity loader =
                helper.getBlockEntity(new BlockPos(2, 2, 2), ChunkLoaderBlockEntity.class);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var menu = new MachineMenu(1, player.getInventory(), loader);
        helper.assertTrue(
                menu.slots.size() == PLAYER_SLOTS + 5,
                "Chunk loader menu must be one discharge slot plus four upgrades and the"
                        + " player inventory");
        helper.assertTrue(
                menu.slots.getFirst().getContainerSlot() == 0,
                "Chunk loader discharge must bind the only non-upgrade inventory slot");
        helper.succeed();
    }

    static void advancedMinerMenuMatchesLiveInventory(GameTestHelper helper) {
        helper.setBlock(
                new BlockPos(2, 2, 2),
                ModMachines.block(MachineKind.ADV_MINER).defaultBlockState());
        AdvMinerBlockEntity miner =
                helper.getBlockEntity(new BlockPos(2, 2, 2), AdvMinerBlockEntity.class);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var menu = new MachineMenu(1, player.getInventory(), miner);
        helper.assertTrue(
                miner.inventory().size() == MachineKind.ADV_MINER.slots(),
                "Advanced miner inventory must contain its scanner, card, filters and four upgrades");
        helper.assertTrue(
                menu.slots.size() == PLAYER_SLOTS + MachineKind.ADV_MINER.slots(),
                "Advanced miner menu must not bind beyond the live block entity inventory");
        for (int index = 0; index < MachineKind.ADV_MINER.slots(); index++)
            menu.slots.get(index).getItem();
        helper.succeed();
    }

    /**
     * Legacy Ic2ScreenHandlers.registerManagedItem registers one shared factory for the whole
     * hand-held item menu family; the port registers one data-driven MenuType per item instead
     * (the generic DynamicContainer&lt;HandHeldInventory&gt; has no dedicated port equivalent).
     * This audit walks the whole nine-member family and proves each MenuType is registered under
     * the legacy family id and that its client data factory actually builds the dedicated menu.
     */
    static void handheldMenuFamilyMatchesLegacy(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var inventory = player.getInventory();
        var registries = helper.getLevel().registryAccess();

        helper.assertTrue(ModToolbox.MENU.isBound(), "tool_box menu must be registered");
        familyMenu(
                helper,
                ModToolbox.MENU.value(),
                "tool_box",
                registries,
                buf -> {
                    buf.writeVarInt(0);
                    buf.writeUUID(UUID.randomUUID());
                },
                ToolboxMenu.class,
                inventory);
        familyMenu(
                helper,
                ModTools.MINING_FILTER_MENU.value(),
                "mining_filter",
                registries,
                buf -> buf.writeVarInt(0),
                MiningFilterMenu.class,
                inventory);
        familyMenu(
                helper,
                ModTools.CROP_ANALYZER_MENU.value(),
                "crop_analyzer",
                registries,
                buf -> buf.writeVarInt(0),
                CropAnalyzerMenu.class,
                inventory);
        familyMenu(
                helper,
                ModTools.METER_MENU.value(),
                "meter",
                registries,
                buf -> buf.writeBlockPos(BlockPos.ZERO),
                MeterMenu.class,
                inventory);
        familyMenu(
                helper,
                ModTools.CONTAINMENT_BOX_MENU.value(),
                "containment_box",
                registries,
                buf -> buf.writeVarInt(0),
                ContainmentBoxMenu.class,
                inventory);
        familyMenu(
                helper,
                ModTools.SCANNER_MENU.value(),
                "scanner",
                registries,
                buf -> buf.writeVarInt(0),
                ScannerMenu.class,
                inventory);
        familyMenu(
                helper,
                ModTools.ADVANCED_UPGRADE_MENU.value(),
                "advanced_upgrade",
                registries,
                buf -> buf.writeVarInt(0),
                AdvancedUpgradeMenu.class,
                inventory);
        familyMenu(
                helper,
                ModTools.ADVANCED_VALUE_CONFIG_MENU.value(),
                "advanced_value_config",
                registries,
                buf -> {
                    buf.writeVarInt(0);
                    buf.writeByte(0);
                },
                AdvancedValueConfigMenu.class,
                inventory);
        familyMenu(
                helper,
                ModTools.ADVANCED_EDIT_ORE_MENU.value(),
                "advanced_edit_ore",
                registries,
                buf -> buf.writeVarInt(0),
                AdvancedEditOreMenu.class,
                inventory);
        helper.succeed();
    }

    private static <T extends AbstractContainerMenu> void familyMenu(
            GameTestHelper helper,
            MenuType<T> type,
            String id,
            net.minecraft.core.RegistryAccess registries,
            Consumer<RegistryFriendlyByteBuf> data,
            Class<T> menuClass,
            net.minecraft.world.entity.player.Inventory inventory) {
        var expected = Identifier.fromNamespaceAndPath("ic2", id);
        var key = registries.lookupOrThrow(Registries.MENU).getKey(type);
        helper.assertTrue(
                expected.equals(key),
                "hand-held menu family id must match the legacy managed-item registration: "
                        + id + " but got " + key);
        var buf = new RegistryFriendlyByteBuf(new FriendlyByteBuf(Unpooled.buffer()), registries);
        data.accept(buf);
        var menu = type.create(1, inventory, buf);
        helper.assertTrue(
                menu != null && menuClass.isInstance(menu),
                "menu " + id + " data factory must build its dedicated container");
    }

    private MenuAuditTests() {}
}
