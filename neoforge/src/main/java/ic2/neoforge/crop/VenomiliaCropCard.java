package ic2.neoforge.crop;

import ic2.core.crop.CropProperties;
import ic2.neoforge.registration.MaterialDefinition;
import ic2.neoforge.registration.ModItems;
import ic2.neoforge.registration.ModCrops;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

import java.util.List;

/**
 * The venomilia crop (legacy CropVenomilia): a purple tulip whose full bloom (age four) poisons
 * whatever touches it and drops grin powder, and which weeds like a crop at high growth stats.
 */
public class VenomiliaCropCard implements CropCard {
    @Override
    public String getId() {
        return "venomilia";
    }

    @Override
    public Block getCropBlock() {
        return ModCrops.VENOMILIA_CROP.get();
    }

    @Override
    public CropProperties getProperties() {
        return new CropProperties(3, 3, 1, 3, 3, 3);
    }

    @Override
    public int getMaxAge() {
        return 5;
    }

    @Override
    public boolean canGrow(CropBlockEntity crop) {
        return crop.getCurrentAge() <= 3 && crop.getLightLevel() >= 12
                || crop.getCurrentAge() == 4;
    }

    @Override
    public boolean canBeHarvested(CropBlockEntity crop) {
        return crop.getCurrentAge() >= 3;
    }

    @Override
    public int getOptimalHarvestAge() {
        return 3;
    }

    @Override
    public List<ItemStack> getGains(CropBlockEntity crop) {
        if (crop.getCurrentAge() == 4) {
            return List.of(
                    new ItemStack(ModItems.MATERIALS.get(MaterialDefinition.GRIN_POWDER).get()));
        }
        if (crop.getCurrentAge() >= 3) {
            return List.of(new ItemStack(Items.PURPLE_DYE));
        }
        return List.of();
    }

    @Override
    public int getAgeAfterHarvest(CropBlockEntity crop) {
        return 2;
    }

    @Override
    public int getGrowthDuration(CropBlockEntity crop) {
        return crop.getCurrentAge() >= 2 ? 600 : 400;
    }

    @Override
    public boolean isWeed(CropBlockEntity crop) {
        return crop.getCurrentAge() == 4 && crop.getStatGrowth() >= 8;
    }

    @Override
    public boolean onRightClick(CropBlockEntity crop, Player player) {
        if (!player.isShiftKeyDown()) this.onEntityCollision(crop, player);
        return crop.performManualHarvest();
    }

    @Override
    public boolean onLeftClick(CropBlockEntity crop, Player player) {
        if (!player.isShiftKeyDown()) this.onEntityCollision(crop, player);
        return crop.pick();
    }

    @Override
    public boolean onEntityCollision(CropBlockEntity crop, net.minecraft.world.entity.Entity entity) {
        if (crop.getCurrentAge() == 4 && entity instanceof LivingEntity living) {
            var random = crop.getLevel() != null
                    ? crop.getLevel().getRandom()
                    : net.minecraft.util.RandomSource.create();
            if (entity instanceof Player && entity.isShiftKeyDown() && random.nextInt(50) != 0) {
                return CropCard.super.onEntityCollision(crop, entity);
            }
            living.addEffect(new MobEffectInstance(MobEffects.POISON, (random.nextInt(10) + 5) * 20, 0));
            crop.setCurrentAge(3);
        }
        return CropCard.super.onEntityCollision(crop, entity);
    }

    @Override
    public String[] getAttributes() {
        return new String[] { "Purple", "Flower", "Tulip", "Poison" };
    }
}
