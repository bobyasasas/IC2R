package ic2.neoforge.test;

import ic2.core.energy.grid.CableSpec;
import ic2.neoforge.energy.DetectorCableBlock;
import ic2.neoforge.energy.DetectorFoamCableBlock;
import ic2.neoforge.energy.FoamCableBlock;
import ic2.neoforge.energy.SplitterCableBlock;
import ic2.neoforge.energy.SplitterFoamCableBlock;
import ic2.neoforge.item.CutterItem;
import ic2.neoforge.item.FoamSprayerItem;
import ic2.neoforge.machine.MachineBlock;
import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.machine.PoweredBlockEntity;
import ic2.neoforge.registration.ModFoam;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.registration.ModTools;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStackTemplate;

final class DetectorCableTests {
    // Same floor layout as CableShockTests; the redstone gate sits above the cable position.
    private static final BlockPos SOURCE = new BlockPos(1, 1, 2),
            CABLE = new BlockPos(2, 1, 2),
            SINK = new BlockPos(3, 1, 2),
            GATE = new BlockPos(2, 2, 2);

    private static PoweredBlockEntity buildLine(
            GameTestHelper helper, String cableId, MachineKind sourceKind, MachineKind sinkKind) {
        helper.setBlock(
                SOURCE,
                ModMachines.block(sourceKind)
                        .defaultBlockState()
                        .setValue(MachineBlock.FACING, Direction.EAST));
        helper.setBlock(CABLE, ModMachines.CABLES.get(cableId).get());
        helper.setBlock(
                SINK,
                ModMachines.block(sinkKind)
                        .defaultBlockState()
                        .setValue(MachineBlock.FACING, Direction.EAST));
        var source = helper.getBlockEntity(SOURCE, PoweredBlockEntity.class);
        var sink = helper.getBlockEntity(SINK, PoweredBlockEntity.class);
        source.energy().restore(source.energy().capacity());
        return sink;
    }

    private static BlockState cableState(GameTestHelper helper) {
        return helper.getBlockState(CABLE);
    }

    /** Legacy comparator readout at the cable position, read from one horizontal side. */
    private static int comparator(GameTestHelper helper) {
        return cableState(helper)
                .getAnalogOutputSignal(helper.getLevel(), helper.absolutePos(CABLE), Direction.WEST);
    }

    private static int redstone(GameTestHelper helper) {
        return helper.getLevel().getSignal(helper.absolutePos(CABLE), Direction.NORTH);
    }

