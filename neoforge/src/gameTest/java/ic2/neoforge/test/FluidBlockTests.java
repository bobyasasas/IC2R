package ic2.neoforge.test;

import ic2.neoforge.fluid.AirFluidBlock;
import ic2.neoforge.fluid.FluidDefinition;
import ic2.neoforge.fluid.HotWaterFluidBlock;
import ic2.neoforge.fluid.PahoehoeLavaFluidBlock;
import ic2.neoforge.fluid.UUMatterFluidBlock;
import ic2.neoforge.registration.ModFluids;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.level.GameType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Legacy misc fluid blocks: steam blinds, UU matter heals/bottles/annihilates, hot water and
 *  pahoehoe lava age into their cooled neighbours, hot coolant and pahoehoe ignite entities. */
final class FluidBlockTests {
    private static final BlockPos FLUID = new BlockPos(2, 2, 2);

    static void steamBlindsEntities(GameTestHelper helper) {
        var pig = wadeInto(helper, FluidDefinition.STEAM, FLUID);
        var superPig = wadeInto(helper, FluidDefinition.SUPERHEATED_STEAM, new BlockPos(6, 2, 2));
        helper.runAtTickTime(40, () -> {
            helper.assertTrue(
                    pig.hasEffect(MobEffects.BLINDNESS),
                    "Steam in the world blinds the entity wading through it");
            helper.assertTrue(
                    superPig.hasEffect(MobEffects.BLINDNESS),
                    "Superheated steam shares the legacy steam block behaviour");
            helper.succeed();
        });
    }

