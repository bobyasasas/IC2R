package ic2.neoforge.component;

import com.mojang.serialization.Codec;

import io.netty.buffer.ByteBuf;

import net.minecraft.core.GlobalPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;

/** Immutable, dimension-aware links; bounded in both persistent and network representations. */
public record RemoteLinks(List<GlobalPos> targets) {
    public static final int MAX_TARGETS = 1024;
    public static final RemoteLinks EMPTY = new RemoteLinks(List.of());
    public static final Codec<RemoteLinks> CODEC =
            GlobalPos.CODEC.listOf(0, MAX_TARGETS).xmap(RemoteLinks::new, RemoteLinks::targets);
    public static final StreamCodec<ByteBuf, RemoteLinks> STREAM_CODEC =
            ByteBufCodecs.collection(ArrayList::new, GlobalPos.STREAM_CODEC, MAX_TARGETS)
                    .map(RemoteLinks::new, links -> new ArrayList<>(links.targets));

    public RemoteLinks {
        targets =
                targets.stream()
                        .map(p -> GlobalPos.of(p.dimension(), p.pos().immutable()))
                        .distinct()
                        .toList();
        if (targets.size() > MAX_TARGETS) {
            throw new IllegalArgumentException("Too many remote targets");
        }
    }

    public RemoteLinks add(GlobalPos position) {
        if (targets.contains(position)) return this;
        var changed = new ArrayList<>(targets);
        changed.add(position);
        return new RemoteLinks(changed);
    }

    public RemoteLinks remove(GlobalPos position) {
        return new RemoteLinks(targets.stream().filter(p -> !p.equals(position)).toList());
    }
}
