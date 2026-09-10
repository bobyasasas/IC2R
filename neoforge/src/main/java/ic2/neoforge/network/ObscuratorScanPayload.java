package ic2.neoforge.network;

import ic2.neoforge.component.ModDataComponents;
import ic2.neoforge.component.ObscuratorReference;
import ic2.neoforge.item.ElectricItemEnergy;
import ic2.neoforge.item.ObscuratorItem;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Client-to-server obscurator scan upload, replacing the legacy sendPlayerItemData path. The server
 * re-checks the energy cost before storing the sampled reference on the held item.
 */
public record ObscuratorScanPayload(
        int slot, String blockId, String variant, int side, int[] colorMultipliers)
        implements CustomPacketPayload {
    public static final int MAX_COLOR_MULTIPLIERS = 64;
    private static final Identifier ID = Identifier.fromNamespaceAndPath("ic2", "obscurator_scan");
    public static final Type<ObscuratorScanPayload> TYPE = new Type<>(ID);

    private static final StreamCodec<ByteBuf, int[]> COLOR_MULTIPLIERS =
            ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.VAR_INT, MAX_COLOR_MULTIPLIERS)
                    .map(ObscuratorScanPayload::toIntArray, ObscuratorScanPayload::toList);

    public static final StreamCodec<ByteBuf, ObscuratorScanPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    ObscuratorScanPayload::slot,
                    ByteBufCodecs.STRING_UTF8,
                    ObscuratorScanPayload::blockId,
                    ByteBufCodecs.STRING_UTF8,
                    ObscuratorScanPayload::variant,
                    ByteBufCodecs.VAR_INT,
                    ObscuratorScanPayload::side,
                    COLOR_MULTIPLIERS,
                    ObscuratorScanPayload::colorMultipliers,
                    ObscuratorScanPayload::new);

    public ObscuratorScanPayload {
        colorMultipliers = colorMultipliers.clone();
    }

    @Override
    public int[] colorMultipliers() {
        return this.colorMultipliers.clone();
    }

    public static void handle(ObscuratorScanPayload payload, IPayloadContext context) {
        applyScan(payload, context.player());
    }

    /** Same-path validation used by the payload handler and covered directly by GameTests. */
    public static boolean applyScan(ObscuratorScanPayload payload, Player player) {
        ItemStack stack = player.getInventory().getItem(payload.slot());
        if (!(stack.getItem() instanceof ObscuratorItem)) return false;
        if (ElectricItemEnergy.charge(stack) < ObscuratorItem.SCAN_ENERGY) return false;
        int[] colors = payload.colorMultipliers();
        if (colors.length == 0) return false;
        stack.set(
                ModDataComponents.OBSCURATOR_REFERENCE,
                new ObscuratorReference(
                        payload.blockId(), payload.variant(), payload.side(), colors));
        ElectricItemEnergy.discharge(stack, ObscuratorItem.SCAN_ENERGY, 2, true, false, false);
        return true;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static int[] toIntArray(List<Integer> values) {
        return values.stream().mapToInt(Integer::intValue).toArray();
    }

    private static ArrayList<Integer> toList(int[] values) {
        return new ArrayList<>(Arrays.stream(values).boxed().toList());
    }
}
