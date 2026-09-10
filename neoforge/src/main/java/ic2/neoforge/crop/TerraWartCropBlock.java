package ic2.neoforge.crop;

import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

import org.jspecify.annotations.Nullable;

/** The terra_wart_crop block (legacy TERRA_WART_CROP): ages zero through 2. */
public class TerraWartCropBlock extends CropBlock {
    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 2);

    public TerraWartCropBlock(BlockBehaviour.Properties properties) {
        super(AGE, properties);
    }

    @Override
    @Nullable
    protected IntegerProperty property() {
        return AGE;
    }
}
