package ic2.neoforge.item;

import ic2.neoforge.crop.CropBlockEntity;
import ic2.neoforge.registration.MaterialDefinition;
import ic2.neoforge.registration.ModCrops;
import ic2.neoforge.registration.ModItems;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * Weeding trowel (legacy ItemWeedingTrowel): server-side first use on a weed crop drops one weed
 * per growth age plus one, then resets the tile back to a plain crop stick.
 */
public class WeedingTrowelItem extends Item {
    public WeedingTrowelItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide()) return InteractionResult.PASS;
        if (level.getBlockEntity(context.getClickedPos()) instanceof CropBlockEntity crop
                && crop.card() == ModCrops.WEED_CARD) {
            var weed = ModItems.MATERIALS.get(MaterialDefinition.WEED).get();
            Containers.dropItemStack(
                    level,
                    context.getClickedPos().getX() + 0.5,
                    context.getClickedPos().getY() + 0.5,
                    context.getClickedPos().getZ() + 0.5,
                    new ItemStack(weed, crop.getCurrentAge() + 1));
            crop.reset((ServerLevel) level);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
