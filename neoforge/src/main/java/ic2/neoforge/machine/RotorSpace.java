package ic2.neoforge.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Samples loaded chunks once per scan; unknown cells obstruct and never force chunk generation. */
final class RotorSpace {
    private final ServerLevel level;
    private final Map<Long, Optional<LevelChunk>> chunks = new HashMap<>();

    RotorSpace(ServerLevel level) {
        this.level = level;
    }

    int obstructions(
            BlockPos center,
            Direction facing,
            int diameter,
            boolean planeOnly,
            boolean water,
            MachineKind family) {
        if (facing.getAxis().isVertical()) return Integer.MAX_VALUE;
        int radius = diameter / 2 * (planeOnly ? 1 : 2);
        int length = planeOnly ? 1 : diameter * 3;
        int start = planeOnly ? 1 : -length;
        var right = facing.getClockWise();
        var sample = new BlockPos.MutableBlockPos();
        int obstructed = 0;
        for (int up = -radius; up <= radius; up++)
            for (int across = -radius; across <= radius; across++) {
                boolean occupied = false;
                for (int forward = start; forward <= length; forward++) {
                    sample.set(
                            center.getX() + facing.getStepX() * forward + right.getStepX() * across,
                            center.getY() + up,
                            center.getZ()
                                    + facing.getStepZ() * forward
                                    + right.getStepZ() * across);
                    int x = sample.getX() >> 4, z = sample.getZ() >> 4;
                    var chunk =
                            chunks.computeIfAbsent(
                                    ChunkPos.pack(x, z),
                                    ignored ->
                                            Optional.ofNullable(
                                                    level.getChunkSource().getChunkNow(x, z)));
                    if (chunk.isEmpty()) {
                        occupied = true;
                        continue;
                    }
                    var state = chunk.get().getBlockState(sample);
                    if (water ? !state.is(Blocks.WATER) : !state.isAir()) occupied = true;
                    if (!planeOnly
                            && !sample.equals(center)
                            && state.getBlock() instanceof MachineBlock machine
                            && machine.kind() == family) return -1;
                }
                if (occupied) obstructed++;
            }
        return obstructed;
    }
}
