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
        growUntil(helper, 7, 2100);
    }

    /**
     * Refreshes the terrain qualities before every tick: age changes swap the tile and a fresh
     * tile starts with unset terrain, which legacy compensates on its staggered refresh cycle.
     */
    private static void growUntil(GameTestHelper helper, int targetAge, int maxTicks) {
        for (int tick = 0; tick < maxTicks && crop(helper).getCurrentAge() < targetAge; tick++) {
            CropBlockEntity crop = crop(helper);
            crop.refreshTerrain(helper.getLevel());
            crop.performTick(1024L);
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

    static void cropBaseSeedPlanting(GameTestHelper helper) {
        helper.setBlock(POSITION, ModCrops.CROP_STICK.get().defaultBlockState());
        CropBlockEntity crop = crop(helper);
        ItemStack cane = new ItemStack(Items.SUGAR_CANE, 8);
        helper.assertTrue(crop.rightClick(null, cane), "Sugar cane plants a reed crop");
        helper.assertTrue(
                helper.getBlockState(POSITION).is(ModCrops.REED_CROP.get()),
                "The reed crop block replaces the stick");
        helper.assertTrue(
                cane.getCount() == 8, "A zero-size base seed consumes nothing (legacy quirk)");
        helper.assertTrue(
                !crop(helper).rightClick(null, new ItemStack(Items.DIRT)),
                "Produce without a base seed registration never plants");
        helper.setBlock(POSITION, ModCrops.CROP_STICK.get().defaultBlockState());
        // Cocoa demands stored nutrients even for planting (legacy: fertilise the stick first).
        helper.assertTrue(
                !crop(helper).rightClick(null, new ItemStack(Items.COCOA_BEANS)),
                "Cocoa refuses to plant on a nutrient-free stick");
        helper.assertTrue(
                helper.getBlockState(POSITION).is(ModCrops.CROP_STICK.get()),
                "A refused planting keeps the empty stick");
        helper.succeed();
    }

    static void cropReedAgeScaledGains(GameTestHelper helper) {
        helper.setBlock(POSITION, ModCrops.CROP_STICK.get().defaultBlockState());
        CropBlockEntity crop = crop(helper);
        helper.assertTrue(
                crop.rightClick(null, new ItemStack(Items.SUGAR_CANE)),
                "Sugar cane plants a reed crop");
        boolean anyCane = false;
        for (int cycle = 0; cycle < 6; cycle++) {
            growUntil(helper, 2, 900);
            if (crop(helper).performManualHarvest()) {
                anyCane |= countItems(helper, Items.SUGAR_CANE) > 0;
            }
        }
        helper.assertTrue(anyCane, "Harvesting a reed drops cane");
        helper.succeed();
    }

    static void cropCoffeeHarvestWindow(GameTestHelper helper) {
        helper.setBlock(POSITION.below(), Blocks.FARMLAND);
        helper.setBlock(POSITION.above().above(), Blocks.GLOWSTONE);
        helper.setBlock(POSITION, ModCrops.CROP_STICK.get().defaultBlockState());
        CropBlockEntity crop = crop(helper);
        helper.assertTrue(
                crop.rightClick(
                        null,
                        new ItemStack(
                                ic2.neoforge.registration.ModItems.MATERIALS
                                        .get(
                                                ic2.neoforge.registration.MaterialDefinition
                                                        .COFFEE_BEANS)
                                        .get())),
                "Coffee beans plant a coffee crop");
        // Age three opens the harvest window but yields no beans (legacy getGain null branch).
        growUntil(helper, 3, 4000);
        helper.assertTrue(
                crop(helper).getCurrentAge() == 3,
                "The coffee stops at the window age, saw " + crop(helper).getCurrentAge());
        boolean anyBeans = false;
        String lastSeen = "";
        for (int cycle = 0; cycle < 8 && !anyBeans; cycle++) {
            growUntil(helper, 4, 6000);
            lastSeen = "age " + crop(helper).getCurrentAge();
            boolean harvested = crop(helper).performManualHarvest();
            lastSeen += " harvested " + harvested;
            if (harvested) {
                anyBeans |=
                        countItems(
                                        helper,
                                        ic2.neoforge.registration.ModItems.MATERIALS
                                                .get(
                                                        ic2.neoforge.registration.MaterialDefinition
                                                                .COFFEE_BEANS)
                                                .get())
                                > 0;
            }
        }
        helper.assertTrue(anyBeans, "Ripe coffee drops coffee beans, last " + lastSeen);
        helper.succeed();
    }

    static void cropCocoaNutrientGate(GameTestHelper helper) {
        helper.setBlock(POSITION, ModCrops.CROP_STICK.get().defaultBlockState());
        CropBlockEntity crop = crop(helper);
        // Planting itself sits behind the nutrient gate (legacy fertilise-first flow).
        crop.setStorageNutrients(3);
        helper.assertTrue(
                crop.rightClick(null, new ItemStack(Items.COCOA_BEANS)),
                "Cocoa beans plant a fertilised stick");
        for (int tick = 0; tick < 120; tick++) crop(helper).performTick(1024L);
        helper.assertTrue(
                crop(helper).getCurrentAge() == 0,
                "Cocoa refuses to grow without stored nutrients, saw "
                        + crop(helper).getCurrentAge());
        crop(helper).setStorageNutrients(1000);
        growUntil(helper, 1, 400);
        helper.assertTrue(
                crop(helper).getCurrentAge() >= 1,
                "Stored nutrients unlock cocoa growth, saw " + crop(helper).getCurrentAge());
        helper.succeed();
    }

    static void cropNetherWartSoulSand(GameTestHelper helper) {
        helper.setBlock(POSITION, ModCrops.CROP_STICK.get().defaultBlockState());
        CropBlockEntity crop = crop(helper);
        helper.assertTrue(
                crop.rightClick(null, new ItemStack(Items.NETHER_WART)),
                "Nether wart plants a nether wart crop");
        crop(helper).refreshTerrain(helper.getLevel());
        for (int tick = 0; tick < 5; tick++) crop(helper).performTick(1024L);
        int plain = crop(helper).getGrowthPoints();
        helper.assertTrue(
                plain < 500, "Without soul sand growth stays slow, saw " + plain);
        helper.setBlock(POSITION.below(), Blocks.SOUL_SAND);
        for (int tick = 0; tick < 5; tick++) crop(helper).performTick(1024L);
        int boosted = crop(helper).getGrowthPoints() - plain;
        helper.assertTrue(
                boosted >= 500,
                "Soul sand grants +100 growth points per tick, saw " + boosted);
        for (int tick = 0; tick < 400 && crop(helper).getCurrentAge() < 2; tick++) {
            crop(helper).performTick(1024L);
        }
        helper.assertTrue(
                crop(helper).getCurrentAge() == 2,
                "The nether wart reaches age two, saw " + crop(helper).getCurrentAge());
        helper.succeed();
    }

    static void cropPotatoHarvestBand(GameTestHelper helper) {
        helper.setBlock(POSITION.below(), Blocks.FARMLAND);
        helper.setBlock(POSITION.above().above(), Blocks.GLOWSTONE);
        helper.setBlock(POSITION, ModCrops.CROP_STICK.get().defaultBlockState());
        CropBlockEntity crop = crop(helper);
        helper.assertTrue(
                crop.rightClick(null, new ItemStack(Items.POTATO)),
                "A potato plants a potato crop");
        crop(helper).refreshTerrain(helper.getLevel());
        growUntil(helper, 2, 4000);
        helper.assertTrue(
                crop(helper).getCurrentAge() == 2,
                "The potato opens its harvest band at age two, saw "
                        + crop(helper).getCurrentAge());
        helper.assertTrue(
                crop(helper).performManualHarvest(), "The half-ripe potato harvests");
        growUntil(helper, 3, 4000);
        helper.assertTrue(
                crop(helper).getCurrentAge() == 3,
                "The potato reaches full ripeness, saw " + crop(helper).getCurrentAge());
        helper.assertTrue(crop(helper).performManualHarvest(), "The ripe potato harvests");
        // Poisonous potatoes are a 5% full-ripe roll; plain potatoes dominate across cycles.
        boolean anyPotato = false;
        for (int cycle = 0; cycle < 6 && !anyPotato; cycle++) {
            growUntil(helper, 3, 4000);
            if (crop(helper).performManualHarvest()) {
                anyPotato |= countItems(helper, Items.POTATO) > 0;
            }
        }
        helper.assertTrue(anyPotato, "Harvesting potatoes drops potato produce");
        helper.succeed();
    }

    static void cropMushroomBaseSeed(GameTestHelper helper) {
        helper.setBlock(POSITION, ModCrops.CROP_STICK.get().defaultBlockState());
        CropBlockEntity crop = crop(helper);
        ItemStack mushrooms = new ItemStack(Items.BROWN_MUSHROOM, 4);
        helper.assertTrue(
                crop.rightClick(null, mushrooms), "Brown mushrooms plant a mushroom crop");
        helper.assertTrue(
                helper.getBlockState(POSITION).is(ModCrops.BROWN_MUSHROOM_CROP.get()),
                "The mushroom crop block replaces the stick");
        helper.assertTrue(
                mushrooms.getCount() == 4,
                "A zero-size base seed consumes nothing (legacy quirk)");
        crop(helper).refreshTerrain(helper.getLevel());
        growUntil(helper, 2, 2000);
        helper.assertTrue(
                crop(helper).getCurrentAge() == 2,
                "The mushroom reaches full ripeness, saw " + crop(helper).getCurrentAge());
        boolean anyMushroom = false;
        for (int cycle = 0; cycle < 6 && !anyMushroom; cycle++) {
            growUntil(helper, 2, 2000);
            if (crop(helper).performManualHarvest()) {
                anyMushroom |= countItems(helper, Items.BROWN_MUSHROOM) > 0;
            }
        }
        helper.assertTrue(anyMushroom, "Harvesting mushrooms returns brown mushrooms");
        helper.succeed();
    }

    static void cropSaplingGains(GameTestHelper helper) {
        helper.setBlock(POSITION.below(), Blocks.FARMLAND);
        helper.setBlock(POSITION.above().above(), Blocks.GLOWSTONE);
        helper.setBlock(POSITION, ModCrops.CROP_STICK.get().defaultBlockState());
        CropBlockEntity crop = crop(helper);
        helper.assertTrue(
                crop.rightClick(null, new ItemStack(Items.OAK_SAPLING)),
                "An oak sapling plants a sapling crop");
        helper.assertTrue(
                helper.getBlockState(POSITION).is(ModCrops.OAK_SAPLING_CROP.get()),
                "The sapling crop block replaces the stick");
        crop(helper).refreshTerrain(helper.getLevel());
        growUntil(helper, 4, 12000);
        helper.assertTrue(
                crop(helper).getCurrentAge() == 4,
                "The sapling reaches full ripeness, saw " + crop(helper).getCurrentAge());
        helper.assertTrue(crop(helper).performManualHarvest(), "The ripe sapling harvests");
        helper.assertTrue(
                crop(helper).getCurrentAge() == 3,
                "Harvest leaves the sapling one step below full ripeness, saw "
                        + crop(helper).getCurrentAge());
        boolean anyLog = false;
        for (int cycle = 0; cycle < 8 && !anyLog; cycle++) {
            growUntil(helper, 4, 12000);
            if (crop(helper).performManualHarvest()) {
                anyLog |= countItems(helper, Items.OAK_LOG) > 0;
            }
        }
        helper.assertTrue(anyLog, "Harvesting saplings drops logs");
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
