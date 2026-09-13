package ic2.neoforge.test;

import ic2.neoforge.machine.ChunkLoaderBlockEntity;
import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.menu.MachineMenu;
import ic2.neoforge.registration.ModMachines;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.GameType;

import java.util.HashSet;
import java.util.Set;

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
            default -> kind.slots();
        };
    }

    static void everyMachineMenuBindsInventorySlotsExactlyOnce(GameTestHelper helper) {
        for (MachineKind kind : MachineKind.values()) {
            var player = helper.makeMockPlayer(GameType.SURVIVAL);
            var menu = new MachineMenu(1, player.getInventory(), BlockPos.ZERO, kind);
            int machineSlotCount = menu.slots.size() - PLAYER_SLOTS;
            helper.assertTrue(
                    machineSlotCount == auditedMachineSlots(kind),
                    kind + " menu must expose exactly its inventory slots: expected "
                            + auditedMachineSlots(kind) + ", got " + machineSlotCount);
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

    private MenuAuditTests() {}
}
