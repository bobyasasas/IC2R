package ic2.neoforge.test;

import ic2.neoforge.component.CropSeed;
import ic2.neoforge.component.ModDataComponents;
import ic2.neoforge.crop.CropBlock;
import ic2.neoforge.crop.CropBlockEntity;
import ic2.neoforge.crop.WheatCropBlock;
import ic2.neoforge.registration.MaterialDefinition;
import ic2.neoforge.registration.ModCrops;
import ic2.neoforge.registration.ModItems;
import ic2.neoforge.registration.ModMaterialBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

    static void cropFlowerDyeHarvest(GameTestHelper helper) {
        helper.setBlock(POSITION.below(), Blocks.FARMLAND);
        helper.setBlock(POSITION.above().above(), Blocks.GLOWSTONE);
        helper.setBlock(POSITION, ModCrops.CROP_STICK.get().defaultBlockState());
        CropBlockEntity crop = crop(helper);
        ItemStack poppies = new ItemStack(Items.POPPY, 4);
        helper.assertTrue(crop.rightClick(null, poppies), "Poppies plant a poppy crop");
        helper.assertTrue(
                helper.getBlockState(POSITION).is(ModCrops.POPPY_CROP.get()),
                "The poppy crop block replaces the stick");
        helper.assertTrue(
                poppies.getCount() == 1,
                "Planting a flower consumes three of four poppies (legacy size three), saw "
                        + poppies.getCount());
        helper.assertTrue(
                crop(helper).getCurrentAge() == 3,
                "Flowers plant at their bloom age, saw " + crop(helper).getCurrentAge());
        helper.assertTrue(crop(helper).performManualHarvest(), "The planted bloom harvests");
        helper.assertTrue(
                crop(helper).getCurrentAge() == 2,
                "Harvest resets the flower one age below the bloom, saw "
                        + crop(helper).getCurrentAge());
        boolean anyDye = false;
        for (int cycle = 0; cycle < 6 && !anyDye; cycle++) {
            growUntil(helper, 3, 4000);
            if (crop(helper).performManualHarvest()) {
                anyDye |= countItems(helper, Items.RED_DYE) > 0;
            }
        }
        helper.assertTrue(anyDye, "Harvesting a bloom drops red dye");
        helper.succeed();
    }

    static void cropPumpkinStem(GameTestHelper helper) {
        helper.setBlock(POSITION.below(), Blocks.FARMLAND);
        helper.setBlock(POSITION.above().above(), Blocks.GLOWSTONE);
        helper.setBlock(POSITION, ModCrops.CROP_STICK.get().defaultBlockState());
        CropBlockEntity crop = crop(helper);
        helper.assertTrue(
                crop.rightClick(null, new ItemStack(Items.PUMPKIN_SEEDS)),
                "Pumpkin seeds plant a pumpkin stem");
        helper.assertTrue(
                helper.getBlockState(POSITION).is(ModCrops.PUMPKIN_CROP.get()),
                "The pumpkin crop block replaces the stick");
        crop(helper).refreshTerrain(helper.getLevel());
        boolean anyPumpkin = false;
        for (int cycle = 0; cycle < 6 && !anyPumpkin; cycle++) {
            growUntil(helper, 3, 4000);
            if (crop(helper).performManualHarvest()) {
                anyPumpkin |= countItems(helper, Items.PUMPKIN) > 0;
            }
        }
        helper.assertTrue(anyPumpkin, "Harvesting the stem drops pumpkins");
        helper.assertTrue(
                crop(helper).getCurrentAge() == 2,
                "Harvest regrows the stem one age below full, saw "
                        + crop(helper).getCurrentAge());
        helper.succeed();
    }

    static void cropMelonStem(GameTestHelper helper) {
        helper.setBlock(POSITION.below(), Blocks.FARMLAND);
        helper.setBlock(POSITION.above().above(), Blocks.GLOWSTONE);
        helper.setBlock(POSITION, ModCrops.CROP_STICK.get().defaultBlockState());
        CropBlockEntity crop = crop(helper);
        helper.assertTrue(
                crop.rightClick(null, new ItemStack(Items.MELON_SEEDS)),
                "Melon seeds plant a melon stem");
        helper.assertTrue(
                helper.getBlockState(POSITION).is(ModCrops.MELON_CROP.get()),
                "The melon crop block replaces the stick");
        crop(helper).refreshTerrain(helper.getLevel());
        boolean anyMelon = false;
        for (int cycle = 0; cycle < 6 && !anyMelon; cycle++) {
            growUntil(helper, 3, 5000);
            if (crop(helper).performManualHarvest()) {
                anyMelon |= countItems(helper, Items.MELON) > 0;
                anyMelon |= countItems(helper, Items.MELON_SLICE) > 0;
            }
        }
        helper.assertTrue(anyMelon, "Harvesting the stem drops a melon or slices");
        helper.succeed();
    }

    static void cropVenomiliaPoison(GameTestHelper helper) {
        helper.setBlock(POSITION.below(), Blocks.FARMLAND);
        helper.setBlock(POSITION.above().above(), Blocks.GLOWSTONE);
        helper.setBlock(POSITION, ModCrops.CROP_STICK.get().defaultBlockState());
        CropBlockEntity crop = crop(helper);
        helper.assertTrue(
                crop.tryPlantIn(ModCrops.VENOMILIA_CARD, 4, 1, 1, 1, 0),
                "Venomilia plants directly at its bloom age (hybrid crop, no base seed)");
        helper.assertTrue(
                crop(helper).getCurrentAge() == 4,
                "The venomilia blooms, saw " + crop(helper).getCurrentAge());
        var grinPowder =
                ic2.neoforge.registration.ModItems.MATERIALS
                        .get(ic2.neoforge.registration.MaterialDefinition.GRIN_POWDER)
                        .get();
        boolean anyGrin = false;
        for (int cycle = 0; cycle < 6 && !anyGrin; cycle++) {
            growUntil(helper, 4, 6000);
            if (crop(helper).getCurrentAge() == 4 && crop(helper).performManualHarvest()) {
                anyGrin |= countItems(helper, grinPowder) > 0;
            }
        }
        helper.assertTrue(anyGrin, "Harvesting the full bloom drops grin powder");
        // Bring the bloom back for the collision check.
        growUntil(helper, 4, 6000);
        helper.assertTrue(
                crop(helper).getCurrentAge() == 4,
                "The venomilia blooms again, saw " + crop(helper).getCurrentAge());
        var pig = helper.spawn(EntityType.PIG, POSITION);
        helper.startSequence()
                .thenExecuteAfter(
                        5,
                        () -> {
                            helper.assertTrue(
                                    pig.hasEffect(net.minecraft.world.effect.MobEffects.POISON),
                                    "The full bloom poisons a colliding animal");
                            helper.assertTrue(
                                    crop(helper).getCurrentAge() == 3,
                                    "Poisoning regresses the bloom, saw "
                                            + crop(helper).getCurrentAge());
                        })
                .thenSucceed();
    }

    static void cropStickyReedResin(GameTestHelper helper) {
        helper.setBlock(POSITION.below(), Blocks.FARMLAND);
        helper.setBlock(POSITION.above().above(), Blocks.GLOWSTONE);
        helper.setBlock(POSITION, ModCrops.CROP_STICK.get().defaultBlockState());
        CropBlockEntity crop = crop(helper);
        helper.assertTrue(
                crop.tryPlantIn(ModCrops.STICKY_REED_CARD, 0, 1, 1, 1, 0),
                "The sticky reed plants on a stick (hybrid crop, no base seed)");
        growUntil(helper, 1, 2000);
        // A young harvest yields cane scaled by age, but the gaussian can legitimately roll zero.
        boolean anyCane = false;
        for (int cycle = 0; cycle < 6 && !anyCane; cycle++) {
            growUntil(helper, 1, 2000);
            if (crop(helper).performManualHarvest()) {
                anyCane |= countItems(helper, Items.SUGAR_CANE) >= 1;
            }
        }
        helper.assertTrue(anyCane, "A young sticky reed yields cane");
        var resin =
                ic2.neoforge.registration.ModItems.MATERIALS
                        .get(ic2.neoforge.registration.MaterialDefinition.RESIN)
                        .get();
        boolean anyResin = false;
        for (int cycle = 0; cycle < 6 && !anyResin; cycle++) {
            growUntil(helper, 3, 4000);
            if (crop(helper).performManualHarvest()) {
                anyResin |= countItems(helper, resin) > 0;
            }
        }
        helper.assertTrue(anyResin, "The full sticky reed oozes resin");
        helper.succeed();
    }

    static void cropTerraWartSnow(GameTestHelper helper) {
        helper.setBlock(POSITION, ModCrops.CROP_STICK.get().defaultBlockState());
        CropBlockEntity crop = crop(helper);
        helper.assertTrue(
                crop.rightClick(null, new ItemStack(ModCrops.TERRA_WART.get())),
                "A terra wart plants a terra wart crop");
        helper.assertTrue(
                helper.getBlockState(POSITION).is(ModCrops.TERRA_WART_CROP.get()),
                "The terra wart crop block replaces the stick");
        crop(helper).refreshTerrain(helper.getLevel());
        for (int tick = 0; tick < 5; tick++) crop(helper).performTick(1024L);
        int plain = crop(helper).getGrowthPoints();
        helper.assertTrue(plain < 500, "Without snow growth stays slow, saw " + plain);
        helper.setBlock(POSITION.below(), Blocks.SNOW);
        for (int tick = 0; tick < 5; tick++) crop(helper).performTick(1024L);
        int boosted = crop(helper).getGrowthPoints() - plain;
        helper.assertTrue(
                boosted >= 500, "Snow grants +100 growth points per tick, saw " + boosted);
        boolean anyWart = false;
        for (int cycle = 0; cycle < 8 && !anyWart; cycle++) {
            for (int tick = 0; tick < 400 && crop(helper).getCurrentAge() < 2; tick++) {
                crop(helper).performTick(1024L);
            }
            if (crop(helper).performManualHarvest()) {
                anyWart |= countItems(helper, ModCrops.TERRA_WART.get()) > 0;
            }
        }
        helper.assertTrue(anyWart, "Harvesting drops terra warts");
        helper.succeed();
    }

    static void cropWartSnowTransmutation(GameTestHelper helper) {
        helper.setBlock(POSITION, ModCrops.CROP_STICK.get().defaultBlockState());
        CropBlockEntity crop = crop(helper);
        helper.assertTrue(
                crop.rightClick(null, new ItemStack(Items.NETHER_WART)),
                "Nether wart plants a nether wart crop");
        helper.setBlock(POSITION.below(), Blocks.SNOW);
        boolean converted = false;
        for (int roll = 0; roll < 9000 && !converted; roll++) {
            if (helper.getBlockState(POSITION).is(ModCrops.TERRA_WART_CROP.get())) {
                converted = true;
                break;
            }
            ModCrops.NETHER_WART_CARD.tick(crop(helper));
        }
        helper.assertTrue(converted, "Snow slowly transmutes the nether wart into a terra wart");
        helper.assertTrue(
                ModCrops.cardFor(helper.getBlockState(POSITION).getBlock())
                        == ModCrops.TERRA_WART_CARD,
                "The transmuted plant resolves to the terra wart card");
        helper.succeed();
    }

    static void cropTerraWartCure(GameTestHelper helper) {
        var pig = helper.spawn(EntityType.PIG, POSITION.above());
        pig.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.POISON, 200, 0));
        pig.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                ic2.neoforge.registration.ModEffects.RADIATION, 900, 0));
        ItemStack warts = new ItemStack(ModCrops.TERRA_WART.get());
        warts.finishUsingItem(helper.getLevel(), pig);
        helper.assertTrue(
                !pig.hasEffect(net.minecraft.world.effect.MobEffects.POISON),
                "A terra wart cures poison");
        var radiation = pig.getEffect(ic2.neoforge.registration.ModEffects.RADIATION);
        helper.assertTrue(
                radiation != null && radiation.getDuration() <= 600,
                "A long radiation dose shortens instead of ending, saw "
                        + (radiation == null ? "cured" : radiation.getDuration()));
        helper.succeed();
    }

    static void cropMetalOreRootGate(GameTestHelper helper) {
        helper.setBlock(POSITION, ModCrops.CROP_STICK.get().defaultBlockState());
        CropBlockEntity crop = crop(helper);
        helper.assertTrue(
                crop.tryPlantIn(ModCrops.FERRU_CARD, 0, 1, 1, 1, 0),
                "A ferru cross product plants into an empty crop stick");
        growUntil(helper, 2, 3000);
        helper.assertTrue(
                crop(helper).getCurrentAge() == 2,
                "Ferru grows freely up to age two, saw " + crop(helper).getCurrentAge());
        for (int tick = 0; tick < 300; tick++) crop(helper).performTick(1024L);
        helper.assertTrue(
                crop(helper).getCurrentAge() == 2,
                "Without metal below the final step stalls, saw " + crop(helper).getCurrentAge());
        helper.setBlock(POSITION.below(), Blocks.IRON_ORE);
        growUntil(helper, 3, 6000);
        helper.assertTrue(
                crop(helper).getCurrentAge() == 3,
                "Iron ore in the roots ripens ferru, saw " + crop(helper).getCurrentAge());
        Item ironDust = ModItems.MATERIALS.get(MaterialDefinition.SMALL_IRON_DUST).get();
        boolean anyDust = false;
        for (int cycle = 0; cycle < 16 && !anyDust; cycle++) {
            for (int tick = 0; tick < 600 && crop(helper).getCurrentAge() < 3; tick++) {
                crop(helper).performTick(1024L);
            }
            if (crop(helper).performManualHarvest()) {
                anyDust |= countItems(helper, ironDust) > 0;
                helper.assertTrue(
                        crop(helper).getCurrentAge() == 1,
                        "Metal crops reset to age one after harvest, saw "
                                + crop(helper).getCurrentAge());
            }
        }
        helper.assertTrue(anyDust, "Harvesting ferru drops small iron dust");
        helper.succeed();
    }

    static void cropShiningUncommonRoots(GameTestHelper helper) {
        helper.setBlock(POSITION, ModCrops.CROP_STICK.get().defaultBlockState());
        CropBlockEntity crop = crop(helper);
        helper.assertTrue(
                crop.tryPlantIn(ModCrops.SHINING_CARD, 0, 1, 1, 1, 0),
                "A shining cross product plants into an empty crop stick");
        helper.assertTrue(
                ModCrops.SHINING_CARD.dropGainChance() > ModCrops.FERRU_CARD.dropGainChance(),
                "The uncommon metal pair keeps the tier gain chance the common pair halves");
        helper.assertTrue(
                ModCrops.FERRU_CARD.getGrowthDuration(crop(helper)) == 800
                        && ModCrops.SHINING_CARD.getGrowthDuration(crop(helper)) == 750,
                "Early growth durations differ between the common and uncommon metal crops");
        growUntil(helper, 2, 3000);
        helper.assertTrue(
                ModCrops.SHINING_CARD.getGrowthDuration(crop(helper)) == 2200,
                "The final shining step takes 2200 ticks");
        helper.setBlock(
                POSITION.below(), ModMaterialBlocks.MATERIALS.get("silver_block").get());
        growUntil(helper, 3, 6000);
        helper.assertTrue(
                crop(helper).getCurrentAge() == 3,
                "A silver block in the roots ripens shining, saw " + crop(helper).getCurrentAge());
        helper.assertTrue(
                ModCrops.cardFor(helper.getBlockState(POSITION).getBlock())
                        == ModCrops.SHINING_CARD,
                "The shining plant resolves to the shining card");
        Item silverDust = ModItems.MATERIALS.get(MaterialDefinition.SMALL_SILVER_DUST).get();
        boolean anyDust = false;
        for (int cycle = 0; cycle < 6 && !anyDust; cycle++) {
            for (int tick = 0; tick < 600 && crop(helper).getCurrentAge() < 3; tick++) {
                crop(helper).performTick(1024L);
            }
            if (crop(helper).performManualHarvest()) {
                anyDust |= countItems(helper, silverDust) > 0;
            }
        }
        helper.assertTrue(anyDust, "Harvesting shining drops small silver dust");
        helper.succeed();
    }

    static void cropRedWheatDimLight(GameTestHelper helper) {
        helper.setBlock(POSITION.below(), Blocks.FARMLAND);
        helper.setBlock(POSITION, ModCrops.CROP_STICK.get().defaultBlockState());
        // The world room sits under open sky, so roof the crop in; a glowstone six blocks up
        // then keeps the pocket inside the 5..10 band. The light engine settles over the next
        // ticks, so every light-dependent assertion runs in a later sequence step.
        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -5; dz <= 5; dz++) {
                helper.setBlock(POSITION.offset(dx, 7, dz), Blocks.STONE);
            }
        }
        helper.setBlock(POSITION.above(6), Blocks.GLOWSTONE);
        // The light engine settles asynchronously and shares its queue with every other test
        // batch, so poll for the light state instead of assuming a fixed settle delay.
        helper.startSequence()
                .thenWaitUntil(
                        () -> {
                            int light = crop(helper).getLightLevel();
                            helper.assertTrue(
                                    light >= 5 && light <= 10,
                                    "The roofed pocket settles to red wheat light, saw " + light);
                        })
                .thenExecute(
                        () -> {
                            CropBlockEntity crop = crop(helper);
                            crop.refreshTerrain(helper.getLevel());
                            helper.assertTrue(
                                    crop.tryPlantIn(ModCrops.RED_WHEAT_CARD, 0, 1, 1, 1, 0),
                                    "Red wheat plants into an empty crop stick (light "
                                            + crop.getLightLevel() + ")");
                            helper.setBlock(POSITION.above().above(), Blocks.GLOWSTONE);
                        })
                .thenWaitUntil(
                        () ->
                                helper.assertTrue(
                                        !ModCrops.RED_WHEAT_CARD.canGrow(crop(helper)),
                                        "Light above ten blocks the red wheat"))
                .thenExecute(() -> helper.setBlock(POSITION.above().above(), Blocks.AIR))
                .thenWaitUntil(
                        () -> {
                            crop(helper).refreshTerrain(helper.getLevel());
                            helper.assertTrue(
                                    ModCrops.RED_WHEAT_CARD.canGrow(crop(helper)),
                                    "Dim light between five and ten lets the red wheat grow");
                        })
                .thenExecute(
                        () -> {
                            growUntil(helper, 6, 12000);
                            helper.assertTrue(
                                    crop(helper).getCurrentAge() == 6,
                                    "The red wheat ripens in dim light, saw "
                                            + crop(helper).getCurrentAge());
                            helper.assertTrue(
                                    helper.getLevel()
                                            .getSignal(
                                                    helper.absolutePos(POSITION),
                                                    net.minecraft.core.Direction.UP)
                                            == 15,
                                    "The ripe red wheat emits a full redstone signal");
                            helper.setBlock(POSITION.above(6), Blocks.AIR);
                        })
                .thenExecuteAfter(
                        20,
                        () -> {
                            int emission = helper.getBlockState(POSITION).getLightEmission();
                            helper.assertTrue(
                                    emission == 7,
                                    "The ripe red wheat emits light seven, saw " + emission);
                            crop(helper).performManualHarvest();
                            int cutEmission = helper.getBlockState(POSITION).getLightEmission();
                            helper.assertTrue(
                                    cutEmission == 0,
                                    "The cut stalk stops glowing, saw " + cutEmission);
                            helper.setBlock(POSITION.above(6), Blocks.GLOWSTONE);
                            boolean anyWheat = false;
                            boolean anyRedstone = false;
                            for (int cycle = 0; cycle < 24 && !(anyWheat && anyRedstone); cycle++) {
                                growUntil(helper, 6, 12000);
                                if (crop(helper).getCurrentAge() == 6
                                        && crop(helper).performManualHarvest()) {
                                    anyWheat |= countItems(helper, Items.WHEAT) > 0;
                                    anyRedstone |= countItems(helper, Items.REDSTONE) > 0;
                                }
                            }
                            helper.assertTrue(
                                    anyWheat && anyRedstone,
                                    "Unpowered harvests yield both wheat and redstone over time");
                            helper.assertTrue(
                                    crop(helper).getCurrentAge() == 1,
                                    "The red wheat resets to age one after harvest");
                            int wheatBefore = countItems(helper, Items.WHEAT);
                            helper.setBlock(POSITION.east(), Blocks.REDSTONE_BLOCK);
                            growUntil(helper, 6, 12000);
                            if (crop(helper).getCurrentAge() == 6) {
                                crop(helper).performManualHarvest();
                                helper.assertTrue(
                                        countItems(helper, Items.WHEAT) == wheatBefore,
                                        "A powered red wheat always drops redstone, not wheat");
                            }
                            helper.succeed();
                        });
    }

    static void cropEatingPlantLava(GameTestHelper helper) {
        helper.setBlock(POSITION, ModCrops.CROP_STICK.get().defaultBlockState());
        CropBlockEntity crop = crop(helper);
        helper.assertTrue(
                crop.rightClick(null, new ItemStack(Blocks.CACTUS)),
                "A cactus plants the eating plant");
        helper.assertTrue(
                helper.getBlockState(POSITION).is(ModCrops.EATING_PLANT_CROP.get()),
                "The eating plant crop replaces the stick");
        helper.setBlock(POSITION.above().above(), Blocks.GLOWSTONE);
        growUntil(helper, 2, 6000);
        helper.assertTrue(
                crop(helper).getCurrentAge() == 2,
                "The seedling grows in bright light, saw " + crop(helper).getCurrentAge());
        for (int tick = 0; tick < 300; tick++) crop(helper).performTick(1024L);
        helper.assertTrue(
                crop(helper).getCurrentAge() == 2,
                "Without lava the late growth stalls, saw " + crop(helper).getCurrentAge());
        helper.setBlock(POSITION.below(), Blocks.LAVA);
        growUntil(helper, 5, 12000);
        helper.assertTrue(
                crop(helper).getCurrentAge() == 5,
                "Lava in the roots ripens the eating plant, saw "
                        + crop(helper).getCurrentAge());
        helper.assertTrue(
                !crop(helper).performManualHarvest(),
                "The full eating plant is past its harvest age");
        helper.setBlock(
                POSITION,
                ModCrops.EATING_PLANT_CROP
                        .get()
                        .defaultBlockState()
                        .setValue(
                                ic2.neoforge.crop.EatingPlantCropBlock.AGE, 4));
        boolean anyCactus = false;
        for (int cycle = 0; cycle < 6 && !anyCactus; cycle++) {
            if (crop(helper).getCurrentAge() == 4 && crop(helper).performManualHarvest()) {
                anyCactus |= countItems(helper, Items.CACTUS) > 0;
            }
            growUntil(helper, 4, 12000);
        }
        helper.assertTrue(anyCactus, "Cutting the stalk before full age drops a cactus");
        helper.setBlock(
                POSITION,
                ModCrops.EATING_PLANT_CROP
                        .get()
                        .defaultBlockState()
                        .setValue(
                                ic2.neoforge.crop.EatingPlantCropBlock.AGE, 1));
        var pig = helper.spawn(EntityType.PIG, POSITION);
        float healthBefore = pig.getHealth();
        ModCrops.EATING_PLANT_CARD.tick(crop(helper));
        helper.assertTrue(pig.getHealth() < healthBefore, "The plant bites a nearby animal");
        helper.assertTrue(
                pig.hasEffect(net.minecraft.world.effect.MobEffects.SLOWNESS)
                        && pig.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS)
                        && pig.hasEffect(net.minecraft.world.effect.MobEffects.INVISIBILITY),
                "The bite slows and blinds its prey");
        ModCrops.EATING_PLANT_CARD.tick(crop(helper));
        helper.assertTrue(
                countItems(helper, Items.ROTTEN_FLESH) > 0,
                "The chewed meal drops rotten flesh on the next tick");
        helper.succeed();
    }

    static void cropGenericCoriumDrops(GameTestHelper helper) {
        helper.setBlock(POSITION.below(), Blocks.FARMLAND);
        helper.setBlock(POSITION, ModCrops.CROP_STICK.get().defaultBlockState());
        CropBlockEntity crop = crop(helper);
        crop.refreshTerrain(helper.getLevel());
        helper.assertTrue(
                crop.tryPlantIn(ModCrops.CORIUM_CARD, 0, 1, 1, 1, 0), "Corium plants on a stick");
        Item leather = Items.LEATHER;
        helper.assertTrue(
                ModCrops.CORIUM_CARD.getGains(crop(helper)).stream().allMatch(g -> g.is(leather)),
                "Corium gains are exactly one leather stack");
        // The harvest window opens at maxSize-1 == 3; age two is still too young. The tier-six
        // gain chance drops ~26% of harvests, so repeat until the leather shows up.
        growUntil(helper, 2, 20000);
        helper.assertTrue(
                !crop(helper).performManualHarvest(),
                "Corium below the harvest window refuses to harvest");
        boolean gotLeather = false;
        for (int cycle = 0; cycle < 16 && !gotLeather; cycle++) {
            growUntil(helper, 3, 30000);
            if (crop(helper).getCurrentAge() == 3 && crop(helper).performManualHarvest()) {
                gotLeather = countItems(helper, leather) > 0;
            }
        }
        helper.assertTrue(gotLeather, "Harvested corium drops leather");
        helper.assertTrue(
                crop(helper).getCurrentAge() == 1, "Corium resets to age one after harvest");
        helper.succeed();
    }

    static void cropGenericSpecialDrops(GameTestHelper helper) {
        helper.setBlock(POSITION.below(), Blocks.FARMLAND);
        helper.setBlock(POSITION, ModCrops.CROP_STICK.get().defaultBlockState());
        CropBlockEntity crop = crop(helper);
        crop.refreshTerrain(helper.getLevel());
        helper.assertTrue(
                crop.tryPlantIn(ModCrops.BLAZEREED_CARD, 0, 1, 1, 1, 0),
                "Blazereed plants on a stick");
        growUntil(helper, 3, 40000);
        helper.assertTrue(
                crop(helper).getCurrentAge() == 3, "Blazereed fills its block age three");
        // Blazereed's card maxSize is four, one above the block states: run past the growth
        // duration once more and the legacy clamp must hold the age at three without crashing.
        for (int i = 0; i < 2500 && crop(helper).getCurrentAge() == 3; i++) {
            CropBlockEntity be = crop(helper);
            be.refreshTerrain(helper.getLevel());
            be.performTick(1024L);
        }
        helper.assertTrue(
                crop(helper).getCurrentAge() == 3,
                "The over-grown blazereed stays at the block max age");
        // The special-drop roulette is pure getGains logic: one roll of length*2+2 where each
        // special owns a single slot, so every harvest carries the base drop and the specials
        // show up over repeated rolls.
        Item rod = Items.BLAZE_ROD;
        Item sulfur = ModItems.MATERIALS.get(MaterialDefinition.SULFUR_DUST).get();
        boolean sawRod = false;
        boolean sawSulfur = false;
        boolean alwaysBase = true;
        for (int roll = 0; roll < 200; roll++) {
            var gains = ModCrops.BLAZEREED_CARD.getGains(crop(helper));
            boolean base = false;
            for (ItemStack gain : gains) {
                base |= gain.is(Items.BLAZE_POWDER);
                sawRod |= gain.is(rod);
                sawSulfur |= gain.is(sulfur);
            }
            alwaysBase &= base && gains.size() >= 1 && gains.size() <= 2;
        }
        helper.assertTrue(alwaysBase, "Every blazereed harvest carries its blaze powder");
        helper.assertTrue(sawRod && sawSulfur, "The roulette yields both blaze rod and sulfur");
        Item chicken = Items.CHICKEN;
        Item feather = Items.FEATHER;
        boolean sawChicken = false;
        boolean sawFeather = false;
        for (int roll = 0; roll < 150; roll++) {
            for (ItemStack gain : ModCrops.EGG_PLANT_CARD.getGains(crop(helper))) {
                sawChicken |= gain.is(chicken);
                sawFeather |= gain.is(feather);
            }
        }
        helper.assertTrue(sawChicken && sawFeather, "Egg plant specials cover chicken and feather");
        helper.assertTrue(
                ModCrops.EGG_PLANT_CARD.getAgeAfterHarvest(crop(helper)) == 2
                        && ModCrops.SLIME_PLANT_CARD.getAgeAfterHarvest(crop(helper)) == 2,
                "Egg plant and slime plant regrow to age two");
        helper.assertTrue(
                ModCrops.SPIDERNIP_CARD.getGrowthDuration(crop(helper)) == 4 * 600
                        && ModCrops.MILK_WART_CARD.getGrowthDuration(crop(helper)) == 6 * 900
                        && ModCrops.MEAT_ROSE_CARD.getGrowthDuration(crop(helper)) == 7 * 1500
                        && ModCrops.OIL_BERRIES_CARD.getGrowthDuration(crop(helper)) == 9 * 200
                        && ModCrops.DIAREED_CARD.getGrowthDuration(crop(helper)) == 12 * 200,
                "Growth durations follow the tier times growth-speed table");
        helper.assertTrue(crop(helper).performManualHarvest(), "The grown blazereed harvests");
        helper.assertTrue(
                crop(helper).getCurrentAge() == 1, "Blazereed resets to age one after harvest");
        helper.succeed();
    }

    static void cropGenericMilkWartBaseSeed(GameTestHelper helper) {
        helper.setBlock(POSITION.below(), Blocks.FARMLAND);
        helper.setBlock(POSITION, ModCrops.CROP_STICK.get().defaultBlockState());
        CropBlockEntity crop = crop(helper);
        Item milkWart = ModItems.MATERIALS.get(MaterialDefinition.MILK_WART).get();
        helper.assertTrue(
                ModCrops.baseSeedFor(milkWart) != null, "The milk wart item is a base seed");
        // Milk wart's card maxSize is three but its block tops at age two: growth clamps there
        // and the harvest window (maxSize-1) still opens at the full block.
        helper.assertTrue(
                crop.rightClick(null, new ItemStack(milkWart)),
                "A held milk wart plants the milk wart crop");
        boolean gotMilkWart = false;
        for (int cycle = 0; cycle < 16 && !gotMilkWart; cycle++) {
            growUntil(helper, 2, 60000);
            if (crop(helper).getCurrentAge() == 2 && crop(helper).performManualHarvest()) {
                gotMilkWart = countItems(helper, milkWart) > 0;
            }
        }
        helper.assertTrue(gotMilkWart, "Harvested milk wart drops milk warts");
        helper.assertTrue(
                crop(helper).getCurrentAge() == 1, "Milk wart resets to age one after harvest");
        helper.succeed();
    }

    static void cropCrossingBaseInteractions(GameTestHelper helper) {
        helper.setBlock(POSITION.below(), Blocks.FARMLAND);
        helper.setBlock(POSITION, ModCrops.CROP_STICK.get().defaultBlockState());
        helper.assertFalse(crop(helper).isCrossingBase(), "A fresh stick is no crossing base");
        // Right-clicking with a crop stick upgrades the stick and consumes one item.
        ItemStack sticks = new ItemStack(ModCrops.CROP_STICK_ITEM.get(), 4);
        helper.assertTrue(crop(helper).rightClick(null, sticks), "A crop stick upgrades the stick");
        helper.assertTrue(crop(helper).isCrossingBase(), "The stick becomes a crossing base");
        helper.assertTrue(sticks.getCount() == 3, "Upgrading consumes one crop stick");
        helper.assertTrue(
                helper.getBlockState(POSITION).getValue(CropBlock.CROSSING_BASE),
                "The crossing base shows its upgraded block state");
        // A base seed is neither consumed nor planted on a crossing base.
        ItemStack milkWart =
                new ItemStack(ModItems.MATERIALS.get(MaterialDefinition.MILK_WART).get());
        helper.assertFalse(crop(helper).rightClick(null, milkWart), "A crossing base ignores seeds");
        helper.assertTrue(milkWart.getCount() == 1, "The base seed is kept");
        // Left-clicking the crossing base downgrades it and drops the stick.
        helper.assertTrue(
                crop(helper).onLeftClickEmpty(null), "Left-clicking downgrades the crossing base");
        helper.assertFalse(crop(helper).isCrossingBase(), "The downgrade clears the flag");
        helper.assertTrue(
                countItems(helper, ModCrops.CROP_STICK_ITEM.get()) == 1,
                "Downgrading drops the crop stick");
        helper.assertFalse(crop(helper).onLeftClickEmpty(null), "A plain stick ignores left clicks");
        helper.succeed();
    }

    /** The four breeding clusters, each a crossing base surrounded by four wheat neighbours. */
    private static final BlockPos[] CROSSING_BASES = {
        new BlockPos(3, 8, 3),
        new BlockPos(13, 8, 3),
        new BlockPos(13, 8, 13),
        new BlockPos(3, 8, 13)
    };

    private static void plantWheatNeighbour(GameTestHelper helper, BlockPos pos, int age) {
        helper.setBlock(pos.below(), Blocks.FARMLAND);
        helper.setBlock(
                pos, ModCrops.WHEAT_CROP.get().defaultBlockState().setValue(WheatCropBlock.AGE, age));
        CropBlockEntity neighbour = helper.getBlockEntity(pos, CropBlockEntity.class);
        neighbour.setStatGrowth(10);
        neighbour.setStatGain(10);
        neighbour.setStatResistance(10);
    }

    static void cropCrossingBreed(GameTestHelper helper) {
        for (BlockPos base : CROSSING_BASES) {
            helper.setBlock(base.below(), Blocks.FARMLAND);
            helper.setBlock(base, ModCrops.CROP_STICK.get().defaultBlockState());
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                plantWheatNeighbour(helper, base.relative(direction), 3);
            }
        }
        // The crossing chance is probabilistic; looping the attempt keeps the assertion
        // deterministic while the 10/10/10 stats pin the inheritance window.
        int wheatBreeds = 0;
        for (BlockPos base : CROSSING_BASES) {
            boolean crossed = false;
            for (int attempt = 0; attempt < 300 && !crossed; attempt++) {
                CropBlockEntity crop = helper.getBlockEntity(base, CropBlockEntity.class);
                crop.refreshTerrain(helper.getLevel());
                crossed = crop.attemptCrossing(helper.getLevel());
            }
            helper.assertTrue(crossed, "A surrounded crossing base eventually breeds");
            var card = ModCrops.cardFor(helper.getBlockState(base).getBlock());
            helper.assertTrue(card != null, "The breeding grows a crop card");
            CropBlockEntity grown = helper.getBlockEntity(base, CropBlockEntity.class);
            helper.assertTrue(grown.getCurrentAge() == 0, "The new crop starts at age zero");
            helper.assertTrue(
                    grown.getStatGrowth() >= 6
                            && grown.getStatGrowth() <= 14
                            && grown.getStatGain() >= 6
                            && grown.getStatGain() <= 14
                            && grown.getStatResistance() >= 6
                            && grown.getStatResistance() <= 14,
                    "The breed averages the neighbour stats plus jitter, saw "
                            + grown.getStatGrowth()
                            + "/"
                            + grown.getStatGain()
                            + "/"
                            + grown.getStatResistance());
            wheatBreeds += card == ModCrops.WHEAT_CARD ? 1 : 0;
        }
        helper.assertTrue(wheatBreeds > 0, "Four wheat neighbours overwhelmingly breed wheat");
        helper.succeed();
    }

    static void cropCrossingSpread(GameTestHelper helper) {
        helper.setBlock(POSITION.below(), Blocks.FARMLAND);
        helper.setBlock(POSITION, ModCrops.CROP_STICK.get().defaultBlockState());
        crop(helper).setCrossingBase(true);
        BlockPos neighbourPos = POSITION.north();
        plantWheatNeighbour(helper, neighbourPos, 3);
        var neighbour = helper.getBlockEntity(neighbourPos, CropBlockEntity.class);
        neighbour.setStatGrowth(5);
        neighbour.setStatGain(6);
        neighbour.setStatResistance(7);
        boolean spread = false;
        for (int attempt = 0; attempt < 200 && !spread; attempt++) {
            spread = crop(helper).attemptSpreading(helper.getLevel());
        }
        helper.assertTrue(spread, "A single strong neighbour eventually spreads onto the base");
        helper.assertTrue(
                ModCrops.cardFor(helper.getBlockState(POSITION).getBlock())
                        == ModCrops.WHEAT_CARD,
                "The base adopts the neighbour's wheat");
        CropBlockEntity spreadCrop = crop(helper);
        helper.assertTrue(spreadCrop.getCurrentAge() == 0, "The spread crop starts at age zero");
        helper.assertTrue(
                spreadCrop.getStatGrowth() == 5
                        && spreadCrop.getStatGain() == 6
                        && spreadCrop.getStatResistance() == 7,
                "Spreading copies the neighbour stats exactly, saw "
                        + spreadCrop.getStatGrowth()
                        + "/"
                        + spreadCrop.getStatGain()
                        + "/"
                        + spreadCrop.getStatResistance());
        // Negative: two crop neighbours never spread (legacy requires exactly one).
        BlockPos twoNeighbours = new BlockPos(3, 8, 3);
        helper.setBlock(twoNeighbours.below(), Blocks.FARMLAND);
        helper.setBlock(twoNeighbours, ModCrops.CROP_STICK.get().defaultBlockState());
        Direction[] sides = {Direction.NORTH, Direction.SOUTH};
        for (Direction direction : sides) plantWheatNeighbour(helper, twoNeighbours.relative(direction), 3);
        CropBlockEntity crowded = helper.getBlockEntity(twoNeighbours, CropBlockEntity.class);
        crowded.setCrossingBase(true);
        helper.assertFalse(
                crowded.attemptSpreading(helper.getLevel()), "Two neighbours never spread");
        // Negative: an immature neighbour cannot cross onto the base.
        BlockPos immature = new BlockPos(13, 8, 3);
        helper.setBlock(immature.below(), Blocks.FARMLAND);
        helper.setBlock(immature, ModCrops.CROP_STICK.get().defaultBlockState());
        plantWheatNeighbour(helper, immature.north(), 1);
        CropBlockEntity youngBase = helper.getBlockEntity(immature, CropBlockEntity.class);
        youngBase.setCrossingBase(true);
        helper.assertFalse(
                youngBase.attemptSpreading(helper.getLevel()),
                "An immature neighbour cannot spread");
        // Negative: an empty neighbouring stick has no crop to spread.
        BlockPos emptySide = new BlockPos(3, 8, 13);
        helper.setBlock(emptySide.below(), Blocks.FARMLAND);
        helper.setBlock(emptySide, ModCrops.CROP_STICK.get().defaultBlockState());
        helper.setBlock(emptySide.north().below(), Blocks.FARMLAND);
        helper.setBlock(emptySide.north(), ModCrops.CROP_STICK.get().defaultBlockState());
        CropBlockEntity bareBase = helper.getBlockEntity(emptySide, CropBlockEntity.class);
        bareBase.setCrossingBase(true);
        helper.assertFalse(
                bareBase.attemptSpreading(helper.getLevel()),
                "An empty neighbour has no crop to spread");
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
