package ic2.neoforge.crop;

import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

import org.jspecify.annotations.Nullable;

/** The bobs_yer_uncle_ranks_berries_crop block (legacy BOBS_YER_UNCLE_RANKS_BERRIES_CROP): ages zero through 3. */
public class BobsYerUncleRanksBerriesCropBlock extends CropBlock {
    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 3);

    public BobsYerUncleRanksBerriesCropBlock(BlockBehaviour.Properties properties) {
        super(AGE, properties);
    }

    @Override
    @Nullable
    protected IntegerProperty property() {
        return AGE;
    }
}
