package ic2.neoforge.world;

import ic2.neoforge.entity.DynamiteEntity;
import ic2.neoforge.registration.ModExplosives;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/**
 * Dynamite stick (legacy BlockDynamite): directional placement with a LINKED state pairing it to
 * remote detonators. Losing its support pops the item. A redstone signal, any player breaking the
 * stick or a neighbouring explosion replaces it with an anchored charge entity — forty ticks of
 * fuse for redstone and breaking, five for explosion chains.
 */
public class DynamiteBlock extends Block {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    public static final BooleanProperty LINKED = BooleanProperty.create("linked");

    public static final int FUSE_TICKS = 40;
    public static final int CHAIN_FUSE_TICKS = 5;

    private static final VoxelShape FLOOR = Block.box(6.0, 0.0, 6.0, 10.0, 10.0, 10.0);
    private static final VoxelShape NORTH = Block.box(5.0, 3.0, 11.0, 11.0, 13.0, 16.0);
    private static final VoxelShape SOUTH = Block.box(5.0, 3.0, 0.0, 11.0, 13.0, 5.0);
    private static final VoxelShape WEST = Block.box(11.0, 3.0, 5.0, 16.0, 13.0, 11.0);
    private static final VoxelShape EAST = Block.box(0.0, 3.0, 5.0, 5.0, 13.0, 11.0);

    public DynamiteBlock(Properties properties) {
        super(properties);
        registerDefaultState(
                stateDefinition.any().setValue(FACING, Direction.UP).setValue(LINKED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LINKED);
    }

    @Override
    protected VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            case EAST -> EAST;
            default -> FLOOR;
        };
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        if (facing == Direction.DOWN) return false;
        BlockPos supportPos = pos.relative(facing.getOpposite());
        return level.getBlockState(supportPos).isFaceSturdy(level, supportPos, facing);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        var level = context.getLevel();
        var pos = context.getClickedPos();
        Direction clicked = context.getClickedFace();
        if (clicked != Direction.DOWN) {
            var state = defaultBlockState().setValue(FACING, clicked);
            if (state.canSurvive(level, pos)) return state;
        }
        for (Direction dir : Direction.values()) {
            if (dir == Direction.DOWN) continue;
            var state = defaultBlockState().setValue(FACING, dir);
            if (state.canSurvive(level, pos)) return state;
        }
        return null;
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            LevelReader level,
            ScheduledTickAccess ticks,
            BlockPos pos,
            Direction direction,
            BlockPos neighbourPos,
            BlockState neighbourState,
            RandomSource random) {
        // Legacy updateShape(): losing the support face pops the stick as an item.
        if (state.getValue(FACING).getOpposite() == direction && !state.canSurvive(level, pos)) {
            if (level instanceof Level realLevel && !realLevel.isClientSide()) {
                Block.popResource(realLevel, pos, new ItemStack(ModExplosives.DYNAMITE.get()));
            }
            return Blocks.AIR.defaultBlockState();
        }
        return state;
    }

    @Override
    public void setPlacedBy(
            Level level,
            BlockPos pos,
            BlockState state,
            @Nullable LivingEntity placer,
            ItemStack stack) {
        this.checkPlacement(level, pos, state);
    }

    @Override
    protected void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block block,
            @Nullable Orientation orientation,
            boolean movedByPiston) {
        this.checkPlacement(level, pos, state);
    }

    private void checkPlacement(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide()) return;
        if (level.hasNeighborSignal(pos)) {
            prime(level, pos, null, FUSE_TICKS);
        } else if (!state.canSurvive(level, pos) && level.getBlockState(pos).is(this)) {
            // Legacy checkPlacement(): a stick that lost its support pops as an item.
            level.removeBlock(pos, false);
            Block.popResource(level, pos, new ItemStack(ModExplosives.DYNAMITE.get()));
        }
    }

    @Override
    public BlockState playerWillDestroy(
            Level level, BlockPos pos, BlockState state, Player player) {
        // Legacy onDestroyedByPlayer(): breaking a stick always primes it, creative included.
        if (!level.isClientSide()) {
            prime(level, pos, player, FUSE_TICKS);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void wasExploded(ServerLevel level, BlockPos pos, Explosion explosion) {
        prime(
                level,
                pos,
                explosion.getIndirectSourceEntity() instanceof LivingEntity living ? living : null,
                CHAIN_FUSE_TICKS);
    }

    @Override
    public boolean dropFromExplosion(Explosion explosion) {
        return false;
    }

    /**
     * Legacy explode(): replaces the stick with an anchored sticky charge carrying the given fuse.
     */
    public static boolean prime(
            Level level, BlockPos pos, @Nullable LivingEntity igniter, int fuse) {
        if (!(level instanceof ServerLevel serverLevel)) return false;
        level.removeBlock(pos, false);
        DynamiteEntity charge =
                DynamiteEntity.at(
                        level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, true);
        charge.setOwner(igniter);
        charge.setFuse(fuse);
        serverLevel.addFreshEntity(charge);
        serverLevel.playSound(
                null,
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5,
                SoundEvents.TNT_PRIMED,
                SoundSource.BLOCKS,
                1.0F,
                1.0F);
        return true;
    }

    /** Legacy detonate(): remote and redstone triggering use the normal forty-tick fuse. */
    public static void detonate(Level level, BlockPos pos, @Nullable LivingEntity igniter) {
        if (!level.isClientSide()) {
            prime(level, pos, igniter, FUSE_TICKS);
        }
    }
}
