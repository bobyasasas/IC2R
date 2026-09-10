package ic2.neoforge.crop;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Legacy ItemCrop: the crop stick item, placeable only on crop soil (legacy CropSoilType). */
public class CropStickItem extends BlockItem {
    public CropStickItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    protected boolean canPlace(BlockPlaceContext context, BlockState state) {
        Block below = context.getLevel().getBlockState(context.getClickedPos().below()).getBlock();
        return CropSoilType.contains(below) && super.canPlace(context, state);
    }
}
