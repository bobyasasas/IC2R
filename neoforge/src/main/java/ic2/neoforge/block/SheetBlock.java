package ic2.neoforge.block;

import ic2.neoforge.registration.ModMaterialBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * Legacy Ic2SheetBlock 1:1: one class serves the resin/rubber/wool sheets and tells them apart
 * by block identity because none of the three carries blockstate properties.
 *
 * Divergence: the legacy jump-key bounce branch reads IC2.keyboard.isJumpKeyDown, which needs the
 * client key-sync network layer the port has not built (same downgrade as the jetpack); a jumping
 * player therefore takes the default {@code -0.8} multiplier instead of {@code -1.3}.
 */
public class SheetBlock extends Block {
    private static final VoxelShape AABB = Shapes.box(0.0, 0.0, 0.0, 1.0, 0.125, 1.0);
    // Legacy positiveHorizontalFacings: the weight scan only walks the east/west and south/north axes.
    private static final Direction[] POSITIVE_HORIZONTAL_FACINGS =
            new Direction[] {Direction.EAST, Direction.SOUTH};

    public SheetBlock(Properties properties) {
        super(properties);
    }

    // 1:1 port of the legacy scan, including its control-flow quirks: a failed direction breaks
    // straight to the next axis, and `if (dir == 1) return true` only fires after the +1 direction
    // finished, so support means anchors on BOTH sides of one axis within 16 steps through a
    // descending rubber-sheet staircase.
    private boolean canSupportWeight(Level level, BlockPos pos) {
        int maxRange = 16;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (Direction axis : POSITIVE_HORIZONTAL_FACINGS) {
            for (int dir = -1; dir <= 1; dir += 2) {
                cursor.set(pos);
                boolean supported = false;

                for (int i = 0; i < maxRange; i++) {
                    cursor.move(axis, dir);
                    BlockState state = level.getBlockState(cursor);
                    if (state.isCollisionShapeFullBlock(level, cursor)) {
                        supported = true;
                        break;
                    }

                    if (state.getBlock() != ModMaterialBlocks.RUBBER_SHEET.get()) {
                        break;
                    }

                    cursor.move(Direction.DOWN);
                    BlockState baseState = level.getBlockState(cursor);
                    if (baseState.isCollisionShapeFullBlock(level, cursor)) {
                        supported = true;
                        break;
                    }

                    cursor.move(Direction.UP);
                }

                if (!supported) {
                    break;
                }

                if (dir == 1) {
                    return true;
                }
            }
        }

        return false;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = this.defaultBlockState();
        return this.isValidPosition(level, pos, state) ? state : null;
    }

    @Override
    protected VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return AABB;
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (state.getBlock() == ModMaterialBlocks.RESIN_SHEET.get()) {
            return Shapes.empty();
        }
        // Wool lets sneaking players and low step-height entities fall through.
        if (!(state.getBlock() == ModMaterialBlocks.WOOL_SHEET.get()
                        && context instanceof EntityCollisionContext entityContext)
                || !(entityContext.getEntity() instanceof Player player)
                || !player.isShiftKeyDown()
                        && !(player.getY() < pos.getY() + 0.125 - player.maxUpStep())) {
            return AABB;
        }
        return Shapes.empty();
    }

    private boolean isValidPosition(Level level, BlockPos pos, BlockState state) {
        if (state.getBlock() == ModMaterialBlocks.RESIN_SHEET.get()) {
            return this.isCollisionShapeFullBlockBelow(level, pos);
        }

        if (state.getBlock() != ModMaterialBlocks.RUBBER_SHEET.get()) {
            return state.getBlock() == ModMaterialBlocks.WOOL_SHEET.get();
        }

        for (Direction facing : Direction.Plane.HORIZONTAL) {
            BlockState neighbor = level.getBlockState(pos.relative(facing));
            // Legacy evaluates the full-block test at the sheet's own pos, not the neighbor pos;
            // kept 1:1 (indistinguishable for full blocks, but this is a faithful port).
            if (neighbor.getBlock() == ModMaterialBlocks.RUBBER_SHEET.get()
                    || neighbor.isCollisionShapeFullBlock(level, pos)) {
                return true;
            }
        }

        return this.isCollisionShapeFullBlockBelow(level, pos);
    }

    private boolean isCollisionShapeFullBlockBelow(Level level, BlockPos pos) {
        BlockPos below = pos.below();
        BlockState state = level.getBlockState(below);
        return state.isCollisionShapeFullBlock(level, below);
    }

    @Override
    protected void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block block,
            @Nullable Orientation orientation,
            boolean movedByPiston) {
        if (!this.isValidPosition(level, pos, state)) {
            Block.dropResources(state, level, pos, null);
            level.removeBlock(pos, false);
        }
    }

    @Override
    protected void entityInside(
            BlockState state,
            Level level,
            BlockPos pos,
            Entity entity,
            InsideBlockEffectApplier effectApplier,
            boolean isPrecise) {
        if (state.getBlock() == ModMaterialBlocks.RESIN_SHEET.get()) {
            entity.fallDistance = entity.fallDistance * 0.75;
            entity.setDeltaMovement(
                    entity.getDeltaMovement().x() * 0.6,
                    entity.getDeltaMovement().y() * 0.85,
                    entity.getDeltaMovement().z() * 0.6);
        } else if (state.getBlock() == ModMaterialBlocks.RUBBER_SHEET.get()) {
            if (!level.isEmptyBlock(pos.below())) {
                return;
            }

            if (entity instanceof LivingEntity && !this.canSupportWeight(level, pos)) {
                level.levelEvent(2001, pos, Block.getId(state));
                level.removeBlock(pos, false);
                return;
            }

            if (entity.getDeltaMovement().y() <= -0.4) {
                entity.fallDistance = 0.0;
                entity.setDeltaMovement(
                        entity.getDeltaMovement().x() * 1.1,
                        entity.getDeltaMovement().y(),
                        entity.getDeltaMovement().z() * 1.1);
                if (entity instanceof LivingEntity) {
                    if (entity instanceof Player && entity.isShiftKeyDown()) {
                        entity.setDeltaMovement(
                                entity.getDeltaMovement().x(),
                                entity.getDeltaMovement().y() * -0.1,
                                entity.getDeltaMovement().z());
                    } else {
                        entity.setDeltaMovement(
                                entity.getDeltaMovement().x(),
                                entity.getDeltaMovement().y() * -0.8,
                                entity.getDeltaMovement().z());
                    }
                } else {
                    entity.setDeltaMovement(
                            entity.getDeltaMovement().x(),
                            entity.getDeltaMovement().y() * -0.8,
                            entity.getDeltaMovement().z());
                }
            }
            // Legacy keeps a wool fallDistance * 0.95 branch here, but it sits inside the rubber
            // else-if and is unreachable for wool; ported as unreachable for parity notes only.
        }
    }
}
