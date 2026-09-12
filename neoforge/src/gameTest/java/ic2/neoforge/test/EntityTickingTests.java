package ic2.neoforge.test;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;

import java.util.function.Consumer;

/**
 * GameTestServer places the whole test grid at a random position up to ±15 million blocks out and
 * starts batches as soon as structure blocks are placed; whether a plot's chunk is promoted to
 * entity-ticking level in time is an async race, so in roughly a third of runs entity-driven
 * assertions (projectiles, boats, random-tick fire) freeze and flake. Every test is wrapped to
 * force its plot's chunk neighbourhood to forced (entity-ticking) level before the test body
 * runs.
 */
final class EntityTickingTests {
    private EntityTickingTests() {}

    static Consumer<GameTestHelper> wrap(Consumer<GameTestHelper> test) {
        return helper -> {
            ServerLevel level = helper.getLevel();
            BlockPos origin = helper.absolutePos(BlockPos.ZERO);
            int chunkX = origin.getX() >> 4;
            int chunkZ = origin.getZ() >> 4;
            for (int dx = 0; dx <= 1; dx++) {
                for (int dz = 0; dz <= 1; dz++) {
                    level.setChunkForced(chunkX + dx, chunkZ + dz, true);
                }
            }
            test.accept(helper);
        };
    }
}