    static void uuMatterRegeneratesAndBottles(GameTestHelper helper) {
        var level = (Level) helper.getLevel();
        place(helper, FluidDefinition.UU_MATTER, FLUID);
        var pig = wadeInto(helper, FluidDefinition.UU_MATTER, FLUID);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GLASS_BOTTLE));
        helper.runAtTickTime(40, () -> {
            var effect = pig.getEffect(MobEffects.REGENERATION);
            helper.assertTrue(
                    effect != null && effect.getAmplifier() == 1,
                    "UU matter grants amplified regeneration");
            var state = helper.getBlockState(FLUID);
            var pos = helper.absolutePos(FLUID);
            InteractionResult result = InteractionResult.FAIL;
            if (state.getBlock() instanceof UUMatterFluidBlock uu) {
                result = uu.useItemOn(
                        player.getItemInHand(InteractionHand.MAIN_HAND),
                        state,
                        level,
                        pos,
                        player,
                        InteractionHand.MAIN_HAND,
                        new BlockHitResult(pos.getCenter(), Direction.UP, pos, false));
            }
            helper.assertTrue(
                    result == InteractionResult.SUCCESS,
                    "Bottling a source accepts the interaction");
            helper.assertTrue(
                    player.getItemInHand(InteractionHand.MAIN_HAND).is(Items.POTION),
                    "The glass bottle leaves the hand as a potion bottle");
            var contents =
                    player.getItemInHand(InteractionHand.MAIN_HAND)
                            .get(net.minecraft.core.component.DataComponents.POTION_CONTENTS);
            helper.assertTrue(
                    contents != null && contents.is(Potions.WATER),
                    "The bottled fluid is plain water");
            helper.succeed();
        });
    }

    static void uuMatterAnnihilatesNeighborFluids(GameTestHelper helper) {
        var level = (ServerLevel) helper.getLevel();
        var uuState = place(helper, FluidDefinition.UU_MATTER, FLUID);
        var uuPos = helper.absolutePos(FLUID);
        var lavaPos = uuPos.east();
        level.setBlock(lavaPos, Blocks.LAVA.defaultBlockState(), 3);
        if (uuState.getBlock() instanceof UUMatterFluidBlock uu) {
            uu.neighborChanged(uuState, level, uuPos, Blocks.LAVA, null, false);
        }
        helper.assertTrue(
                level.getBlockState(lavaPos).is(Blocks.OBSIDIAN),
                "Neighbor source lava freezes into obsidian");
        var waterPos = uuPos.above();
        level.setBlock(waterPos, Blocks.WATER.defaultBlockState(), 3);
        if (uuState.getBlock() instanceof UUMatterFluidBlock uu) {
            uu.neighborChanged(uuState, level, uuPos, Blocks.WATER, null, false);
        }
        helper.assertTrue(
                level.getBlockState(waterPos).isAir(),
                "Neighbor non-lava source fluid vanishes");
        helper.succeed();
    }

    static void hotWaterHealsThenCools(GameTestHelper helper) {
        var level = (ServerLevel) helper.getLevel();
        var state = place(helper, FluidDefinition.HOT_WATER, FLUID);
        var pig = wadeInto(helper, FluidDefinition.HOT_WATER, FLUID);
        helper.runAtTickTime(20, () -> {
            helper.assertTrue(
                    pig.hasEffect(MobEffects.REGENERATION),
                    "Touching hot water heals the entity");
            helper.assertTrue(
                    helper.getBlockState(FLUID).is(Blocks.WATER),
                    "The touched hot water source cools to plain water in place");
            var untouched = new BlockPos(6, 2, 2);
            var cold = place(helper, FluidDefinition.HOT_WATER, untouched);
            if (cold.getBlock() instanceof HotWaterFluidBlock hot) {
                hot.tick(cold, level, helper.absolutePos(untouched), RandomSource.create());
            }
            helper.assertTrue(
                    helper.getBlockState(untouched).is(Blocks.WATER),
                    "The scheduled cooldown tick settles hot water into plain water");
            helper.succeed();
        });
    }

    static void hotCoolantIgnitesEntities(GameTestHelper helper) {
        var pig = wadeInto(helper, FluidDefinition.HOT_COOLANT, FLUID);
        helper.runAtTickTime(40, () -> {
            helper.assertTrue(
                    pig.isOnFire(),
                    "Hot coolant sets the entity wading through it on fire");
            helper.succeed();
        });
    }

    static void pahoehoeBurnsEntities(GameTestHelper helper) {
        place(helper, FluidDefinition.PAHOEHOE_LAVA, FLUID);
        var pig = wadeInto(helper, FluidDefinition.PAHOEHOE_LAVA, FLUID);
        helper.runAtTickTime(40, () -> {
            helper.assertTrue(
                    pig.getHealth() < pig.getMaxHealth(),
                    "Pahoehoe lava hurts like lava");
            helper.assertTrue(
                    pig.isOnFire(),
                    "Pahoehoe lava sets the entity on fire");
            helper.succeed();
        });
    }

    static void pahoehoeSolidifiesToBasalt(GameTestHelper helper) {
        var level = (ServerLevel) helper.getLevel();
        var state = place(helper, FluidDefinition.PAHOEHOE_LAVA, FLUID);
        var pos = helper.absolutePos(FLUID);
        if (state.getBlock() instanceof PahoehoeLavaFluidBlock lava) {
            lava.tick(state, level, pos, RandomSource.create());
        }
        helper.assertTrue(
                helper.getBlockState(FLUID).is(Blocks.BASALT),
                "The scheduled cooldown settles pahoehoe lava into basalt");
        var wet = new BlockPos(6, 2, 2);
        var wetState = place(helper, FluidDefinition.PAHOEHOE_LAVA, wet);
        var wetPos = helper.absolutePos(wet);
        var waterPos = wetPos.above();
        level.setBlock(waterPos, Blocks.WATER.defaultBlockState(), 3);
        if (wetState.getBlock() instanceof PahoehoeLavaFluidBlock lava) {
            lava.neighborChanged(wetState, level, wetPos, Blocks.WATER, null, false);
        }
        var solidified = helper.getBlockState(wet);
        helper.assertTrue(
                solidified.is(Blocks.BASALT) && !(solidified.getBlock() instanceof PahoehoeLavaFluidBlock),
                "Pahoehoe lava touching water turns straight into basalt");
        helper.succeed();
    }

    static void hydrogenDetonatesNearFire(GameTestHelper helper) {
        var level = (ServerLevel) helper.getLevel();
        var hydrogenState = place(helper, FluidDefinition.HYDROGEN, FLUID);
        var pos = helper.absolutePos(FLUID);
        var markerPos = pos.east();
        level.setBlock(markerPos, Blocks.GLASS.defaultBlockState(), 3);
        var firePos = pos.west();
        level.setBlock(firePos.below(), Blocks.NETHERRACK.defaultBlockState(), 3);
        level.setBlock(firePos, Blocks.FIRE.defaultBlockState(), 3);
        helper.assertTrue(
                hydrogenState.getBlock() instanceof ic2.neoforge.fluid.HydrogenFluidBlock,
                "The hydrogen family carries the legacy hydrogen block");
        helper.assertTrue(
                helper.getBlockState(FLUID).isAir(),
                "The hydrogen source detonates away next to fire");
        helper.assertTrue(
                !level.getBlockState(markerPos).is(Blocks.GLASS),
                "The detonation breaks blocks around the hydrogen");
        helper.succeed();
    }

    static void airBlockStaysInert(GameTestHelper helper) {
        place(helper, FluidDefinition.AIR, FLUID);
        var pig = wadeInto(helper, FluidDefinition.AIR, FLUID);
        helper.runAtTickTime(40, () -> {
            helper.assertTrue(
                    helper.getBlockState(FLUID).getBlock() instanceof AirFluidBlock,
                    "The air fluid block stays in place");
            helper.assertTrue(
                    !pig.isOnFire() && pig.getHealth() >= pig.getMaxHealth(),
                    "Air fluid does not hurt or ignite entities");
            helper.succeed();
        });
    }

    private static BlockState place(GameTestHelper helper, FluidDefinition definition, BlockPos pos) {
        BlockState state = ModFluids.FAMILIES.get(definition).block().get().defaultBlockState();
        helper.setBlock(pos, state);
        return helper.getBlockState(pos);
    }

    private static Pig wadeInto(GameTestHelper helper, FluidDefinition definition, BlockPos pos) {
        place(helper, definition, pos);
        Pig pig = helper.spawn(EntityType.PIG, pos);
        pig.setNoAi(true);
        return pig;
    }

    private FluidBlockTests() {}
}
