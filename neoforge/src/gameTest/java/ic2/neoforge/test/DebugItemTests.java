package ic2.neoforge.test;

import ic2.neoforge.item.ElectricItemEnergy;
import ic2.neoforge.crop.CropBlockEntity;
import ic2.neoforge.energy.WorldEnergyNetworks;
import ic2.neoforge.item.DebugItem;
import ic2.neoforge.machine.MachineBlock;
import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.registration.ModCrops;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.registration.ModTools;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Legacy ItemDebug: mode cycling, tile data, energy net dumps, reflection dumps, acceleration. */
final class DebugItemTests {
    // Shared layout on the world_room plateau around the (8, 8, 8) centre.
    private static final BlockPos MACHINE = new BlockPos(6, 8, 8),
            SAFE = new BlockPos(10, 8, 8),
            CROP = new BlockPos(8, 8, 6),
            ICE = new BlockPos(6, 8, 6),
            PLAIN = new BlockPos(8, 8, 10),
            SOURCE = new BlockPos(4, 8, 10),
            CABLE = new BlockPos(5, 8, 10),
            SINK = new BlockPos(6, 8, 10);

    private static ServerLevel level(GameTestHelper helper) {
        return (ServerLevel) helper.getLevel();
    }

    private static List<String> chats = new ArrayList<>();

    private static DebugItem.Output chatOutput() {
        chats = new ArrayList<>();
        return new DebugItem.Output(line -> {}, chats::add);
    }

    private static String chatText() {
        return String.join("\n", chats);
    }

    static void modesCycleOnSneakUse(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack debug = new ItemStack(ModTools.DEBUG_ITEM.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, debug);
        helper.assertTrue(
                DebugItem.mode(debug) == DebugItem.Mode.InterfacesFields,
                "The debug item starts in the interfaces mode");
        helper.assertTrue(
                ModTools.DEBUG_ITEM
                        .get()
                        .use(helper.getLevel(), player, InteractionHand.MAIN_HAND)
                        == InteractionResult.PASS,
                "Using the item without sneaking keeps the mode");
        player.setShiftKeyDown(true);
        for (int i = 1; i < DebugItem.Mode.modes.length; i++)
            ModTools.DEBUG_ITEM.get().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(
                DebugItem.mode(debug) == DebugItem.Mode.AccelerateX100,
                "Five cycles land on the last mode");
        ModTools.DEBUG_ITEM.get().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(
                DebugItem.mode(debug) == DebugItem.Mode.InterfacesFields,
                "Cycling wraps around to the first mode");
        helper.setBlock(PLAIN, Blocks.STONE);
        var before = DebugItem.mode(debug);
        var result =
                ModTools.DEBUG_ITEM
                        .get()
                        .onItemUseFirst(
                                debug,
                                new UseOnContext(
                                        player,
                                        InteractionHand.MAIN_HAND,
                                        new BlockHitResult(
                                                Vec3.atCenterOf(helper.absolutePos(PLAIN)),
                                                Direction.UP,
                                                helper.absolutePos(PLAIN),
                                                false)));
        helper.assertTrue(
                result == InteractionResult.SUCCESS,
                "Sneaking onto a block cycles the mode as well");
        helper.assertTrue(DebugItem.mode(debug) != before, "The sneak use moved the mode");
        helper.succeed();
    }

