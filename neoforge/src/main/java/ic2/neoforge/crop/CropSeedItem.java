package ic2.neoforge.crop;

import ic2.neoforge.component.CropSeed;
import ic2.neoforge.component.ModDataComponents;
import ic2.neoforge.registration.ModCrops;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;

import java.util.function.Consumer;

/**
 * Legacy ItemCropSeed (crop_seed_bag): carries the crop id and its Gr/Ga/Re stats. Right-clicking a
 * crop stick plants the crop; from scan level 4 the tooltip reveals the stats.
 */
public class CropSeedItem extends Item {
    public CropSeedItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().getBlockEntity(context.getClickedPos())
                instanceof CropBlockEntity crop) {
            ItemStack stack = context.getItemInHand();
            CropSeed seed = stack.get(ModDataComponents.CROP_SEED.get());
            if (seed == null) return InteractionResult.PASS;
            boolean planted =
                    crop.tryPlantIn(
                            ModCrops.card(seed.cropId()),
                            0,
                            seed.growth(),
                            seed.gain(),
                            seed.resistance(),
                            seed.scan());
            if (!planted) return InteractionResult.PASS;
            Player player = context.getPlayer();
            if (player == null || !player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> tooltip,
            TooltipFlag flag) {
        CropSeed seed = stack.get(ModDataComponents.CROP_SEED.get());
        if (seed != null && seed.scan() >= 4) {
            tooltip.accept(Component.literal("Gr " + seed.growth()));
            tooltip.accept(Component.literal("Ga " + seed.gain()));
            tooltip.accept(Component.literal("Re " + seed.resistance()));
        }
    }
}
