package ic2.neoforge.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * The sampled face reference stored on an obscurator, mirroring the legacy refBlock/refVariant/
 * refSide/refColorMuls NBT; the render info itself is re-derived on the client at render time.
 */
public record ObscuratorReference(
        String blockId, String variant, int side, int[] colorMultipliers) {
    public static final Codec<ObscuratorReference> CODEC =
            RecordCodecBuilder.create(
                    instance ->
                            instance.group(
                                            Codec.STRING
                                                    .fieldOf("block")
                                                    .forGetter(ObscuratorReference::blockId),
                                            Codec.STRING
                                                    .optionalFieldOf("variant", "")
                                                    .forGetter(ObscuratorReference::variant),
                                            Codec.intRange(0, 5)
                                                    .fieldOf("side")
                                                    .forGetter(ObscuratorReference::side),
                                            Codec.INT_STREAM
                                                    .xmap(IntStream::toArray, IntStream::of)
                                                    .fieldOf("color_muls")
                                                    .forGetter(
                                                            ObscuratorReference::colorMultipliers))
                                    .apply(instance, ObscuratorReference::new));
    public static final StreamCodec<ByteBuf, ObscuratorReference> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8,
                    ObscuratorReference::blockId,
                    ByteBufCodecs.STRING_UTF8,
                    ObscuratorReference::variant,
                    ByteBufCodecs.VAR_INT,
                    ObscuratorReference::side,
                    ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.VAR_INT)
                            .map(
                                    ObscuratorReference::toIntArray,
                                    colors ->
                                            new ArrayList<>(IntStream.of(colors).boxed().toList())),
                    ObscuratorReference::colorMultipliers,
                    ObscuratorReference::new);

    public ObscuratorReference {
        if (side < 0 || side > 5) {
            throw new IllegalArgumentException("Invalid obscurator side " + side);
        }
        colorMultipliers = colorMultipliers.clone();
        if (colorMultipliers.length == 0) {
            throw new IllegalArgumentException("Empty obscurator color multipliers");
        }
    }

    @Override
    public int[] colorMultipliers() {
        return colorMultipliers.clone();
    }

    /** Resolves the stored block id and property string back into a live block state. */
    public @Nullable BlockState resolveState() {
        return resolveState(this.blockId, this.variant);
    }

    /** Legacy BlockStateUtil.getState: property string parsing into a live block state. */
    public static @Nullable BlockState resolveState(String blockId, String variant) {
        Identifier parsed = Identifier.tryParse(blockId);
        if (parsed == null) return null;
        Block block = BuiltInRegistries.BLOCK.getValue(parsed);
        if (block == Blocks.AIR && !blockId.equals("minecraft:air")) return null;
        BlockState state = block.defaultBlockState();
        if (variant.isEmpty() || variant.equals("normal")) return state;
        for (String part : variant.split(",")) {
            int separator = part.indexOf('=');
            if (separator <= 0) return null;
            Property<?> property =
                    block.getStateDefinition().getProperty(part.substring(0, separator));
            if (property == null) return null;
            BlockState applied = applyValue(state, property, part.substring(separator + 1));
            if (applied == null) return null;
            state = applied;
        }
        return state;
    }

    private static <T extends Comparable<T>> @Nullable BlockState applyValue(
            BlockState state, Property<T> property, String valueName) {
        return property.getValue(valueName)
                .map(value -> state.setValue(property, value))
                .orElse(null);
    }

    /** Legacy BlockStateUtil.getVariantString: registration-ordered property listing. */
    public static String variantOf(BlockState state) {
        var properties = state.getProperties();
        if (properties.isEmpty()) return "normal";
        StringBuilder text = new StringBuilder();
        for (Property<?> property : properties) text = appendProperty(text, state, property);
        return text.toString();
    }

    private static <T extends Comparable<T>> StringBuilder appendProperty(
            StringBuilder text, BlockState state, Property<T> property) {
        if (!text.isEmpty()) text.append(',');
        return text.append(property.getName())
                .append('=')
                .append(property.getName(state.getValue(property)));
    }

    private static int[] toIntArray(List<Integer> values) {
        return values.stream().mapToInt(Integer::intValue).toArray();
    }
}
