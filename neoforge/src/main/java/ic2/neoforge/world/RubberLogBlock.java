package ic2.neoforge.world;

import com.mojang.serialization.MapCodec;

import ic2.neoforge.registration.MaterialDefinition;
import ic2.neoforge.registration.ModItems;
import ic2.neoforge.registration.ModWorldContent;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;

public final class RubberLogBlock extends RotatedPillarBlock {
    public static final MapCodec<RubberLogBlock> CODEC = simpleCodec(RubberLogBlock::new);
    public static final EnumProperty<ResinState> RESIN =
            EnumProperty.create("state", ResinState.class);

    public RubberLogBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(RESIN, ResinState.PLAIN));
    }

    @Override
    public MapCodec<? extends RotatedPillarBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(RESIN);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(AXIS, context.getClickedFace().getAxis());
    }

    public static boolean canRegenerate(ServerLevel level, BlockPos pos) {
        var top = pos.mutable();
        // Never request an unloaded chunk while following a player-built log column.
        while (level.isLoaded(top)
                && level.getBlockState(top).is(ModWorldContent.RUBBER_LOG.get())) {
            top.move(Direction.UP);
            if (level.isOutsideBuildHeight(top)) return false;
        }
        return level.isLoaded(top) && level.getBlockState(top).is(BlockTags.LEAVES);
    }

    @Override
    protected void randomTick(
            BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        var resin = state.getValue(RESIN);
        if (random.nextInt(7) == 0 && !resin.plain() && !resin.wet() && canRegenerate(level, pos))
            level.setBlockAndUpdate(pos, state.setValue(RESIN, resin.withWet(true)));
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return state.getValue(RESIN).plain() ? PushReaction.NORMAL : PushReaction.BLOCK;
    }

    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        return 4;
    }

    @Override
    public int getFireSpreadSpeed(
            BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        return 20;
    }

    @Override
    public BlockState getToolModifiedState(
            BlockState state, UseOnContext context, ItemAbility ability, boolean simulate) {
        if (ability != ItemAbilities.AXE_STRIP
                || !context.getItemInHand().canPerformAction(ability))
            return super.getToolModifiedState(state, context, ability, simulate);
        if (!simulate && !context.getLevel().isClientSide() && state.getValue(RESIN).wet())
            Block.popResource(
                    context.getLevel(),
                    context.getClickedPos(),
                    new ItemStack(
                            ModItems.MATERIALS.get(MaterialDefinition.RESIN).get(),
                            context.getLevel().getRandom().nextInt(2) + 1));
        return ModWorldContent.STRIPPED_RUBBER_LOG
                .get()
                .defaultBlockState()
                .setValue(AXIS, state.getValue(AXIS));
    }
}
