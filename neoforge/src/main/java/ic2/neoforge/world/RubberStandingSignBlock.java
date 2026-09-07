package ic2.neoforge.world;

import com.mojang.serialization.MapCodec;

import ic2.neoforge.registration.ModRubberBuilding;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import org.jspecify.annotations.Nullable;

public final class RubberStandingSignBlock extends StandingSignBlock {
    public static final MapCodec<StandingSignBlock> CODEC =
            simpleCodec(RubberStandingSignBlock::new);

    public RubberStandingSignBlock(Properties properties) {
        super(ModRubberBuilding.WOOD, properties);
    }

    @Override
    public MapCodec<StandingSignBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RubberSignBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModRubberBuilding.SIGN_ENTITY.get(), SignBlockEntity::tick);
    }
}
