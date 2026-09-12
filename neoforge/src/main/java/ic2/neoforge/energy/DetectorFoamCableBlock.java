package ic2.neoforge.energy;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import ic2.core.energy.grid.CableSpec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * The foam-shelled detector cable: while the shell cures on its one-tick loop the probe rides
 * along every tick; once hardened the loop hands over to the plain detector's thirty-two tick
 * probe, started on the transition.
 */
public class DetectorFoamCableBlock extends FoamCableBlock {
    /** Shared with the plain detector so the redstone helpers read one property instance. */
    public static final BooleanProperty ACTIVE = DetectorCableBlock.ACTIVE;
    public static final MapCodec<DetectorFoamCableBlock> CODEC =
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
                                    .apply(instance, DetectorFoamCableBlock::new));

    public DetectorFoamCableBlock(
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
    protected void onPlace(
            BlockState state, Level level, BlockPos pos, BlockState previous, boolean moved) {
        super.onPlace(state, level, pos, previous, moved);
        DetectorCableBlock.scheduleProbe(this, level, pos);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(FOAM).isSoft()) {
            DetectorCableBlock.energyProbe(state, level, pos, this);
            return;
        }
        tickFoamHardening(state, level, pos, random);
        BlockState current = level.getBlockState(pos);
        if (!current.is(this)) return;
        DetectorCableBlock.probeNow(current, level, pos);
        // The curing loop dies on hardening; hand the probe over to its own thirty-two tick chain.
        if (current.getValue(FOAM).isHard())
            level.scheduleTick(pos, this, DetectorCableBlock.TICK_RATE);
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        return DetectorCableBlock.signalFor(state);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(
            BlockState state, Level level, BlockPos pos, Direction side) {
        return DetectorCableBlock.analogFor(state, level, pos, specification());
    }
}
