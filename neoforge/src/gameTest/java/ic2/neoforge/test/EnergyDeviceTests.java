package ic2.neoforge.test;

import ic2.core.energy.StorageRedstoneMode;
import ic2.core.energy.TransformerMode;
import ic2.neoforge.item.ElectricItemEnergy;
import ic2.neoforge.machine.*;
import ic2.neoforge.menu.MachineMenu;
import ic2.neoforge.registration.ModItems;
import ic2.neoforge.registration.ModMachines;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.transfer.item.ItemResource;

final class EnergyDeviceTests {
    private static final BlockPos LEFT = new BlockPos(1, 2, 1),
            MID = new BlockPos(2, 2, 1),
            RIGHT = new BlockPos(3, 2, 1);

    private static void place(
            GameTestHelper helper, BlockPos pos, MachineKind kind, Direction facing) {
        helper.setBlock(
                pos,
                ModMachines.block(kind).defaultBlockState().setValue(MachineBlock.FACING, facing));
    }

    static void storageDirection(GameTestHelper helper) {
        place(helper, LEFT, MachineKind.BATBOX, Direction.EAST);
        helper.setBlock(MID, ModMachines.CABLES.get("insulated_copper_cable").get());
        place(helper, RIGHT, MachineKind.ELECTRIC_FURNACE, Direction.NORTH);
        var storage = helper.getBlockEntity(LEFT, EnergyStorageBlockEntity.class);
        var sink = helper.getBlockEntity(RIGHT, ElectricFurnaceBlockEntity.class);
        storage.energy().restore(1000);
        helper.runAtTickTime(
                5,
                () -> {
                    helper.assertTrue(
                            sink.energy().stored() > 0, "Storage must supply its marked face");
                    place(helper, LEFT, MachineKind.BATBOX, Direction.WEST);
                    sink.energy().restore(0);
                });
        helper.runAtTickTime(
                10,
                () -> {
                    helper.assertTrue(
                            sink.energy().stored() == 0,
                            "Rotating storage must invalidate its output route");
                    place(helper, LEFT, MachineKind.BATBOX, Direction.EAST);
                    storage.setMode(StorageRedstoneMode.STOP_WHEN_POWERED);
                    helper.setBlock(LEFT.below(), Blocks.REDSTONE_BLOCK);
                });
        helper.runAtTickTime(12, () -> sink.energy().restore(0));
        helper.runAtTickTime(
                16,
                () -> {
                    helper.assertTrue(
                            sink.energy().stored() == 0,
                            "Powered redstone stop must disable EU output");
                    helper.setBlock(LEFT.below(), Blocks.AIR);
                });
        helper.runAtTickTime(
                22,
                () -> {
                    helper.assertTrue(
                            sink.energy().stored() > 0, "Removing redstone must re-enable output");
                    helper.succeed();
                });
    }

    static void storageInput(GameTestHelper helper) {
        place(helper, LEFT, MachineKind.GENERATOR, Direction.NORTH);
        place(helper, MID, MachineKind.BATBOX, Direction.WEST);
        var generator = helper.getBlockEntity(LEFT, GeneratorBlockEntity.class);
        var storage = helper.getBlockEntity(MID, EnergyStorageBlockEntity.class);
        generator.energy().restore(1000);
        helper.runAtTickTime(
                5,
                () -> {
                    helper.assertTrue(
                            storage.energy().stored() == 0,
                            "Marked output face must reject incoming EU");
                    place(helper, MID, MachineKind.BATBOX, Direction.UP);
                });
        helper.runAtTickTime(
                10,
                () -> {
                    helper.assertTrue(
                            storage.energy().stored() > 0,
                            "Vertical orientation must accept EU on other faces");
                    helper.succeed();
                });
    }

    static void transformerChain(GameTestHelper helper) {
        place(helper, LEFT, MachineKind.CESU, Direction.EAST);
        place(helper, MID, MachineKind.LV_TRANSFORMER, Direction.WEST);
        place(helper, RIGHT, MachineKind.BATBOX, Direction.EAST);
        var high = helper.getBlockEntity(LEFT, EnergyStorageBlockEntity.class);
        var transformer = helper.getBlockEntity(MID, TransformerBlockEntity.class);
        var low = helper.getBlockEntity(RIGHT, EnergyStorageBlockEntity.class);
        high.energy().restore(1024);
        helper.runAtTickTime(
                8,
                () -> {
                    helper.assertTrue(
                            low.energy().stored() > 0,
                            "Step-down must power LV storage from an MV source");
                    helper.assertTrue(
                            high.energy().stored()
                                            + transformer.energy().stored()
                                            + low.energy().stored()
                                    == 1024,
                            "Direct transformation must conserve energy");
                    transformer.energy().restore(0);
                    high.energy().restore(0);
                    low.energy().restore(1024);
                    place(helper, LEFT, MachineKind.CESU, Direction.WEST);
                    place(helper, RIGHT, MachineKind.BATBOX, Direction.WEST);
                    transformer.setMode(TransformerMode.STEP_UP);
                });
        helper.runAtTickTime(
                20,
                () -> {
                    helper.assertTrue(
                            high.energy().stored() > 0 && transformer.stepUp(),
                            "Step-up must return HV packets to the high-side storage");
                    helper.assertTrue(
                            high.energy().stored()
                                            + transformer.energy().stored()
                                            + low.energy().stored()
                                    == 1024,
                            "Step-up must conserve energy");
                    helper.succeed();
                });
    }

