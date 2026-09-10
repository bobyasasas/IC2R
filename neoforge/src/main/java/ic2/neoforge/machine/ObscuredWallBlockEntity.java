package ic2.neoforge.machine;

import ic2.neoforge.component.ObscuratorReference;
import ic2.neoforge.model.WallRenderState;
import ic2.neoforge.registration.ModObscurator;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.model.data.ModelData;
import net.neoforged.neoforge.model.data.ModelProperty;

import org.jspecify.annotations.Nullable;

import java.util.Arrays;

/**
 * Stores the wall color plus one sampled face reference per side, mirroring the legacy
 * TileEntityWall and its Obscuration component. The full render state is exposed through model data
 * because chunk meshing runs off the main thread.
 */
public final class ObscuredWallBlockEntity extends BlockEntity {
    public static final ModelProperty<WallRenderState> RENDER_STATE = new ModelProperty<>();

    /** One sampled face reference; immutable so meshing threads can read it safely. */
    public record FaceData(
            BlockState referenceState, Direction referenceSide, int[] colorMultipliers) {
        public FaceData {
            colorMultipliers = colorMultipliers.clone();
        }
    }

    private DyeColor color = DyeColor.LIGHT_GRAY;
    private final FaceData @Nullable [] faces = new FaceData[6];
    private WallRenderState renderState = WallRenderState.of(this.color, null);

    public ObscuredWallBlockEntity(BlockPos pos, BlockState state) {
        super(ModObscurator.OBSCURED_WALL_ENTITY.get(), pos, state);
    }

    public WallRenderState renderState() {
        return this.renderState;
    }

    public DyeColor color() {
        return this.color;
    }

    /**
     * Legacy TileEntityWall.initializeFromWall: stores the wall color and the sampled face the
     * obscurator is currently holding.
     */
    public void initialize(
            DyeColor color,
            Direction face,
            BlockState referenceState,
            Direction referenceSide,
            int[] colorMultipliers) {
        this.color = color;
        this.faces[face.ordinal()] = new FaceData(referenceState, referenceSide, colorMultipliers);
        this.refreshRenderState();
    }

    /**
     * Applies one sampled face, mirroring Obscuration.applyObscuration; identical data is ignored
     * so repeated applications do not dirty the world.
     */
    public boolean applyFace(
            Direction side,
            BlockState referenceState,
            Direction referenceSide,
            int[] colorMultipliers) {
        int index = side.ordinal();
        FaceData previous = this.faces[index];
        if (previous != null
                && previous.referenceState() == referenceState
                && previous.referenceSide() == referenceSide
                && Arrays.equals(previous.colorMultipliers(), colorMultipliers)) {
            return false;
        }
        this.faces[index] = new FaceData(referenceState, referenceSide, colorMultipliers);
        this.refreshRenderState();
        return true;
    }

    public void refreshRenderState() {
        this.renderState = WallRenderState.of(this.color, this.faces.clone());
        this.setChanged();
        this.requestModelDataUpdate();
    }

    @Override
    public ModelData getModelData() {
        return ModelData.builder().with(RENDER_STATE, this.renderState).build();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putByte("color", (byte) this.color.getId());
        for (Direction side : Direction.values()) {
            FaceData data = this.faces[side.ordinal()];
            if (data == null) continue;
            ValueOutput face = output.child("face_" + side.ordinal());
            face.putString(
                    "block",
                    BuiltInRegistries.BLOCK.getKey(data.referenceState().getBlock()).toString());
            face.putString("variant", ObscuratorReference.variantOf(data.referenceState()));
            face.putByte("ref_side", (byte) data.referenceSide().ordinal());
            face.putIntArray("color_muls", data.colorMultipliers());
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.color = DyeColor.byId(input.getByteOr("color", (byte) DyeColor.LIGHT_GRAY.getId()));
        Arrays.fill(this.faces, null);
        for (Direction side : Direction.values()) {
            input.child("face_" + side.ordinal())
                    .ifPresent(
                            face -> {
                                String blockId = face.getStringOr("block", "");
                                String variant = face.getStringOr("variant", "normal");
                                byte rawSide =
                                        face.getByteOr("ref_side", (byte) Direction.UP.ordinal());
                                int[] colorMultipliers =
                                        face.getIntArray("color_muls").orElse(new int[0]);
                                Direction referenceSide =
                                        rawSide >= 0 && rawSide < 6
                                                ? Direction.values()[rawSide]
                                                : Direction.UP;
                                BlockState reference = resolveState(blockId, variant);
                                if (reference != null && colorMultipliers.length > 0) {
                                    this.faces[side.ordinal()] =
                                            new FaceData(
                                                    reference, referenceSide, colorMultipliers);
                                }
                            });
        }
        this.renderState = WallRenderState.of(this.color, this.faces.clone());
    }

    private static @Nullable BlockState resolveState(String blockId, String variant) {
        return ObscuratorReference.resolveState(blockId, variant);
    }

    @Override
    public net.minecraft.nbt.CompoundTag getUpdateTag(
            net.minecraft.core.HolderLookup.Provider registries) {
        return this.saveWithFullMetadata(registries);
    }

    @Override
    public net.minecraft.network.protocol.Packet<
                    net.minecraft.network.protocol.game.ClientGamePacketListener>
            getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection connection, ValueInput input) {
        super.onDataPacket(connection, input);
        this.loadAdditional(input);
        this.requestModelDataUpdate();
    }
}
