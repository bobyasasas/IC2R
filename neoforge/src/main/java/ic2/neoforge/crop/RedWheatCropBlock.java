package ic2.neoforge.crop;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

import org.jspecify.annotations.Nullable;

/**
 * The red_wheat_crop block (legacy REDWHEAT_CROP): ages zero through 6, and a ripe stalk emits
 * light seven and a full redstone signal (legacy getEmittedLight/getEmittedRedstoneSignal).
 */
public class RedWheatCropBlock extends CropBlock {
    public static final int MAX_AGE = 6;
    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, MAX_AGE);

    public RedWheatCropBlock(BlockBehaviour.Properties properties) {
        super(AGE, properties);
    }

    @Override
    @Nullable
    protected IntegerProperty property() {
        return AGE;
    }

    private static boolean ripe(BlockState state) {
        return state.getValue(AGE) == MAX_AGE;
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        return ripe(state) ? 15 : 0;
    }
}