    static void tileDataReportsMachinesCropsAndSafes(GameTestHelper helper) {
        helper.setBlock(
                MACHINE,
                ModMachines.block(MachineKind.MFE)
                        .defaultBlockState()
                        .setValue(MachineBlock.FACING, Direction.EAST));
        helper.setBlock(SAFE, ModMachines.block(MachineKind.PERSONAL_CHEST).defaultBlockState());
        helper.setBlock(CROP.below(), Blocks.FARMLAND);
        helper.setBlock(CROP.above().above(), Blocks.GLOWSTONE);
        helper.setBlock(CROP, ModCrops.CROP_STICK.get().defaultBlockState());
        var crop = helper.getBlockEntity(CROP, CropBlockEntity.class);
        helper.assertTrue(
                crop.tryPlantIn(ModCrops.WHEAT_CARD, 0, 2, 2, 2, 4),
                "Wheat plants into the crop stick");
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        var output = chatOutput();
        DebugItem.dumpTileData(helper.getLevel(), helper.absolutePos(MACHINE), player, output);
        output.flush();
        String machine = chatText();
        helper.assertTrue(
                machine.contains("Block: Active=false Facing=east"),
                "Tile data reports the machine active flag and facing: " + machine);
        helper.assertTrue(
                machine.contains("Energy: 0.00 / "),
                "Tile data reports the machine energy budget: " + machine);
        DebugItem.dumpTileData(helper.getLevel(), helper.absolutePos(SAFE), player, output);
        output.flush();
        helper.assertTrue(
                chatText().contains("PersonalBlock: CanAccess=true"),
                "Tile data reports an unowned personal chest as accessible: " + chatText());
        DebugItem.dumpTileData(helper.getLevel(), helper.absolutePos(CROP), player, output);
        output.flush();
        String cropText = chatText();
        helper.assertTrue(
                cropText.contains("Crop=wheat"),
                "Tile data names the planted crop card: " + cropText);
        helper.assertTrue(
                cropText.contains("Nutrients=0") && cropText.contains("Growth=2"),
                "Tile data reports the stored crop stats: " + cropText);
        helper.succeed();
    }

    static void energyNetDumpsTheGrid(GameTestHelper helper) {
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
        var source = helper.getBlockEntity(SOURCE, ic2.neoforge.machine.PoweredBlockEntity.class);
        source.energy().restore(source.energy().capacity());
        helper.runAtTickTime(
                30,
                () -> {
                    List<String> console = new ArrayList<>();
                    List<String> chat = new ArrayList<>();
                    helper.assertTrue(
                            WorldEnergyNetworks.dumpDebugInfo(
                                    level(helper),
                                    helper.absolutePos(CABLE),
                                    console::add,
                                    chat::add),
                            "A cable position dumps its energy network");
                    String text = String.join("\n", chat);
                    helper.assertTrue(
                            text.contains("role: conductor"),
                            "The dump describes the conductor role: " + text);
                    helper.assertTrue(
                            text.contains("machines: 2"),
                            "The dump counts both grid machines: " + text);
                    helper.assertTrue(
                            text.contains("last packets:"),
                            "The dump includes the latest distribution statistics: " + text);
                    helper.assertTrue(
                            console.equals(chat),
                            "The console channel mirrors the chat channel");
                    List<String> plainConsole = new ArrayList<>();
                    List<String> plainChat = new ArrayList<>();
                    helper.setBlock(PLAIN, Blocks.STONE);
                    helper.assertTrue(
                            !WorldEnergyNetworks.dumpDebugInfo(
                                    level(helper),
                                    helper.absolutePos(PLAIN),
                                    plainConsole::add,
                                    plainChat::add),
                            "A plain block has no energy network to dump");
                    helper.assertTrue(
                            plainChat.isEmpty(),
                            "A failed dump writes nothing to the sinks");
                    helper.succeed();
                });
    }

    static void reflectionDumpsFields(GameTestHelper helper) {
        helper.setBlock(MACHINE, ModMachines.block(MachineKind.GENERATOR).defaultBlockState());
        List<String> console = new ArrayList<>();
        List<String> chat = new ArrayList<>();
        DebugItem.Output output = new DebugItem.Output(console::add, chat::add);
        DebugItem.dumpInterfacesFields(helper.getLevel(), helper.absolutePos(MACHINE), output);
        output.flush();
        String consoleText = String.join("\n", console);
        String chatText = String.join("\n", chat);
        helper.assertTrue(
                chatText.contains("[server] block state:")
                        && chatText.contains("name:"),
                "The block state summary reaches the chat channel: " + chatText);
        helper.assertTrue(
                consoleText.contains("interfaces:"),
                "The tile entity interfaces reach the console channel");
        helper.assertTrue(
                consoleText.contains("block fields:") && consoleText.contains("tile entity fields:"),
                "Both field sections are dumped: " + consoleText);
        helper.assertTrue(
                consoleText.contains(" type: "),
                "Reflected fields are dumped with their types");
        helper.assertTrue(
                consoleText.split("\n").length > 10,
                "The reflection dump carries real content, saw "
                        + consoleText.split("\n").length
                        + " lines");
        helper.succeed();
    }

