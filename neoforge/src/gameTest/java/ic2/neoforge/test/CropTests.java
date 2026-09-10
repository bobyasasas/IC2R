package ic2.neoforge.test;

import ic2.neoforge.component.CropSeed;
import ic2.neoforge.component.ModDataComponents;
import ic2.neoforge.crop.CropBlockEntity;
import ic2.neoforge.registration.ModCrops;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

final class CropTests {
    private static final BlockPos POSITION = new BlockPos(8, 8, 8);

    static void cropSeedBagRoundtrip(GameTestHelper helper) {
        ItemStack bag = new ItemStack(ModCrops.CROP_SEED_BAG.get());
        bag.set(ModDataComponents.CROP_SEED.get(), new CropSeed("wheat", 3, 2, 5, 4));
        CropSeed seed = bag.get(ModDataComponents.CROP_SEED.get());
        helper.assertTrue(seed.cropId().equals("wheat"), "The seed bag carries its crop id");
        helper.assertTrue(
                seed.growth() == 3
                        && seed.gain() == 2
                        && seed.resistance() == 5
                        && seed.scan() == 4,
                "The seed bag carries its stats");
        helper.assertTrue(
                ModCrops.card("wheat") == ModCrops.WHEAT_CARD
                        && ModCrops.card("weed") == ModCrops.WEED_CARD,
                "The wheat and weed cards resolve by id");
        helper.assertTrue(ModCrops.card("nope") == null, "Unknown crop ids resolve to null");
        helper.succeed();
    }

    static void cropPlantGrowHarvest(GameTestHelper helper) {
        helper.setBlock(POSITION.below(), Blocks.FARMLAND);
        helper.setBlock(POSITION.above().above(), Blocks.GLOWSTONE);
        helper.setBlock(POSITION, ModCrops.CROP_STICK.get().defaultBlockState());
        CropBlockEntity crop = crop(helper);
        helper.assertTrue(
                crop.tryPlantIn(ModCrops.WHEAT_CARD, 0, 2, 2, 2, 4),
                "Wheat plants into an empty crop stick");
        helper.assertTrue(
                helper.getBlockState(POSITION).is(ModCrops.WHEAT_CROP.get()),
                "The stick transforms into the wheat crop block");
        helper.assertTrue(crop(helper).getCurrentAge() == 0, "The wheat plants at age zero");
        for (long ticker = 0; ticker < 4; ticker++) crop(helper).performTick(ticker * 256L);
        growToMaturity(helper);
        helper.assertTrue(
                crop(helper).getCurrentAge() == 7,
                "The wheat reaches the maximum age, saw " + crop(helper).getCurrentAge());
        // Re-grow and harvest several times; a single gaussian harvest can legitimately roll
        // zero produce, so the assertions accumulate over the cycles.
        boolean anyHarvest = false;
        boolean anyWheat = false;
        for (int cycle = 0; cycle < 6; cycle++) {
            growToMaturity(helper);
            if (crop(helper).performManualHarvest()) {
                anyHarvest = true;
                helper.assertTrue(
                        crop(helper).getCurrentAge() == 2,
                        "Harvest resets the age to the post-harvest age, saw "
                                + crop(helper).getCurrentAge());
                anyWheat |= countItems(helper, Items.WHEAT) > 0;
            }
        }
        helper.assertTrue(anyHarvest, "Mature crops harvest on use");
        helper.assertTrue(anyWheat, "Harvesting drops wheat produce");
        // Picking the plant drops stat-carrying seeds and restores the stick.
        growToMaturity(helper);
        helper.assertTrue(crop(helper).pick(), "Picking the crop consumes the plant");
        helper.assertTrue(
                countItems(helper, ModCrops.CROP_SEED_BAG.get()) > 0,
                "Picking drops the stat-carrying seed bag");
        helper.assertTrue(
                helper.getBlockState(POSITION).is(ModCrops.CROP_STICK.get()),
                "The pick resets the plant to an empty crop stick");
        helper.succeed();
    }

    /** Age changes swap the block and thus the tile; always re-fetch from the world. */
    private static CropBlockEntity crop(GameTestHelper helper) {
        return helper.getBlockEntity(POSITION, CropBlockEntity.class);
    }

    private static void growToMaturity(GameTestHelper helper) {
        for (int tick = 0; tick < 2100 && crop(helper).getCurrentAge() < 7; tick++) {
            crop(helper).performTick(1024L);
        }
    }

    static void cropWeedGrowth(GameTestHelper helper) {
        helper.setBlock(POSITION, ModCrops.CROP_STICK.get().defaultBlockState());
        CropBlockEntity crop = crop(helper);
        helper.assertTrue(
                !crop.tryPlantIn(ModCrops.WEED_CARD, 0, 1, 1, 1, 0),
                "Weeds cannot be planted from seeds like real crops");
        CropBlockEntity weedCrop = crop.transformToWeed(helper.getLevel());
        helper.assertTrue(weedCrop != null, "Weeds transform an empty stick");
        weedCrop.performTick(0L);
        for (int tick = 0; tick < 130; tick++) weedCrop.performTick(1024L);
        helper.assertTrue(
                weedCrop.getCurrentAge() >= 1,
                "The weed grows, saw age " + weedCrop.getCurrentAge());
        helper.assertTrue(
                ModCrops.cardFor(helper.getBlockState(POSITION).getBlock()).isWeed(weedCrop),
                "A grown weed counts as a weed");
        helper.succeed();
    }

    private static int countItems(GameTestHelper helper, Item item) {
        int count = 0;
        for (var entity : helper.getEntities(EntityType.ITEM)) {
            if (entity.getItem().is(item)) count += entity.getItem().getCount();
        }
        return count;
    }

    private CropTests() {}
}
