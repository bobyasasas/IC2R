package ic2.neoforge.uu;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * One datapack UU seed: base value for an item id. Values use the legacy world-scan scale
 * (cobblestone = 1.0) and may be fractional or large (legacy config allowed e.g. 1.7E8).
 */
public record UuSeed(String item, double value) {
    public static final Codec<UuSeed> CODEC =
            RecordCodecBuilder.create(
                    instance ->
                            instance.group(
                                            Codec.STRING.fieldOf("item").forGetter(UuSeed::item),
                                            Codec.doubleRange(Double.MIN_VALUE, Double.MAX_VALUE)
                                                    .fieldOf("value")
                                                    .forGetter(UuSeed::value))
                                    .apply(instance, UuSeed::new));

    public UuSeed {
        if (!(value > 0.0) || !Double.isFinite(value)) {
            throw new IllegalArgumentException("Invalid UU seed value " + value + " for " + item);
        }
    }
}
