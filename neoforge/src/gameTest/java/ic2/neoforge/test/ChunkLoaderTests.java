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

/** Legacy TileEntityChunkLoader: power-following tickets, break release and NBT round trip. */
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

    private static ChunkLoaderBlockEntity loader(GameTestHelper helper) {
        var pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, ModMachines.block(MachineKind.CHUNK_LOADER));
        return helper.getBlockEntity(pos, ChunkLoaderBlockEntity.class);
    }

    private ChunkLoaderTests() {}
}
