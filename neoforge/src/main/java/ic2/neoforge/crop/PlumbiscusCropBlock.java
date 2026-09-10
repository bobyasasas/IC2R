package ic2.neoforge.crop;

import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

import org.jspecify.annotations.Nullable;

/** The {n.lower()}_crop block (legacy {n.upper()}_CROP): ages zero through 3. */
public class PlumbiscusCropBlock extends CropBlock {
    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 3);

    public PlumbiscusCropBlock(BlockBehaviour.Properties properties) {
        super(AGE, properties);
    }

    @Override
    @Nullable
    protected IntegerProperty property() {
        return AGE;
    }
}
