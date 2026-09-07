package ic2.neoforge.test;

import ic2.neoforge.item.ElectricItemEnergy;
import ic2.neoforge.registration.MaterialDefinition;
import ic2.neoforge.registration.ModItems;
import ic2.neoforge.registration.ModTools;
import ic2.neoforge.registration.ModWorldContent;
import ic2.neoforge.world.ResinState;
import ic2.neoforge.world.RubberLogBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.ItemAbilities;

import java.util.List;

final class WorldContentTests {
    static void loadedFeatures(GameTestHelper helper) {
        var registry = helper.getLevel().registryAccess();
        for (String name :
                List.of(
                        "rubber_tree",
                        "lead_ore",
                        "tin_ore",
                        "tin_ore_small",
                        "uranium_ore",
                        "uranium_ore_large",
                        "uranium_ore_buried")) {
            registry.lookupOrThrow(Registries.CONFIGURED_FEATURE)
                    .getOrThrow(
                            ResourceKey.create(
                                    Registries.CONFIGURED_FEATURE,
                                    Identifier.parse("ic2:" + name)));
        }
        var forest = registry.lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.FOREST).value();
        var swamp = registry.lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.SWAMP).value();
        var tree =
                registry.lookupOrThrow(Registries.PLACED_FEATURE)
                        .getOrThrow(
                                ResourceKey.create(
                                        Registries.PLACED_FEATURE,
                                        Identifier.parse("ic2:trees_rubber_forest")));
        var swampTree =
                registry.lookupOrThrow(Registries.PLACED_FEATURE)
                        .getOrThrow(
                                ResourceKey.create(
                                        Registries.PLACED_FEATURE,
                                        Identifier.parse("ic2:trees_rubber_swamp")));
        helper.assertTrue(
                forest.getGenerationSettings().features().stream()
                        .anyMatch(set -> set.contains(tree)),
                "Forest modifier must actually attach rubber trees");
        helper.assertTrue(
                swamp.getGenerationSettings().features().stream()
                        .anyMatch(set -> set.contains(swampTree)),
                "Common swamp biome tag must attach its tree feature");
        for (String ore :
                List.of(
                        "lead_ore",
                        "lead_ore_lower",
                        "tin_ore_small",
                        "tin_ore_upper",
                        "uranium_ore",
                        "uranium_ore_large",
                        "uranium_ore_buried")) {
            var feature =
                    registry.lookupOrThrow(Registries.PLACED_FEATURE)
                            .getOrThrow(
                                    ResourceKey.create(
                                            Registries.PLACED_FEATURE,
                                            Identifier.parse("ic2:" + ore)));
            helper.assertTrue(
                    forest.getGenerationSettings().features().stream()
                            .anyMatch(set -> set.contains(feature)),
                    "Overworld must include ore placement: " + ore);
        }
        helper.succeed();
    }

    static void sapling(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(8, 1, 8));
        level.setBlockAndUpdate(pos, ModWorldContent.RUBBER_SAPLING.get().defaultBlockState());
        var sapling = ModWorldContent.RUBBER_SAPLING.get();
        var random = RandomSource.create(12);
        sapling.advanceTree(level, pos, level.getBlockState(pos), random);
        sapling.advanceTree(level, pos, level.getBlockState(pos), random);
        helper.assertTrue(
                level.getBlockState(pos).is(ModWorldContent.RUBBER_LOG.get()),
                "Sapling must grow through the registered tree configuration");
        int trunk = 0, leaves = 0;
        for (int y = 0; y < 14; y++)
            if (level.getBlockState(pos.above(y)).is(ModWorldContent.RUBBER_LOG.get())) trunk++;
        for (var p : BlockPos.betweenClosed(pos.offset(-3, 0, -3), pos.offset(3, 15, 3)))
            if (level.getBlockState(p).is(ModWorldContent.RUBBER_LEAVES.get())) leaves++;
        helper.assertTrue(
                trunk >= 4 && trunk <= 8 && leaves >= 20,
                "Rubber tree must retain its height range and crown");
        helper.succeed();
    }

    static void resin(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(8, 2, 8));
        var log = ModWorldContent.RUBBER_LOG.get();
        var wet = log.defaultBlockState().setValue(RubberLogBlock.RESIN, ResinState.WET_NORTH);
        level.setBlockAndUpdate(pos, wet);
        level.setBlockAndUpdate(
                pos.above(), ModWorldContent.RUBBER_LEAVES.get().defaultBlockState());
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, ModTools.TREETAP.toStack());
        var wrong =
                new UseOnContext(
                        player,
                        InteractionHand.MAIN_HAND,
                        new BlockHitResult(Vec3.atCenterOf(pos), Direction.SOUTH, pos, false));
        ModTools.TREETAP.get().useOn(wrong);
        helper.assertTrue(
                level.getBlockState(pos).getValue(RubberLogBlock.RESIN).wet()
                        && player.getMainHandItem().getDamageValue() == 0,
                "Wrong log face must not spend a tap use");
        var context =
                new UseOnContext(
                        player,
                        InteractionHand.MAIN_HAND,
                        new BlockHitResult(Vec3.atCenterOf(pos), Direction.NORTH, pos, false));
        ModTools.TREETAP.get().useOn(context);
        int resin =
                helper.getEntities(EntityType.ITEM).stream()
                        .mapToInt(
                                e ->
                                        e.getItem()
                                                        .is(
                                                                ModItems.MATERIALS
                                                                        .get(
                                                                                MaterialDefinition
                                                                                        .RESIN)
                                                                        .get())
                                                ? e.getItem().getCount()
                                                : 0)
                        .sum();
        helper.assertTrue(
                resin >= 1
                        && resin <= 3
                        && level.getBlockState(pos).getValue(RubberLogBlock.RESIN)
                                == ResinState.DRY_NORTH
                        && player.getMainHandItem().getDamageValue() == 1,
                "Wet tapping must produce resin once and dry the hole");
        helper.assertTrue(
                RubberLogBlock.canRegenerate(level, pos),
                "A leaf-connected log must remain renewable");
        var random = RandomSource.create(1);
        for (int i = 0;
                i < 100 && !level.getBlockState(pos).getValue(RubberLogBlock.RESIN).wet();
                i++) level.getBlockState(pos).randomTick(level, pos, random);
        helper.assertTrue(
                level.getBlockState(pos).getValue(RubberLogBlock.RESIN).wet(),
                "Random ticking must regenerate a dry resin hole");
        level.setBlockAndUpdate(pos.above(), Blocks.AIR.defaultBlockState());
        helper.assertTrue(
                !RubberLogBlock.canRegenerate(level, pos)
                        && wet.getPistonPushReaction() == PushReaction.BLOCK,
                "Detached resin logs cannot regenerate or be moved by pistons");
        var tap = ModTools.ELECTRIC_TREETAP.toStack();
        ElectricItemEnergy.charge(tap, 50, 1, true, false);
        player.setItemInHand(InteractionHand.MAIN_HAND, tap);
        var electricContext =
                new UseOnContext(
                        player,
                        InteractionHand.MAIN_HAND,
                        new BlockHitResult(Vec3.atCenterOf(pos), Direction.NORTH, pos, false));
        ModTools.ELECTRIC_TREETAP.get().useOn(electricContext);
        helper.assertTrue(
                ElectricItemEnergy.charge(tap) == 0
                        && !level.getBlockState(pos).getValue(RubberLogBlock.RESIN).wet(),
                "Electric tapping must spend exactly 50 EU");
        helper.succeed();
    }

    static void lootAndStripping(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(8, 2, 8));
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        for (var entry : ModWorldContent.ORES.entrySet()) {
            var state = entry.getValue().get().defaultBlockState();
            level.setBlockAndUpdate(pos, state);
            String metal = entry.getKey().replace("deepslate_", "").replace("_ore", "");
            var normal =
                    Block.getDrops(
                            state, level, pos, null, player, new ItemStack(Items.DIAMOND_PICKAXE));
            helper.assertTrue(
                    normal.size() == 1
                            && normal.getFirst()
                                    .is(
                                            BuiltInRegistries.ITEM.getValue(
                                                    Identifier.parse("ic2:raw_" + metal))),
                    "Ore loot must yield its raw material: " + entry.getKey());
            var silk = new ItemStack(Items.DIAMOND_PICKAXE);
            silk.enchant(
                    level.registryAccess()
                            .lookupOrThrow(Registries.ENCHANTMENT)
                            .getOrThrow(Enchantments.SILK_TOUCH),
                    1);
            var silkDrops = Block.getDrops(state, level, pos, null, player, silk);
            helper.assertTrue(
                    silkDrops.size() == 1
                            && silkDrops.getFirst().is(entry.getValue().get().asItem()),
                    "Modern component predicate must preserve silk touch");
        }
        var wet =
                ModWorldContent.RUBBER_LOG
                        .get()
                        .defaultBlockState()
                        .setValue(RubberLogBlock.RESIN, ResinState.WET_NORTH);
        level.setBlockAndUpdate(pos, wet);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_AXE));
        var context =
                new UseOnContext(
                        player,
                        InteractionHand.MAIN_HAND,
                        new BlockHitResult(Vec3.atCenterOf(pos), Direction.NORTH, pos, false));
        var simulated = wet.getToolModifiedState(context, ItemAbilities.AXE_STRIP, true);
        helper.assertTrue(
                simulated.is(ModWorldContent.STRIPPED_RUBBER_LOG.get())
                        && helper.getEntities(EntityType.ITEM).isEmpty(),
                "Simulated axe stripping must not eject resin");
        Items.IRON_AXE.useOn(context);
        helper.assertTrue(
                level.getBlockState(pos).is(ModWorldContent.STRIPPED_RUBBER_LOG.get())
                        && !helper.getEntities(EntityType.ITEM).isEmpty(),
                "Native axe use must strip the log and eject wet resin");
        helper.succeed();
    }

    static void orePlacement(GameTestHelper helper) {
        var level = helper.getLevel();
        var center = helper.absolutePos(new BlockPos(8, 8, 8));
        var region = BlockPos.betweenClosed(center.offset(-6, -6, -6), center.offset(6, 6, 6));
        for (boolean deep : new boolean[] {false, true})
            for (String metal : List.of("lead", "tin", "uranium")) {
                var base = (deep ? Blocks.DEEPSLATE : Blocks.STONE).defaultBlockState();
                for (var pos : region) level.setBlock(pos, base, 2);
                var key =
                        ResourceKey.<ConfiguredFeature<?, ?>>create(
                                Registries.CONFIGURED_FEATURE,
                                Identifier.parse("ic2:" + metal + "_ore"));
                var feature =
                        level.registryAccess()
                                .lookupOrThrow(Registries.CONFIGURED_FEATURE)
                                .getOrThrow(key)
                                .value();
                boolean placed = false;
                for (int seed = 0; seed < 8 && !placed; seed++)
                    placed =
                            feature.place(
                                    level,
                                    level.getChunkSource().getGenerator(),
                                    RandomSource.create(seed),
                                    center);
                var expected =
                        ModWorldContent.ORES.get((deep ? "deepslate_" : "") + metal + "_ore").get();
                int ores = 0;
                for (var pos : region) if (level.getBlockState(pos).is(expected)) ores++;
                helper.assertTrue(
                        placed && ores > 0,
                        "Ore feature must replace the correct host rock: " + expected);
            }
        helper.succeed();
    }

    static void leafSupport(GameTestHelper helper) {
        var level = helper.getLevel();
        var log = helper.absolutePos(new BlockPos(8, 3, 8));
        var leaf = log.east();
        level.setBlockAndUpdate(leaf, ModWorldContent.RUBBER_LEAVES.get().defaultBlockState());
        level.setBlockAndUpdate(log, ModWorldContent.RUBBER_LOG.get().defaultBlockState());
        helper.assertTrue(
                level.getBlockState(log).is(BlockTags.LOGS)
                        && level.getBlockState(log).is(BlockTags.PREVENTS_NEARBY_LEAF_DECAY),
                "Nested common tags must put rubber logs in the native logs tag");
        helper.runAtTickTime(
                5,
                () -> {
                    helper.assertTrue(
                            level.getBlockState(leaf).getValue(LeavesBlock.DISTANCE) == 1,
                            "Leaves must recognize rubber logs as support");
                    level.removeBlock(log, false);
                });
        helper.runAtTickTime(
                10,
                () -> {
                    var state = level.getBlockState(leaf);
                    if (!state.isAir()) {
                        helper.assertTrue(
                                state.getValue(LeavesBlock.DISTANCE) == 7,
                                "Removing the trunk must invalidate leaf support");
                        state.randomTick(level, leaf, RandomSource.create(3));
                    }
                    helper.assertTrue(
                            level.getBlockState(leaf).isAir(),
                            "Unsupported natural leaves must decay");
                    helper.succeed();
                });
    }

    private WorldContentTests() {}
}
