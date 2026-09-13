package ic2.neoforge.test;

import ic2.neoforge.machine.ChunkLoaderBlockEntity;
import ic2.neoforge.machine.ChunkLoaderTickets;
import ic2.neoforge.machine.MachineBlock;
import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.registration.ModMachines;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Legacy TileEntityChunkLoader: power-following tickets, break release, NBT round trip and the
 * GuiChunkLoader nine-by-nine pick canvas wired through menu actions and family values. */
final class ChunkLoaderTests {
    static void ticketsFollowPower(GameTestHelper helper) {
        var machine = loader(helper);
        ServerLevel level = helper.getLevel();
        ChunkPos own = ChunkPos.containing(machine.getBlockPos());
        machine.energy().forceAdd(5);
        for (int i = 0; i < 3; i++) machine.serverTick(level);
        helper.assertTrue(
                machine.getBlockState().getValue(MachineBlock.ACTIVE),
                "A powered chunk loader must be active");
        helper.assertTrue(
                ChunkLoaderTickets.holds(level, own),
                "An active loader must carry a chunk_loader ticket on its own chunk");
        helper.assertTrue(
                machine.energy().stored() == 2,
                "One EU per tick per chunk: 5 EU minus three ticks must leave 2, got "
                        + machine.energy().stored());
        machine.energy().extract(machine.energy().stored());
        machine.serverTick(level);
        helper.assertTrue(
                !machine.getBlockState().getValue(MachineBlock.ACTIVE)
                        && !ChunkLoaderTickets.holds(level, own),
                "Power loss must deactivate the loader and release its ticket");
        helper.succeed();
    }

    static void breakingReleasesTickets(GameTestHelper helper) {
        var machine = loader(helper);
        ServerLevel level = helper.getLevel();
        ChunkPos own = ChunkPos.containing(machine.getBlockPos());
        machine.energy().forceAdd(10);
        machine.serverTick(level);
        helper.assertTrue(
                ChunkLoaderTickets.holds(level, own), "The ticket must exist before breaking");
        level.destroyBlock(machine.getBlockPos(), false);
        helper.assertTrue(
                !ChunkLoaderTickets.holds(level, own),
                "Breaking the loader must release its ticket");
        helper.succeed();
    }

    static void nbtRoundTripKeepsChunks(GameTestHelper helper) {
        var machine = loader(helper);
        ChunkPos own = ChunkPos.containing(machine.getBlockPos());
        helper.assertTrue(machine.addChunk(new ChunkPos(own.x() + 1, own.z())),
                "A chunk inside the nine-by-nine window must be addable");
        helper.assertTrue(!machine.addChunk(new ChunkPos(own.x() + 8, own.z())),
                "A chunk outside the window must be rejected (legacy isChunkInRange)");
        var restored =
                (ChunkLoaderBlockEntity)
                        BlockEntity.loadStatic(
                                machine.getBlockPos(),
                                machine.getBlockState(),
                                machine.saveWithFullMetadata(helper.getLevel().registryAccess()),
                                helper.getLevel().registryAccess());
        helper.assertTrue(
                restored.loadedChunks().contains(own.pack())
                        && restored.loadedChunks()
                                .contains(new ChunkPos(own.x() + 1, own.z()).pack()),
                "The loaded-chunk set must survive a save/load round trip");
        helper.assertTrue(
                restored.loadedChunks().size() == 2,
                "Exactly the own and the picked chunk must be stored, got "
                        + restored.loadedChunks().size());
        helper.succeed();
    }

    static void canvasToggleAndLimits(GameTestHelper helper) {
        var machine = loader(helper);
        helper.assertTrue(
                machine.menuValue(3) == 0,
                "A fresh loader must show no loaded chunks until it seeds its own");
        helper.assertTrue(machine.menuValue(4) == 9, "Legacy maxChunks must stay 9");
        // Cell 40 = the machine's own chunk. Toggling an unloaded cell adds it.
        helper.assertTrue(machine.menuAction(40), "Toggling the own cell must load the own chunk");
        helper.assertTrue(
                machine.menuValue(1) == 1 << 13,
                "Cell 40 must sit in word 1 bit 13, got " + machine.menuValue(1));
        // Toggling it again must refuse (legacy removeChunk refuses the machine's own chunk).
        helper.assertTrue(
                !machine.menuAction(40),
                "The own chunk must refuse removal through the canvas");
        helper.assertTrue(
                machine.menuValue(3) == 1,
                "The refused removal must not change the loaded count");
        helper.assertTrue(machine.menuAction(41), "Cell 41 must be addable");
        helper.assertTrue(
                machine.menuValue(1) == ((1 << 13) | (1 << 14)),
                "Cells 40 and 41 must decode to bits 13 and 14 of word 1");
        helper.assertTrue(machine.menuAction(41), "Cell 41 must toggle back off");
        helper.assertTrue(
                machine.menuValue(1) == 1 << 13 && machine.menuValue(3) == 1,
                "The toggle off must clear only cell 41");
        helper.assertTrue(machine.menuAction(41), "Cell 41 must toggle on again");
        // Cells 0..6 are the row four chunks north of the machine; seven more hit the cap.
        for (int cell = 0; cell < 7; cell++)
            helper.assertTrue(machine.menuAction(cell), "Cell " + cell + " must be addable");
        helper.assertTrue(
                machine.menuValue(3) == 9, "Nine picks must reach the legacy maxChunks cap");
        helper.assertTrue(
                !machine.menuAction(7), "A tenth chunk must be refused (legacy maxChunks)");
        helper.assertTrue(
                machine.menuValue(0) == 0x7f
                        && machine.menuValue(1) == ((1 << 13) | (1 << 14))
                        && machine.menuValue(2) == 0,
                "The three words must mirror the picked cells exactly");
        helper.succeed();
    }

    static void canvasPersistsAcrossReload(GameTestHelper helper) {
        var machine = loader(helper);
        helper.assertTrue(machine.menuAction(40), "The own chunk must be loadable");
        helper.assertTrue(machine.menuAction(41), "Cell 41 must be addable");
        helper.assertTrue(machine.menuAction(72), "Cell 72 (south-east corner) must be addable");
        int[] before = {machine.menuValue(0), machine.menuValue(1), machine.menuValue(2)};
        var restored =
                (ChunkLoaderBlockEntity)
                        BlockEntity.loadStatic(
                                machine.getBlockPos(),
                                machine.getBlockState(),
                                machine.saveWithFullMetadata(helper.getLevel().registryAccess()),
                                helper.getLevel().registryAccess());
        helper.assertTrue(
                restored.menuValue(3) == 3,
                "The canvas count must survive the save/load round trip");
        helper.assertTrue(
                restored.menuValue(0) == before[0]
                        && restored.menuValue(1) == before[1]
                        && restored.menuValue(2) == before[2],
                "The canvas words must survive the save/load round trip");
        helper.succeed();
    }

    private static ChunkLoaderBlockEntity loader(GameTestHelper helper) {
        var pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, ModMachines.block(MachineKind.CHUNK_LOADER));
        return helper.getBlockEntity(pos, ChunkLoaderBlockEntity.class);
    }

    private ChunkLoaderTests() {}
}