    static void stateAndMenu(GameTestHelper helper) {
        place(helper, MID, MachineKind.MFSU, Direction.UP);
        var storage = helper.getBlockEntity(MID, EnergyStorageBlockEntity.class);
        storage.energy().restore(40000000);
        storage.setMode(StorageRedstoneMode.EMIT_NEAR_FULL);
        storage.serverTick(helper.getLevel());
        helper.assertTrue(
                storage.signal() == 15 && storage.comparator() == 15,
                "Full storage must emit redstone and comparator output");
        var restored =
                (EnergyStorageBlockEntity)
                        BlockEntity.loadStatic(
                                storage.getBlockPos(),
                                storage.getBlockState(),
                                storage.saveWithFullMetadata(helper.getLevel().registryAccess()),
                                helper.getLevel().registryAccess());
        helper.assertTrue(
                restored.energy().stored() == 40000000
                        && restored.mode() == StorageRedstoneMode.EMIT_NEAR_FULL,
                "Storage mode and full precision energy must survive reload");
        var battery = new ItemStack(ModItems.RE_BATTERY.get());
        restored.inventory().set(0, ItemResource.of(battery), 1);
        restored.serverTick(helper.getLevel());
        helper.assertTrue(
                ElectricItemEnergy.charge(restored.inventory().stack(0)) == 100
                        && restored.energy().stored() == 39999900,
                "Charging must obey item limit and preserve EU");
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(
                storage.getBlockPos().getX() + .5,
                storage.getBlockPos().getY(),
                storage.getBlockPos().getZ() + .5);
        var menu = new MachineMenu(1, player.getInventory(), storage);
        helper.assertTrue(
                !menu.clickMenuButton(player, 5), "Actions from a non-current menu must fail");
        player.containerMenu = menu;
        helper.assertTrue(
                menu.slots.size() == 38
                        && menu.clickMenuButton(player, 5)
                        && storage.mode() == StorageRedstoneMode.STOP_WHEN_POWERED
                        && !menu.clickMenuButton(player, 7),
                "Storage menu must validate mode bounds");
        var client =
                new MachineMenu(1, player.getInventory(), storage.getBlockPos(), MachineKind.MFSU);
        client.setData(0, 40000000 & 0xffff);
        client.setData(1, 40000000 >>> 16);
        helper.assertTrue(
                client.energy() == 40000000,
                "Container properties must preserve values beyond 16 bits");
        helper.succeed();
    }

    static void profiles(GameTestHelper helper) {
        for (var kind : MachineKind.values()) {
            if (!kind.transformer()) continue;
            var state = ModMachines.block(kind).defaultBlockState();
            var transformer = new TransformerBlockEntity(BlockPos.ZERO, state);
            transformer.serverTick(helper.getLevel());
            var down = transformer.energyNode();
            helper.assertTrue(
                    down.output().orElseThrow().classicPacketCount() == 4
                            && down.output().orElseThrow().classicPacketAmps() == 1
                            && down.input().orElseThrow().maxAmps() == 1,
                    "Classic transformers must emit four small packets, not one oversized packet");
            transformer.setMode(TransformerMode.STEP_UP);
            transformer.serverTick(helper.getLevel());
            var up = transformer.energyNode();
            helper.assertTrue(
                    up.output().orElseThrow().maxAmps() == 1
                            && up.input().orElseThrow().maxAmps() == 4
                            && transformer.emitsTo(Direction.NORTH)
                            && !transformer.emitsTo(Direction.UP),
                    "GT transformer profiles must swap voltage and current with face permissions");
            var restored =
                    (TransformerBlockEntity)
                            BlockEntity.loadStatic(
                                    BlockPos.ZERO,
                                    state,
                                    transformer.saveWithFullMetadata(
                                            helper.getLevel().registryAccess()),
                                    helper.getLevel().registryAccess());
            helper.assertTrue(
                    restored.mode() == TransformerMode.STEP_UP && restored.stepUp(),
                    "Transformer mode must persist without triggering a charged mode-switch fault"
                        + " on load");
            var player = helper.makeMockPlayer(GameType.SURVIVAL);
            var menu = new MachineMenu(1, player.getInventory(), restored);
            helper.assertTrue(
                    menu.slots.size() == 36, "Transformers must expose only player inventory");
        }
        helper.succeed();
    }

    private EnergyDeviceTests() {}
}
