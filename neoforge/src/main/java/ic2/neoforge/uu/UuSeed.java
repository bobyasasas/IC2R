package ic2.neoforge.uu;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** One datapack UU seed: base value for an item id. */
public record UuSeed(String item, int value) {
    public static final Codec<UuSeed> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.fieldOf("item").forGetter(UuSeed::item),
                    Codec.intRange(1, 1000000).fieldOf("value").forGetter(UuSeed::value))
                    .apply(instance, UuSeed::new));
}
