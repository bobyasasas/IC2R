package ic2.neoforge.energy;

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
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class CableBlock extends Block {
    private static final com.mojang.serialization.Codec<CableSpec.Material> MATERIAL_CODEC =
            com.mojang.serialization.Codec.STRING.xmap(
                    name -> CableSpec.Material.valueOf(name.toUpperCase(java.util.Locale.ROOT)),
                    material -> material.name().toLowerCase(java.util.Locale.ROOT));
    public static final MapCodec<CableBlock> CODEC =
            RecordCodecBuilder.mapCodec(
                    instance ->
                            instance.group(
                                            MATERIAL_CODEC
                                                    .fieldOf("material")
                                                    .forGetter(block -> block.material),
                                            com.mojang.serialization.Codec.INT
                                                    .fieldOf("insulation")
                                                    .forGetter(block -> block.insulation),
                                            propertiesCodec())
                                    .apply(instance, CableBlock::new));
    private final CableSpec.Material material;
    private final int insulation;
    private final CableSpec specification;
    private final VoxelShape[] shapes = new VoxelShape[64];

    public CableBlock(CableSpec.Material material, int insulation, Properties properties) {
        super(properties);
        this.material = material;
        this.insulation = insulation;
        specification = material.insulated(insulation);
        BlockState state = stateDefinition.any();
        for (BooleanProperty property : PipeBlock.PROPERTY_BY_DIRECTION.values())
            state = state.setValue(property, false);
        registerDefaultState(state);
        double low = (1 - specification.diameter()) * 8, high = 16 - low;
        for (int mask = 0; mask < shapes.length; mask++) {
            VoxelShape shape = Block.box(low, low, low, high, high, high);
            for (Direction side : Direction.values())
                if ((mask & (1 << side.ordinal())) != 0) {
                    shape =
                            Shapes.or(
                                    shape,
                                    Block.box(
                                            side == Direction.WEST ? 0 : low,
                                            side == Direction.DOWN ? 0 : low,
                                            side == Direction.NORTH ? 0 : low,
                                            side == Direction.EAST ? 16 : high,
                                            side == Direction.UP ? 16 : high,
                                            side == Direction.SOUTH ? 16 : high));
                }
            shapes[mask] = shape.optimize();
        }
    }

    public CableSpec specification() {
        return specification;
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        PipeBlock.PROPERTY_BY_DIRECTION.values().forEach(builder::add);
    }

    @Override
    protected VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int mask = 0;
        for (Direction side : Direction.values())
            if (state.getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(side)))
                mask |= 1 << side.ordinal();
        return shapes[mask];
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState();
        for (Direction side : Direction.values())
            state =
                    state.setValue(
                            PipeBlock.PROPERTY_BY_DIRECTION.get(side),
                            connects(context.getLevel(), context.getClickedPos().relative(side)));
        return state;
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            LevelReader level,
            ScheduledTickAccess ticks,
            BlockPos pos,
            Direction side,
            BlockPos neighborPos,
            BlockState neighbor,
            RandomSource random) {
        return state.setValue(
                PipeBlock.PROPERTY_BY_DIRECTION.get(side), connects(level, neighborPos));
    }

    private static boolean connects(LevelReader level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof CableBlock
                || level.getBlockState(pos).getBlock() instanceof ic2.neoforge.machine.MachineBlock;
    }

    @Override
    protected void onPlace(
            BlockState state, Level level, BlockPos pos, BlockState previous, boolean moved) {
        if (state.getBlock() != previous.getBlock() && level instanceof ServerLevel server)
            WorldEnergyNetworks.invalidate(server);
    }

    @Override
    protected void affectNeighborsAfterRemoval(
            BlockState state, ServerLevel level, BlockPos pos, boolean moved) {
        super.affectNeighborsAfterRemoval(state, level, pos, moved);
        WorldEnergyNetworks.invalidate(level);
    }
}
