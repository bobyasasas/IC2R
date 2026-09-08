package ic2.neoforge.test;

import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;

import ic2.neoforge.machine.WorldWind;

import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.server.level.ServerLevel;

/** Native test batches isolate their controlled wind and restore the original world state. */
final class StrongWindEnvironment implements TestEnvironmentDefinition<WorldWind> {
    static final MapCodec<StrongWindEnvironment> CODEC = MapCodec.unit(new StrongWindEnvironment());

    @Override
    public WorldWind setup(ServerLevel level) {
        var original = WorldWind.get(level);
        var encoded =
                WorldWind.CODEC
                        .encodeStart(JsonOps.INSTANCE, original)
                        .getOrThrow()
                        .getAsJsonObject();
        encoded.addProperty("strength", 30);
        encoded.addProperty("ticks", 0);
        level.getDataStorage()
                .set(WorldWind.TYPE, WorldWind.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow());
        return original;
    }

    @Override
    public void teardown(ServerLevel level, WorldWind original) {
        level.getDataStorage().set(WorldWind.TYPE, original);
    }

    @Override
    public MapCodec<StrongWindEnvironment> codec() {
        return CODEC;
    }
}
