package ic2.neoforge.energy;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import ic2.core.energy.grid.CableSpec;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.redstone.Orientation;

import javax.annotation.Nullable;

/** The foam-shelled splitter cable: same redstone gate, hidden under the construction foam. */
public class SplitterFoamCableBlock extends FoamCableBlock {
    /** Shared with the plain splitter so the grid gate reads one property instance. */
    public static final BooleanProperty ACTIVE = SplitterCableBlock.ACTIVE;
    public static final MapCodec<SplitterFoamCableBlock> CODEC =
            RecordCodecBuilder.mapCodec(
                    instance ->
                            instance.group(
                                            MATERIAL_CODEC
                                                    .fieldOf("material")
                                                    .forGetter(block -> block.material),
                                            Codec.INT
                                                    .fieldOf("insulation")
                                                    .forGetter(block -> block.insulation),
                                            propertiesCodec())
                                    .apply(instance, SplitterFoamCableBlock::new));

    public SplitterFoamCableBlock(
            CableSpec.Material material, int insulation, Properties properties) {
        super(material, insulation, properties);
        registerDefaultState(defaultBlockState().setValue(ACTIVE, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(ACTIVE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return super
                .getStateForPlacement(context)
                .setValue(ACTIVE, context.getLevel().hasNeighborSignal(context.getClickedPos()));
    }

    @Override
    protected void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block neighborBlock,
            @Nullable Orientation orientation,
            boolean movedByPiston) {
        if (!level.isClientSide()
                && state.getValue(ACTIVE) != level.hasNeighborSignal(pos)
                && level.setBlock(pos, state.cycle(ACTIVE), 3)) {
            if (level instanceof ServerLevel server) WorldEnergyNetworks.invalidate(server);
        }
        super.neighborChanged(state, level, pos, neighborBlock, orientation, movedByPiston);
    }

    /** Grid admission test used by the network rebuild; the gate itself never conducts. */
    public boolean isConducting(BlockState state) {
        return state.getValue(ACTIVE);
    }
}
