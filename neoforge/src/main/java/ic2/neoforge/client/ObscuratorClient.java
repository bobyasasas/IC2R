package ic2.neoforge.client;

import ic2.neoforge.client.model.ObscuredFaceSampler;
import ic2.neoforge.client.model.ObscuredWallModel;
import ic2.neoforge.component.ModDataComponents;
import ic2.neoforge.component.ObscuratorReference;
import ic2.neoforge.item.ObscuratorSampling;
import ic2.neoforge.network.ObscuratorScanPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.RegisterBlockStateModels;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.Arrays;

/**
 * Client-side obscurator scan: derives the sampled face rectangles from the baked model of the
 * targeted block state, colors each sample through the block tint sources, deduplicates against the
 * stored reference and uploads the result through the scan payload.
 */
public final class ObscuratorClient {
    public static void register() {
        ObscuratorSampling.register(ObscuratorClient::scan);
    }

    static void registerModels(RegisterBlockStateModels event) {
        event.registerModel(
                net.minecraft.resources.Identifier.fromNamespaceAndPath(
                        "ic2", ObscuredWallModel.ID),
                ObscuredWallModel.Unbaked.CODEC);
    }

    private static boolean scan(
            ItemStack stack, Player player, Level level, BlockPos pos, Direction side) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return false;

        ObscuredFaceSampler.RenderInfo info = ObscuredFaceSampler.renderInfo(state, side);
        if (info == null) return false;

        BlockColors blockColors = Minecraft.getInstance().getBlockColors();
        int[] colorMultipliers = new int[info.samples.length];
        for (int index = 0; index < info.samples.length; index++) {
            colorMultipliers[index] = tint(state, level, pos, blockColors, index);
        }

        ObscuratorReference reference =
                new ObscuratorReference(
                        blockId(state),
                        ObscuratorReference.variantOf(state),
                        side.ordinal(),
                        colorMultipliers);
        ObscuratorReference current = stack.get(ModDataComponents.OBSCURATOR_REFERENCE);
        if (current != null && sameReference(current, reference)) return false;

        int slot = player.getInventory().getSelectedSlot();
        ClientPacketDistributor.sendToServer(
                new ObscuratorScanPayload(
                        slot,
                        reference.blockId(),
                        reference.variant(),
                        reference.side(),
                        reference.colorMultipliers()));
        return true;
    }

    private static int tint(
            BlockState state, Level level, BlockPos pos, BlockColors blockColors, int layer) {
        var source = blockColors.getTintSource(state, layer);
        if (source == null) return -1;
        if (!(level instanceof net.minecraft.client.renderer.block.BlockAndTintGetter tintLevel))
            return source.color(state);
        return source.colorInWorld(state, tintLevel, pos);
    }

    private static String blockId(BlockState state) {
        var key = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return key.toString();
    }

    private static boolean sameReference(ObscuratorReference current, ObscuratorReference sampled) {
        return current.blockId().equals(sampled.blockId())
                && current.variant().equals(sampled.variant())
                && current.side() == sampled.side()
                && Arrays.equals(current.colorMultipliers(), sampled.colorMultipliers());
    }

    private ObscuratorClient() {}
}
