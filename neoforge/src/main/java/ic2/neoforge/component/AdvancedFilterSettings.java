package ic2.neoforge.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Legacy UpgradeSettings tag ("meta"/"energy" settings on an advanced ejector/pulling upgrade):
 * an on/off flag plus the dev-only numeric comparison configuration. {@code active} mirrors
 * {@code comparisonType.enabled()}; the bounds are always present because the port's config screen
 * uses value steppers instead of text boxes, so the legacy empty-box downgrade ladder has no
 * reachable state.
 */
public record AdvancedFilterSettings(
        boolean active, int type, int normalBound, int normalOp, int extraBound, int extraOp) {
    public static final AdvancedFilterSettings DEFAULT =
            new AdvancedFilterSettings(
                    false,
                    ComparisonType.DIRECT.ordinal(),
                    0,
                    ComparisonSetting.LESS.ordinal(),
                    0,
                    ComparisonSetting.LESS.ordinal());

    public static final Codec<AdvancedFilterSettings> CODEC =
            RecordCodecBuilder.create(
                    instance ->
                            instance.group(
                                            Codec.BOOL.fieldOf("active")
                                                    .forGetter(AdvancedFilterSettings::active),
                                            Codec.INT.fieldOf("type")
                                                    .forGetter(AdvancedFilterSettings::type),
                                            Codec.INT.fieldOf("normal_bound")
                                                    .forGetter(
                                                            AdvancedFilterSettings::normalBound),
                                            Codec.INT.fieldOf("normal_op")
                                                    .forGetter(AdvancedFilterSettings::normalOp),
                                            Codec.INT.fieldOf("extra_bound")
                                                    .forGetter(AdvancedFilterSettings::extraBound),
                                            Codec.INT.fieldOf("extra_op")
                                                    .forGetter(AdvancedFilterSettings::extraOp))
                                    .apply(instance, AdvancedFilterSettings::new));

    public static final StreamCodec<ByteBuf, AdvancedFilterSettings> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL,
                    AdvancedFilterSettings::active,
                    ByteBufCodecs.VAR_INT,
                    AdvancedFilterSettings::type,
                    ByteBufCodecs.VAR_INT,
                    AdvancedFilterSettings::normalBound,
                    ByteBufCodecs.VAR_INT,
                    AdvancedFilterSettings::normalOp,
                    ByteBufCodecs.VAR_INT,
                    AdvancedFilterSettings::extraBound,
                    ByteBufCodecs.VAR_INT,
                    AdvancedFilterSettings::extraOp,
                    AdvancedFilterSettings::new);

    public ComparisonType comparisonType() {
        return ComparisonType.VALUES[Math.floorMod(type, ComparisonType.VALUES.length)];
    }

    public ComparisonSetting normalSetting() {
        return setting(normalOp);
    }

    public ComparisonSetting extraSetting() {
        return setting(extraOp);
    }

    private static ComparisonSetting setting(int ordinal) {
        return ComparisonSetting.VALUES[Math.floorMod(ordinal, ComparisonSetting.VALUES.length)];
    }

    public AdvancedFilterSettings withType(ComparisonType newType) {
        return new AdvancedFilterSettings(
                newType.enabled(), newType.ordinal(), normalBound, normalOp, extraBound, extraOp);
    }

    public AdvancedFilterSettings withActive(boolean newActive) {
        return new AdvancedFilterSettings(
                newActive, type, normalBound, normalOp, extraBound, extraOp);
    }

    public AdvancedFilterSettings withNormalOp(int newOp) {
        return new AdvancedFilterSettings(
                active, type, normalBound, newOp, extraBound, extraOp);
    }

    public AdvancedFilterSettings withExtraOp(int newOp) {
        return new AdvancedFilterSettings(
                active, type, normalBound, normalOp, extraBound, newOp);
    }

    public AdvancedFilterSettings withNormalBound(int newBound) {
        return new AdvancedFilterSettings(
                active, type, newBound, normalOp, extraBound, extraOp);
    }

    public AdvancedFilterSettings withExtraBound(int newBound) {
        return new AdvancedFilterSettings(
                active, type, normalBound, normalOp, newBound, extraOp);
    }

    /** Legacy UpgradeSettings.doComparison: COMPARISON checks one bound, RANGE a window. */
    public boolean doComparison(int value) {
        var comparison = comparisonType();
        return switch (comparison) {
            case COMPARISON -> normalSetting().compare(normalBound, value);
            case RANGE ->
                    normalSetting().compare(normalBound, value)
                            && extraSetting().compare(value, extraBound);
            default -> throw new IllegalStateException("Unexpected comparison type " + comparison);
        };
    }

    /** Legacy ComparisonType: IGNORED/DIRECT defer to the filter list, COMPARISON/RANGE don't. */
    public enum ComparisonType {
        IGNORED,
        DIRECT,
        COMPARISON,
        RANGE;

        public boolean enabled() {
            return this != IGNORED;
        }

        public boolean ignoreFilters() {
            return this == IGNORED || this == DIRECT;
        }

        public static final ComparisonType[] VALUES = values();
    }

    /** Legacy ComparisonSettings: bound operators, applied as {@code compare(bound, value)}. */
    public enum ComparisonSetting {
        LESS_OR_EQUAL("<="),
        LESS("<"),
        GREATER(">"),
        GREATER_OR_EQUAL(">=");

        public static final ComparisonSetting DEFAULT = LESS;
        public static final ComparisonSetting[] VALUES = values();

        public final String symbol;

        ComparisonSetting(String symbol) {
            this.symbol = symbol;
        }

        public boolean compare(int value, int comparison) {
            return switch (this) {
                case LESS_OR_EQUAL -> value <= comparison;
                case LESS -> value < comparison;
                case GREATER -> value > comparison;
                case GREATER_OR_EQUAL -> value >= comparison;
            };
        }
    }
}
