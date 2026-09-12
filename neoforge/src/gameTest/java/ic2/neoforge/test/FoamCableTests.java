package ic2.neoforge.test;

import ic2.neoforge.component.ModDataComponents;
import ic2.neoforge.energy.FoamCableBlock;
import ic2.neoforge.item.CutterItem;
import ic2.neoforge.item.FoamSprayerItem;
import ic2.neoforge.machine.EnergyStorageBlockEntity;
import ic2.neoforge.machine.GeneratorBlockEntity;
import ic2.neoforge.machine.MachineBlock;
import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.registration.ModFoam;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.registration.ModTools;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Method;
import java.util.UUID;

final class FoamCableTests {
    // Same working corner as CableShockTests; the floor sits at y=0 so blocks stay on y=1.
    private static final BlockPos WEST = new BlockPos(1, 1, 2),
            MID = new BlockPos(2, 1, 2),
            EAST = new BlockPos(3, 1, 2);

    private static EnergyStorageBlockEntity buildLine(
            GameTestHelper helper, String cableId, MachineKind storageKind) {
        helper.setBlock(
                WEST,
                ModMachines.block(MachineKind.GENERATOR)
                        .defaultBlockState()
                        .setValue(MachineBlock.FACING, Direction.EAST));
        helper.setBlock(MID, ModMachines.CABLES.get(cableId).get());
        helper.setBlock(
                EAST,
                ModMachines.block(storageKind)
                        .defaultBlockState()
                        .setValue(MachineBlock.FACING, Direction.EAST));
        var generator = helper.getBlockEntity(WEST, GeneratorBlockEntity.class);
        var sink = helper.getBlockEntity(EAST, EnergyStorageBlockEntity.class);
        generator.energy().restore(1000);
        return sink;
    }

    private static ItemStack loadedSprayer(int mB) {
        ItemStack stack = new ItemStack(ModFoam.FOAM_SPRAYER.get());
        if (mB > 0) {
            stack.set(
                    ModDataComponents.FLUID,
                    new net.neoforged.neoforge.fluids.FluidStackTemplate(
                            FoamSprayerItem.foamFluid(), mB));
        }
        return stack;
    }

