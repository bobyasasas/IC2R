package ic2.neoforge.crop;

import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

import org.jspecify.annotations.Nullable;

/** The sticky_reed_crop block (legacy STICKY_REED_CROP): ages zero through 3. */
public class StickyReedCropBlock extends CropBlock {
    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 3);

    public StickyReedCropBlock(BlockBehaviour.Properties properties) {
        super(AGE, properties);
    }

    @Override
    @Nullable
    protected IntegerProperty property() {
        return AGE;
    }
}
