package ic2.neoforge.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * One IC2 crop seed (legacy crop_seed_bag NBT): the crop id plus its growth/gain/resistance stats
 * and scan level. Stats run 0..31 like the legacy byte fields.
 */
public record CropSeed(String cropId, int growth, int gain, int resistance, int scan) {
    public static final Codec<CropSeed> CODEC =
            RecordCodecBuilder.create(
                    instance ->
                            instance.group(
                                            Codec.STRING
                                                    .fieldOf("crop_id")
                                                    .forGetter(CropSeed::cropId),
                                            Codec.intRange(0, 31).fieldOf("growth")
                                                    .forGetter(CropSeed::growth),
                                            Codec.intRange(0, 31).fieldOf("gain").forGetter(CropSeed::gain),
                                            Codec.intRange(0, 31).fieldOf("resistance")
                                                    .forGetter(CropSeed::resistance),
                                            Codec.intRange(0, 31).fieldOf("scan").forGetter(CropSeed::scan))
                                    .apply(instance, CropSeed::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CropSeed> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8,
                    CropSeed::cropId,
                    ByteBufCodecs.VAR_INT,
                    CropSeed::growth,
                    ByteBufCodecs.VAR_INT,
                    CropSeed::gain,
                    ByteBufCodecs.VAR_INT,
                    CropSeed::resistance,
                    ByteBufCodecs.VAR_INT,
                    CropSeed::scan,
                    CropSeed::new);

    public CropSeed {
        growth = clampStat(growth);
        gain = clampStat(gain);
        resistance = clampStat(resistance);
        scan = clampStat(scan);
    }

    private static int clampStat(int value) {
        return Math.clamp(value, 0, 31);
    }

    /** Legacy addScan: one analyzer scan step, capped at 31 like every other stat. */
    public CropSeed incrementScan() {
        return new CropSeed(cropId, growth, gain, resistance, scan + 1);
    }
}
