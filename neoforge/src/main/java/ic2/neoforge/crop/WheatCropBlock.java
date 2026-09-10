package ic2.neoforge.crop;

import net.minecraft.world.level.block.state.BlockBehaviour;
import org.jspecify.annotations.Nullable;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/** The wheat crop block (legacy WHEAT_CROP): ages zero through seven. */
public class WheatCropBlock extends CropBlock {
    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 7);

    public WheatCropBlock(BlockBehaviour.Properties properties) {
        super(AGE, properties);
    }

    @Override
    @Nullable
    protected IntegerProperty property() {
        return AGE;
    }
}
