package ic2.neoforge.crop;

import ic2.neoforge.registration.ModCrops;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.jetbrains.annotations.Nullable;

/**
 * Legacy crop plant block: a thin stick shape with the crop age in its state. Every crop type has
 * its own block (crop_stick, wheat_crop, weed_crop, ...), all sharing the crop tile. Left click
 * picks (seed drops), use harvests a mature crop.
 */
public class CropBlock extends Block implements EntityBlock {
    @Nullable private final IntegerProperty ageProperty;

    public CropBlock(@Nullable IntegerProperty ageProperty, Properties properties) {
        super(properties);
        this.ageProperty = ageProperty;
        if (ageProperty != null) registerDefaultState(defaultBlockState().setValue(ageProperty, 0));
    }

    /**
     * The age property of this crop, or null for the empty stick. Subclasses must override this
     * against a static field: the state definition is built from the Block superclass constructor
     * before any instance field is assigned.
     */
    @Nullable
    protected IntegerProperty property() {
        return ageProperty;
    }

    @Nullable
    public IntegerProperty ageProperty() {
        return property();
    }

    @Override
    protected final void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        IntegerProperty property = property();
        if (property != null) builder.add(property);
    }

    @Override
    protected VoxelShape getShape(
            BlockState state,
            net.minecraft.world.level.BlockGetter level,
            BlockPos pos,
            CollisionContext context) {
        return net.minecraft.world.phys.shapes.Shapes.box(0.2, 0.0, 0.2, 0.8, 0.85, 0.8);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            net.minecraft.world.phys.BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof CropBlockEntity crop) {
            return crop.rightClick(player) ? InteractionResult.CONSUME : InteractionResult.PASS;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected InteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            net.minecraft.world.InteractionHand hand,
            net.minecraft.world.phys.BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof CropBlockEntity crop) {
            return crop.rightClick(player, stack)
                    ? InteractionResult.CONSUME
                    : InteractionResult.PASS;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
        if (level instanceof ServerLevel serverLevel
                && serverLevel.getBlockEntity(pos) instanceof CropBlockEntity crop) {
            var card = crop.card();
            if (card != null) card.onLeftClick(crop, player);
        }
    }

    @Override
    protected void entityInside(
            BlockState state,
            Level level,
            BlockPos pos,
            net.minecraft.world.entity.Entity entity,
            net.minecraft.world.entity.InsideBlockEffectApplier effectApplier,
            boolean isPrecise) {
        if (level instanceof ServerLevel serverLevel
                && serverLevel.getBlockEntity(pos) instanceof CropBlockEntity crop) {
            crop.onEntityCollision(entity);
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CropBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != ModCrops.CROP_ENTITY.get()) return null;
        return (world, pos, blockState, entity) -> {
            if (entity instanceof CropBlockEntity crop) crop.serverTick((ServerLevel) world);
        };
    }
}