    private static void spray(GameTestHelper helper, ItemStack sprayer, BlockPos clicked) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, sprayer);
        BlockPos absolute = helper.absolutePos(clicked);
        var context =
                new UseOnContext(
                        helper.getLevel(),
                        player,
                        InteractionHand.MAIN_HAND,
                        sprayer,
                        new BlockHitResult(
                                Vec3.atCenterOf(absolute), Direction.UP, absolute, false));
        ModFoam.FOAM_SPRAYER.get().useOn(context);
    }

    static void sprayDipsCableInFoam(GameTestHelper helper) {
        buildLine(helper, "insulated_tin_cable", MachineKind.MFE);
        ItemStack sprayer = loadedSprayer(1000);
        spray(helper, sprayer, MID);
        var foamBlock = ModMachines.FOAM_CABLES.get("insulated_tin_foam_cable").get();
        BlockState state = helper.getBlockState(MID);
        helper.assertTrue(
                state.getBlock() == foamBlock,
                "Spraying a plain cable swaps it to its foam-covered counterpart");
        helper.assertTrue(
                state.getValue(FoamCableBlock.FOAM).isSoft(),
                "Freshly dipped foam starts out soft");
        helper.assertTrue(
                FoamSprayerItem.getContentsMb(sprayer) == 900,
                "Dipping one cable drains one hundred mB");
        helper.assertTrue(
                helper.getLevel()
                        .getBlockTicks()
                        .hasScheduledTick(helper.absolutePos(MID), foamBlock),
                "Soft foam immediately schedules its hardening probe");
        helper.succeed();
    }

    static void spraySpreadsAlongCableNetwork(GameTestHelper helper) {
        var copper = ModMachines.CABLES.get("copper_cable").get();
        helper.setBlock(WEST, copper);
        helper.setBlock(MID, copper);
        helper.setBlock(EAST, copper);
        ItemStack sprayer = loadedSprayer(1000);
        spray(helper, sprayer, MID);
        var foamBlock = ModMachines.FOAM_CABLES.get("copper_foam_cable").get();
        helper.assertTrue(
                helper.getBlockState(WEST).getBlock() == foamBlock
                        && helper.getBlockState(MID).getBlock() == foamBlock
                        && helper.getBlockState(EAST).getBlock() == foamBlock,
                "The spray flood-fills across every connected plain cable");
        helper.assertTrue(
                FoamSprayerItem.getContentsMb(sprayer) == 700,
                "Every dipped cable drains one hundred mB");

        // Single mode only dips the clicked cable again.
        helper.setBlock(MID, copper);
        ItemStack single = loadedSprayer(1000);
        single.set(ModDataComponents.SPRAY_MODE, 1);
        spray(helper, single, MID);
        helper.assertTrue(
                helper.getBlockState(MID).getBlock() == foamBlock,
                "Single mode dips the clicked cable");
        helper.assertTrue(
                FoamSprayerItem.getContentsMb(single) == 900,
                "Single mode drains only one hundred mB");
        helper.succeed();
    }

    static void foamCableCarriesPower(GameTestHelper helper) {
        EnergyStorageBlockEntity sink = buildLine(helper, "insulated_tin_cable", MachineKind.MFE);
        spray(helper, loadedSprayer(1000), MID);
        helper.assertTrue(
                helper.getBlockState(MID).getBlock()
                        == ModMachines.FOAM_CABLES.get("insulated_tin_foam_cable").get(),
                "The dipped line is foam-covered");
        helper.runAtTickTime(
                20,
                () -> {
                    helper.assertTrue(
                            sink.energy().stored() > 0,
                            "The foam-covered cable still conducts its packet");
                    helper.succeed();
                });
    }

    static void breakingFoamRevealsCable(GameTestHelper helper) {
        helper.setBlock(MID, ModMachines.FOAM_CABLES.get("insulated_tin_foam_cable").get());
        helper.setBlock(
                MID.east(),
                ModMachines.block(MachineKind.MFE)
                        .defaultBlockState()
                        .setValue(MachineBlock.FACING, Direction.WEST));
        var player =
                new net.neoforged.neoforge.common.util.FakePlayer(
                        helper.getLevel(),
                        new com.mojang.authlib.GameProfile(
                                UUID.randomUUID(), "ic2-foam-cable-test"));
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        // 26.1.2 destroyBlock always reports true; the hook's false only suppresses the
        // destroy-side effects (drops), so the revealed state is the observable contract.
        player.gameMode.destroyBlock(helper.absolutePos(MID));
        BlockState revealed = helper.getBlockState(MID);
        helper.assertTrue(
                revealed.is(ModMachines.CABLES.get("insulated_tin_cable").get()),
                "Breaking the foam shell reveals the plain cable again");
        helper.assertTrue(
                revealed.getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(Direction.EAST)),
                "The revealed cable rebuilds its six-way connections");
        helper.succeed();
    }

    static void cutterStripsFoam(GameTestHelper helper) {
        ItemStack cutter = new ItemStack(ModTools.CUTTER.get());
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, cutter);
        BlockPos absolute = helper.absolutePos(MID);

        helper.setBlock(MID, ModMachines.FOAM_CABLES.get("insulated_tin_foam_cable").get());
        CutterItem.strip(player, helper.getLevel(), absolute, helper.getBlockState(MID));
        helper.assertTrue(
                helper.getBlockState(MID).is(ModMachines.CABLES.get("tin_cable").get()),
                "Cutting the soft shell strips one insulation layer and discards the foam");
        helper.assertTrue(
                cutter.getDamageValue() == 3, "Stripping foam costs three tool durability");

        helper.setBlock(MID, ModMachines.FOAM_CABLES.get("tin_foam_cable").get());
        CutterItem.strip(player, helper.getLevel(), absolute, helper.getBlockState(MID));
        helper.assertTrue(
                helper.getBlockState(MID)
                        .is(ModMachines.FOAM_CABLES.get("tin_foam_cable").get()),
                "A bare foam cable has no layer left to cut");

        helper.setBlock(
                MID,
                ModMachines.FOAM_CABLES
                        .get("insulated_tin_foam_cable")
                        .get()
                        .defaultBlockState()
                        .setValue(FoamCableBlock.FOAM, FoamCableBlock.Foam.HARD));
        CutterItem.strip(player, helper.getLevel(), absolute, helper.getBlockState(MID));
        helper.assertTrue(
                helper.getBlockState(MID)
                        .is(ModMachines.FOAM_CABLES.get("insulated_tin_foam_cable").get()),
                "Hardened foam shields the insulation from the cutter");
        helper.succeed();
    }

    static void softFoamHardensDeterministically(GameTestHelper helper) {
        var foamBlock = ModMachines.FOAM_CABLES.get("tin_foam_cable").get();
        helper.setBlock(MID, foamBlock);
        BlockState soft = helper.getBlockState(MID);
        // A fixed losing roll keeps probing, a winning roll cures the shell in place.
        try {
            Method probe =
                    FoamCableBlock.class.getDeclaredMethod(
                            "tickFoamHardening",
                            BlockState.class,
                            ServerLevel.class,
                            BlockPos.class,
                            RandomSource.class);
            probe.setAccessible(true);
            probe.invoke(
                    foamBlock, soft, helper.getLevel(), helper.absolutePos(MID), LOSING_ROLL);
            helper.assertTrue(
                    helper.getBlockState(MID).getValue(FoamCableBlock.FOAM).isSoft(),
                    "A losing roll reschedules the hardening probe");
            probe.invoke(
                    foamBlock,
                    helper.getBlockState(MID),
                    helper.getLevel(),
                    helper.absolutePos(MID),
                    WINNING_ROLL);
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException(failure);
        }
        helper.assertTrue(
                helper.getBlockState(MID).getValue(FoamCableBlock.FOAM).isHard(),
                "A winning roll cures the foam shell in place");
        helper.succeed();
    }

    private static final RandomSource WINNING_ROLL =
            new RandomSource() {
                @Override
                public RandomSource fork() {
                    return this;
                }

                @Override
                public PositionalRandomFactory forkPositional() {
                    throw new UnsupportedOperationException();
                }

                @Override
                public void setSeed(long seed) {}

                @Override
                public int nextInt() {
                    return 0;
                }

                @Override
                public int nextInt(int bound) {
                    return 0;
                }

                @Override
                public long nextLong() {
                    return 0;
                }

                @Override
                public boolean nextBoolean() {
                    return true;
                }

                @Override
                public float nextFloat() {
                    return 0.0F;
                }

                @Override
                public double nextDouble() {
                    return 0.0;
                }

                @Override
                public double nextGaussian() {
                    return 0.0;
                }
            };

    private static final RandomSource LOSING_ROLL =
            new RandomSource() {
                @Override
                public RandomSource fork() {
                    return this;
                }

                @Override
                public PositionalRandomFactory forkPositional() {
                    throw new UnsupportedOperationException();
                }

                @Override
                public void setSeed(long seed) {}

                @Override
                public int nextInt() {
                    return Integer.MAX_VALUE;
                }

                @Override
                public int nextInt(int bound) {
                    return bound - 1;
                }

                @Override
                public long nextLong() {
                    return Long.MAX_VALUE;
                }

                @Override
                public boolean nextBoolean() {
                    return false;
                }

                @Override
                public float nextFloat() {
                    return 1.0F;
                }

                @Override
                public double nextDouble() {
                    return 1.0;
                }

                @Override
                public double nextGaussian() {
                    return 0.0;
                }
            };

    static void foamFamilyRegistryMirrorsCables(GameTestHelper helper) {
        helper.assertTrue(
                ModMachines.FOAM_CABLES.size() == 12,
                "Twelve plain foam cables are registered");
        for (var entry : ModMachines.FOAM_CABLES.entrySet()) {
            var foamBlock = entry.getValue().get();
            var counterpart = ModMachines.cableCounterpart(foamBlock);
            helper.assertTrue(
                    counterpart != null,
                    "Every foam cable pairs with the plain cable of the same material and tier");
            if (counterpart != null) {
                helper.assertTrue(
                        ModMachines.foamCounterpart(counterpart.get()) == entry.getValue(),
                        "The foam pairing is symmetric in both directions");
            }
            helper.assertTrue(
                    entry.getKey().endsWith("_foam_cable"),
                    "Foam cable ids keep the legacy suffix");
        }
        helper.succeed();
    }
}
