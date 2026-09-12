package ic2.neoforge.energy;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import ic2.core.energy.grid.CableSpec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Legacy EU-detector cable: a probe on a thirty-two tick cadence latches whether any packet
 * crossed it and emits a full weak redstone signal plus a comparator reading scaled over the
 * material capacity.
 */
public class DetectorCableBlock extends CableBlock {
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    /** Legacy probe cadence; the first probe is phase-shifted by a random offset. */
    static final int TICK_RATE = 32;
    public static final MapCodec<DetectorCableBlock> CODEC =
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
                                    .apply(instance, DetectorCableBlock::new));

    public DetectorCableBlock(CableSpec.Material material, int insulation, Properties properties) {
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
        scheduleProbe(this, level, pos);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        energyProbe(state, level, pos, this);
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        return state.getValue(ACTIVE) ? 15 : 0;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(
            BlockState state, Level level, BlockPos pos, Direction side) {
        return analogFor(state, level, pos, specification());
    }

    /**
     * Reschedules the probe loop unless a tick of this block is already pending; the active-state
     * latch re-enters onPlace, which must not fork a second chain.
     */
    static void scheduleProbe(Block block, Level level, BlockPos pos) {
        if (level instanceof ServerLevel server
                && !level.getBlockTicks().hasScheduledTick(pos, block))
            server.scheduleTick(pos, block, server.getRandom().nextInt(TICK_RATE));
    }

    /** The probe body: reschedule, then latch the energy-in reading into the active state. */
    static void energyProbe(BlockState state, ServerLevel level, BlockPos pos, Block block) {
        level.scheduleTick(pos, block, TICK_RATE);
        probeNow(state, level, pos);
    }

    /** Reads the latest distribution without rescheduling; shared with the foam counterpart. */
    static void probeNow(BlockState state, ServerLevel level, BlockPos pos) {
        boolean newActive = WorldEnergyNetworks.conductorEnergyIn(level, pos) > 0.0;
        if (newActive != state.getValue(ACTIVE)) {
            level.setBlockAndUpdate(pos, state.setValue(ACTIVE, newActive));
        } else if (newActive) {
            level.updateNeighbourForOutputSignal(pos, state.getBlock());
        }
    }

    /** Legacy comparator: fifteen steps across the material capacity, zero while unprobed. */
    static int analogFor(BlockState state, Level level, BlockPos pos, CableSpec spec) {
        if (!(level instanceof ServerLevel server)) return 0;
        double energyIn = WorldEnergyNetworks.conductorEnergyIn(server, pos);
        return (int) Mth.clamp(energyIn / spec.voltageLimit() * 15.0, 0.0, 15.0);
    }

    static int signalFor(BlockState state) {
        return state.getValue(ACTIVE) ? 15 : 0;
    }
}
