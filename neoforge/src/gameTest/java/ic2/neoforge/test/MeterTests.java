package ic2.neoforge.test;

import ic2.neoforge.energy.WorldEnergyNetworks;
import ic2.neoforge.item.MeterItem;
import ic2.neoforge.machine.MachineBlock;
import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.machine.PoweredBlockEntity;
import ic2.neoforge.menu.MeterMenu;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.registration.ModTools;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Legacy ContainerMeter/EU-Reader reading the per-node flow statistics of the grid. */
final class MeterTests {
    private static final BlockPos SOURCE = new BlockPos(1, 1, 2),
            CABLE = new BlockPos(2, 1, 2),
            SINK = new BlockPos(3, 1, 2);

    private static ServerLevel level(GameTestHelper helper) {
        return (ServerLevel) helper.getLevel();
    }

    private static void buildLine(GameTestHelper helper) {
        helper.setBlock(
                SOURCE,
                ModMachines.block(MachineKind.GENERATOR)
                        .defaultBlockState()
                        .setValue(MachineBlock.FACING, Direction.EAST));
        helper.setBlock(CABLE, ModMachines.CABLES.get("insulated_copper_cable").get());
        helper.setBlock(
                SINK,
                ModMachines.block(MachineKind.MFE)
                        .defaultBlockState()
                        .setValue(MachineBlock.FACING, Direction.EAST));
        var source = helper.getBlockEntity(SOURCE, PoweredBlockEntity.class);
        source.energy().restore(source.energy().capacity());
    }

    static void nodeStatsTrackFlowAcrossTheLine(GameTestHelper helper) {
        buildLine(helper);
        helper.runAtTickTime(
                40,
                () -> {
                    var source = WorldEnergyNetworks.nodeStats(level(helper), helper.absolutePos(SOURCE));
                    var cable = WorldEnergyNetworks.nodeStats(level(helper), helper.absolutePos(CABLE));
                    var sink = WorldEnergyNetworks.nodeStats(level(helper), helper.absolutePos(SINK));
                    helper.assertTrue(
                            source != null && cable != null && sink != null,
                            "Every node in a live grid carries flow statistics");
                    helper.assertTrue(
                            source.energyIn() == 0 && source.energyOut() > 0,
                            "The generator records outflow only");
                    helper.assertTrue(
                            cable.energyIn() > 0 && cable.energyOut() == cable.energyIn(),
                            "A conductor mirrors its inflow as outflow");
                    helper.assertTrue(
                            cable.voltage() > 0 && cable.amperage() >= 1,
                            "The conductor sees the packet power and packet count");
                    helper.assertTrue(
                            sink.energyIn() > 0 && sink.energyOut() == 0,
                            "The sink records inflow only");
                    helper.assertTrue(
                            WorldEnergyNetworks.conductorEnergyIn(
                                    level(helper), helper.absolutePos(CABLE))
                                    == cable.energyIn(),
                            "The detector accessor shares the per-node statistics");
                    helper.succeed();
                });
    }

    static void idleNodesStillReportZeroFlow(GameTestHelper helper) {
        buildLine(helper);
        var source = helper.getBlockEntity(SOURCE, PoweredBlockEntity.class);
        source.energy().extract(source.energy().stored());
        helper.runAtTickTime(
                40,
                () -> {
                    var cable = WorldEnergyNetworks.nodeStats(level(helper), helper.absolutePos(CABLE));
                    helper.assertTrue(
                            cable != null, "An idle grid node still exists in the statistics");
                    helper.assertTrue(
                            cable.energyIn() == 0 && cable.amperage() == 0,
                            "An idle node reads as zero flow");
                    helper.succeed();
                });
    }