    /** FoamTests spray pattern: a survival mock player using a loaded sprayer on the cable. */
    private static void sprayCable(GameTestHelper helper) {
        ItemStack sprayer = new ItemStack(ModFoam.FOAM_SPRAYER.get());
        sprayer.set(
                ic2.neoforge.component.ModDataComponents.FLUID,
                new FluidStackTemplate(FoamSprayerItem.foamFluid(), 1000));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, sprayer);
        BlockPos absolute = helper.absolutePos(CABLE);
        ModFoam.FOAM_SPRAYER.get().useOn(
                new UseOnContext(
                        helper.getLevel(),
                        player,
                        InteractionHand.MAIN_HAND,
                        sprayer,
                        new BlockHitResult(
                                Vec3.atCenterOf(absolute), Direction.UP, absolute, false)));
    }

    static void detectorActivatesOnFlow(GameTestHelper helper) {
        PoweredBlockEntity sink =
                buildLine(helper, "detector_cable", MachineKind.GENERATOR, MachineKind.MFE);
        helper.runAtTickTime(
                40,
                () -> {
                    helper.assertTrue(
                            cableState(helper).getValue(DetectorCableBlock.ACTIVE),
                            "The detector latches active once packets cross it");
                    helper.assertTrue(
                            redstone(helper) == 15,
                            "An active detector emits a full weak redstone signal");
                    helper.assertTrue(
                            sink.energy().stored() > 0,
                            "The detector still conducts energy to the sink");
                    helper.succeed();
                });
    }

    static void detectorComparatorScalesWithPacket(GameTestHelper helper) {
        buildLine(helper, "detector_cable", MachineKind.MFSU, MachineKind.MFSU);
        helper.runAtTickTime(
                40,
                () -> {
                    helper.assertTrue(
                            cableState(helper).getValue(DetectorCableBlock.ACTIVE),
                            "The detector stays active under HV flow");
                    // 2048 EU packets over the 8192 capacity: (int)(2048 / 8192 * 15) == 3.
                    helper.assertTrue(
                            comparator(helper) == 3,
                            "The comparator reads three steps at 2048 EU per packet");
                    helper.assertTrue(
                            redstone(helper) == 15,
                            "The detector signal stays full under HV flow");
                    helper.succeed();
                });
    }

    static void detectorDeactivatesWhenFlowStops(GameTestHelper helper) {
        buildLine(helper, "detector_cable", MachineKind.GENERATOR, MachineKind.MFE);
        helper.runAtTickTime(
                40,
                () -> {
                    helper.assertTrue(
                            cableState(helper).getValue(DetectorCableBlock.ACTIVE),
                            "The detector is active while the source still feeds the line");
                    var source = helper.getBlockEntity(SOURCE, PoweredBlockEntity.class);
                    source.energy().extract(source.energy().stored());
                });
        helper.runAtTickTime(
                100,
                () -> {
                    helper.assertTrue(
                            !cableState(helper).getValue(DetectorCableBlock.ACTIVE),
                            "The next probe after the source runs dry latches inactive");
                    helper.assertTrue(
                            redstone(helper) == 0 && comparator(helper) == 0,
                            "An inactive detector is redstone-silent");
                    helper.succeed();
                });
    }

    static void splitterGatesUntilPowered(GameTestHelper helper) {
        PoweredBlockEntity sink =
                buildLine(helper, "splitter_cable", MachineKind.GENERATOR, MachineKind.MFE);
        helper.runAtTickTime(
                30,
                () -> {
                    helper.assertTrue(
                            !cableState(helper).getValue(SplitterCableBlock.ACTIVE),
                            "A splitter placed without signal starts inactive");
                    helper.assertTrue(
                            sink.energy().stored() == 0,
                            "The unpowered splitter keeps the sink at zero");
                    helper.setBlock(GATE, Blocks.REDSTONE_BLOCK);
                });
        helper.runAtTickTime(
                70,
                () -> {
                    helper.assertTrue(
                            cableState(helper).getValue(SplitterCableBlock.ACTIVE),
                            "The neighbour signal flips the splitter on");
                    helper.assertTrue(
                            sink.energy().stored() > 0,
                            "The powered splitter lets packets through to the sink");
                    helper.succeed();
                });
    }

    static void splitterPlacementAdoptsSignal(GameTestHelper helper) {
        helper.setBlock(GATE, Blocks.REDSTONE_BLOCK);
        ItemStack stack = new ItemStack(ModMachines.CABLES.get("splitter_cable").get());
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos absolute = helper.absolutePos(CABLE);
        ModMachines.CABLES
                .get("splitter_cable")
                .get()
                .asItem()
                .useOn(
                        new UseOnContext(
                                helper.getLevel(),
                                player,
                                InteractionHand.MAIN_HAND,
                                stack,
                                new BlockHitResult(
                                        Vec3.atCenterOf(absolute),
                                        Direction.UP,
                                        absolute,
                                        false)));
        helper.assertTrue(
                cableState(helper).getValue(SplitterCableBlock.ACTIVE),
                "Placing the splitter under signal adopts the powered state");
        helper.runAtTickTime(
                10,
                () -> {
                    helper.setBlock(GATE, Blocks.AIR);
                    helper.assertTrue(
                            !cableState(helper).getValue(SplitterCableBlock.ACTIVE),
                            "Losing the signal flips the splitter back off");
                    helper.succeed();
                });
    }

    static void foamDetectorDipsCarriesActivates(GameTestHelper helper) {
        PoweredBlockEntity sink =
                buildLine(helper, "detector_cable", MachineKind.GENERATOR, MachineKind.MFE);
        sprayCable(helper);
        helper.assertTrue(
                cableState(helper).getBlock() instanceof DetectorFoamCableBlock,
                "Spraying the detector cable swaps in its foam shell");
        helper.runAtTickTime(
                60,
                () -> {
                    BlockState state = cableState(helper);
                    helper.assertTrue(
                            state.getValue(DetectorFoamCableBlock.ACTIVE),
                            "The foam-shelled detector still probes its inflow");
                    helper.assertTrue(
                            sink.energy().stored() > 0,
                            "The foam shell still conducts energy to the sink");
                    helper.succeed();
                });
    }

    static void detectorSplitterFamilyRegistry(GameTestHelper helper) {
        var detector = ModMachines.CABLES.get("detector_cable").get();
        var detectorFoam = ModMachines.FOAM_CABLES.get("detector_foam_cable").get();
        var splitter = ModMachines.CABLES.get("splitter_cable").get();
        var splitterFoam = ModMachines.FOAM_CABLES.get("splitter_foam_cable").get();
        helper.assertTrue(
                ModMachines.cableCounterpart(detectorFoam).get() == detector
                        && ModMachines.foamCounterpart(detector).get() == detectorFoam,
                "Detector foam and plain pair in both directions");
        helper.assertTrue(
                ModMachines.cableCounterpart(splitterFoam).get() == splitter
                        && ModMachines.foamCounterpart(splitter).get() == splitterFoam,
                "Splitter foam and plain pair in both directions");
        helper.assertTrue(
                detector.material() == CableSpec.Material.DETECTOR
                        && detector.specification().voltageLimit() == 8192
                        && detector.specification().insulation() == 0,
                "The detector keeps the legacy 8192-capacity unshieldable spec");
        helper.assertTrue(
                splitter.material() == CableSpec.Material.SPLITTER
                        && splitter.specification().voltageLimit() == 8192,
                "The splitter keeps the legacy 8192-capacity spec");
        helper.assertTrue(
                detector.asItem() != Items.AIR && splitter.asItem() != Items.AIR,
                "Only the plain specials have an item form");
        helper.assertTrue(
                detectorFoam.asItem() == Items.AIR && splitterFoam.asItem() == Items.AIR,
                "The foam shells are block-only like every foam cable");

        // The cutter must not strip insulation the detector foam never had.
        helper.setBlock(CABLE, detectorFoam.defaultBlockState());
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModTools.CUTTER.get()));
        CutterItem.strip(player, helper.getLevel(), helper.absolutePos(CABLE), cableState(helper));
        helper.assertTrue(
                cableState(helper).getBlock() == detectorFoam
                        && cableState(helper).getValue(FoamCableBlock.FOAM).isSoft(),
                "The cutter leaves the zero-insulation detector foam untouched");
        helper.succeed();
    }
}