    static void accelerateForcesTicks(GameTestHelper helper) {
        helper.setBlock(ICE, Blocks.ICE.defaultBlockState());
        helper.setBlock(ICE.above(), Blocks.GLOWSTONE);
        helper.setBlock(MACHINE, ModMachines.block(MachineKind.MFE).defaultBlockState());
        helper.setBlock(PLAIN, Blocks.STONE);
        var output = chatOutput();
        helper.assertTrue(
                !DebugItem.accelerate(
                        helper.getLevel(), helper.absolutePos(PLAIN), 100, output),
                "A block with no ticker and no random ticks refuses acceleration");
        output.flush();
        boolean machineRan =
                DebugItem.accelerate(helper.getLevel(), helper.absolutePos(MACHINE), 1000, output);
        output.flush();
        helper.assertTrue(
                machineRan,
                "The machine ticker runs under acceleration");
        helper.assertTrue(
                chatText().contains("Running 1000 ticks"),
                "The acceleration announces its work (chats=" + chats.size() + "): " + chatText());
        // Block light needs a tick to propagate before the ice melt check can pass.
        helper.runAtTickTime(
                20,
                () -> {
                    var lightOutput = chatOutput();
                    helper.assertTrue(
                            DebugItem.accelerate(
                                    helper.getLevel(), helper.absolutePos(ICE), 1000, lightOutput),
                            "A randomly ticking block accelerates too");
                    lightOutput.flush();
                    helper.assertTrue(
                            chatText().contains("before a state change"),
                            "The random tick run reports the state change: " + chatText());
                    helper.assertTrue(
                            helper.getBlockState(ICE).is(Blocks.WATER),
                            "The accelerated ice melted under the glowstone");
                    helper.succeed();
                });
    }

    static void infiniteBudgetAndRetraceDumps(GameTestHelper helper) {
        ItemStack debug = new ItemStack(ModTools.DEBUG_ITEM.get());
        double capacity = DebugItem.INFINITE_SPEC.capacity();
        helper.assertTrue(
                ElectricItemEnergy.charge(debug) == capacity,
                "Fresh debug items ship with a full budget");
        helper.assertTrue(
                ElectricItemEnergy.use(debug, 1.0e12, null),
                "The huge finite budget pays any reasonable operation");
        helper.assertTrue(
                ElectricItemEnergy.charge(debug) == capacity - 1.0e12,
                "Uses debit the stored budget like any electric item");
        double pulled = ElectricItemEnergy.discharge(debug, 1.0e15, 0, false, true, true);
        helper.assertTrue(
                pulled == 1.0e15,
                "Machines may draw from the budget externally despite the transfer limit");
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        var chicken = helper.spawn(EntityType.CHICKEN, PLAIN);
        var pass =
                ModTools.DEBUG_ITEM
                        .get()
                        .interactLivingEntity(debug, player, chicken, InteractionHand.MAIN_HAND);
        helper.assertTrue(
                pass == InteractionResult.PASS,
                "Only the retrace mode dumps entities");
        DebugItem.setMode(debug, DebugItem.Mode.InterfacesFieldsRetrace);
        var success =
                ModTools.DEBUG_ITEM
                        .get()
                        .interactLivingEntity(debug, player, chicken, InteractionHand.MAIN_HAND);
        helper.assertTrue(
                success == InteractionResult.SUCCESS,
                "Retrace mode dumps the entity and consumes the interaction");
        helper.succeed();
    }
}