    static void menuSamplesModesAndResets(GameTestHelper helper) {
        buildLine(helper);
        helper.runAtTickTime(
                30,
                () -> {
                    Player player = helper.makeMockPlayer(GameType.SURVIVAL);
                    var menu = new MeterMenu(1, player.getInventory(), helper.absolutePos(CABLE));
                    player.containerMenu = menu;
                    menu.broadcastChanges();
                    helper.assertTrue(
                            menu.resultCount() == 1, "One broadcast samples one cycle");
                    helper.assertTrue(
                            menu.resultAvg() == menu.resultMax()
                                    && menu.resultMax() == menu.resultMin(),
                            "The first sample pins avg, min and max together");
                    double expected =
                            WorldEnergyNetworks.nodeStats(level(helper), helper.absolutePos(CABLE))
                                    .energyIn();
                    helper.assertTrue(
                            Math.abs(menu.resultAvg() - expected) < 0.001,
                            "EnergyIn mode reads the node inflow scaled by 1000");
                    helper.assertTrue(
                            menu.clickMenuButton(player, MeterMenu.Mode.AMPERAGE.ordinal()),
                            "The mode buttons switch through clickMenuButton");
                    helper.assertTrue(
                            menu.mode() == MeterMenu.Mode.AMPERAGE && menu.resultCount() == 0,
                            "Switching the mode resets the counters");
                    menu.broadcastChanges();
                    helper.assertTrue(
                            menu.resultAvg() >= 1, "Amperage mode reads the packet count");
                    helper.assertTrue(
                            menu.clickMenuButton(player, MeterMenu.RESET_BUTTON)
                                    && menu.resultCount() == 0
                                    && menu.resultAvg() == 0
                                    && menu.resultMin() == 0
                                    && menu.resultMax() == 0,
                            "The reset button zeroes every counter");
                    helper.succeed();
                });
    }

    static void menuClosesWhenTargetLeavesTheGrid(GameTestHelper helper) {
        buildLine(helper);
        Player[] holder = new Player[1];
        helper.runAtTickTime(
                30,
                () -> {
                    Player player = helper.makeMockPlayer(GameType.SURVIVAL);
                    holder[0] = player;
                    var menu = new MeterMenu(1, player.getInventory(), helper.absolutePos(CABLE));
                    player.containerMenu = menu;
                    menu.broadcastChanges();
                    helper.assertTrue(
                            player.containerMenu == menu,
                            "An in-grid target keeps the meter open");
                    helper.setBlock(CABLE, Blocks.AIR);
                });
        helper.runAtTickTime(
                70,
                () -> {
                    var menu = holder[0].containerMenu;
                    if (menu instanceof MeterMenu meter) meter.broadcastChanges();
                    helper.assertTrue(
                            holder[0].containerMenu != menu,
                            "A target that left the grid closes the meter like the legacy null check");
                    helper.assertTrue(
                            WorldEnergyNetworks.nodeStats(level(helper), helper.absolutePos(CABLE))
                                    == null,
                            "The removed cable no longer reports statistics");
                    helper.succeed();
                });
    }

    static void itemUsageDistinguishesEnergyNodes(GameTestHelper helper) {
        buildLine(helper);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack meter = new ItemStack(ModTools.METER.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, meter);
        var machine = helper.absolutePos(SOURCE);
        var result =
                ModTools.METER
                        .get()
                        .onItemUseFirst(
                                meter,
                                new UseOnContext(
                                        player,
                                        InteractionHand.MAIN_HAND,
                                        new BlockHitResult(
                                                Vec3.atCenterOf(machine),
                                                Direction.UP,
                                                machine,
                                                false)));
        helper.assertTrue(
                result == InteractionResult.SUCCESS,
                "Using the meter on a machine target is accepted");
        helper.assertTrue(
                MeterItem.isEnergyNode(helper.getLevel(), helper.absolutePos(CABLE)),
                "Cables count as energy net nodes");
        helper.setBlock(new BlockPos(1, 2, 2), Blocks.STONE);
        helper.assertTrue(
                !MeterItem.isEnergyNode(helper.getLevel(), helper.absolutePos(new BlockPos(1, 2, 2))),
                "Plain blocks are not energy net nodes");
        var stone = helper.absolutePos(new BlockPos(1, 2, 2));
        var rejected =
                ModTools.METER
                        .get()
                        .onItemUseFirst(
                                meter,
                                new UseOnContext(
                                        player,
                                        InteractionHand.MAIN_HAND,
                                        new BlockHitResult(
                                                Vec3.atCenterOf(stone),
                                                Direction.UP,
                                                stone,
                                                false)));
        helper.assertTrue(
                rejected == InteractionResult.SUCCESS,
                "A non-node target still consumes the use with the legacy message");
        helper.succeed();
    }
}
