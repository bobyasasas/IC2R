package ic2.neoforge.machine;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import ic2.core.machine.WindSimulation;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.RandomSequence;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.Objects;

/** Per-dimension wind, including RNG position, owned and saved by the native world lifecycle. */
public final class WorldWind extends SavedData {
    public static final Codec<WorldWind> CODEC =
            RecordCodecBuilder.create(
                    instance ->
                            instance.group(
                                            Codec.intRange(0, 30)
                                                    .fieldOf("strength")
                                                    .forGetter(
                                                            value ->
                                                                    value.simulation
                                                                            .state()
                                                                            .strength()),
                                            Codec.intRange(0, 359)
                                                    .fieldOf("direction")
                                                    .forGetter(
                                                            value ->
                                                                    value.simulation
                                                                            .state()
                                                                            .direction()),
                                            Codec.intRange(0, 127)
                                                    .fieldOf("ticks")
                                                    .forGetter(
                                                            value ->
                                                                    value.simulation
                                                                            .state()
                                                                            .ticks()),
                                            RandomSequence.CODEC
                                                    .fieldOf("random")
                                                    .forGetter(value -> value.random))
                                    .apply(instance, WorldWind::new));
    public static final SavedDataType<WorldWind> TYPE =
            new SavedDataType<>(
                    Identifier.fromNamespaceAndPath("ic2", "wind"),
                    level -> create(Objects.requireNonNull(level)),
                    level -> CODEC);
    private final WindSimulation simulation;
    private final RandomSequence random;

    private WorldWind(int strength, int direction, int ticks, RandomSequence random) {
        this.simulation = new WindSimulation(new WindSimulation.State(strength, direction, ticks));
        this.random = random;
    }

    public static WorldWind create(ServerLevel level) {
        var random = new RandomSequence(level.getSeed(), level.dimension().identifier());
        return new WorldWind(
                5 + random.random().nextInt(20), random.random().nextInt(360), 0, random);
    }

    public static WorldWind get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public static void tick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) get(level).advance();
    }

    public void advance() {
        simulation.tick(random.random()::nextInt);
        setDirty();
    }

    public double windAt(ServerLevel level, int y) {
        return simulation.windAt(
                y, level.getHeight(), level.getSeaLevel(), level.isRaining(), level.isThundering());
    }
}
