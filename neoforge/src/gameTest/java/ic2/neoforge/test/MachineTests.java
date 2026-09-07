package ic2.neoforge.test;

import ic2.neoforge.item.ElectricItemEnergy;
import ic2.neoforge.machine.*;
import ic2.neoforge.menu.MachineMenu;
import ic2.neoforge.registration.ModItems;
import ic2.neoforge.registration.ModMachines;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

final class MachineTests {
    private static final BlockPos SOURCE = new BlockPos(1, 1, 1),
            WIRE = new BlockPos(2, 1, 1),
            SINK = new BlockPos(3, 1, 1);

    static void chain(GameTestHelper helper) {
        helper.setBlock(SOURCE, ModMachines.GENERATOR.get());
        helper.setBlock(WIRE, ModMachines.CABLES.get("insulated_copper_cable").get());
        helper.setBlock(SINK, ModMachines.ELECTRIC_FURNACE.get());
        var source = helper.getBlockEntity(SOURCE, GeneratorBlockEntity.class);
        var sink = helper.getBlockEntity(SINK, ElectricFurnaceBlockEntity.class);
        source.inventory().set(0, ItemResource.of(Items.COAL), 1);
        sink.inventory().set(0, ItemResource.of(Items.RAW_IRON), 1);
        helper.succeedWhen(
                () -> {
                    helper.assertTrue(
                            sink.inventory().stack(1).is(Items.IRON_INGOT),
                            "Wire must deliver generator energy to smelt raw iron");
                    helper.assertTrue(
                            sink.inventory().stack(0).isEmpty(),
                            "Smelting must consume exactly one input");
                    helper.assertTrue(
                            source.inventory().stack(0).isEmpty(), "Generation must consume fuel");
                });
    }

    static void reconnect(GameTestHelper helper) {
        helper.setBlock(SOURCE, ModMachines.GENERATOR.get());
        helper.setBlock(SINK, ModMachines.ELECTRIC_FURNACE.get());
        var source = helper.getBlockEntity(SOURCE, GeneratorBlockEntity.class);
        var sink = helper.getBlockEntity(SINK, ElectricFurnaceBlockEntity.class);
        source.energy().insert(1000);
        helper.runAtTickTime(
                5,
                () -> {
                    helper.assertTrue(
                            sink.energy().stored() == 0, "Disconnected machines must not transfer");
                    helper.setBlock(WIRE, ModMachines.CABLES.get("copper_cable").get());
                });
        helper.runAtTickTime(
                10,
                () -> {
                    helper.assertTrue(
                            sink.energy().stored() > 0, "Adding a wire must rebuild topology");
                    helper.setBlock(WIRE, Blocks.AIR);
                });
        helper.runAtTickTime(
                12,
                () -> {
                    sink.energy().restore(0);
                });
        helper.runAtTickTime(
                17,
                () -> {
                    helper.assertTrue(
                            sink.energy().stored() == 0,
                            "Removing a wire must invalidate cached routes");
                    helper.setBlock(WIRE, ModMachines.CABLES.get("copper_cable").get());
                });
        helper.runAtTickTime(
                22,
                () -> {
                    helper.assertTrue(
                            sink.energy().stored() > 0, "Reconnected wire must resume transfer");
                    helper.succeed();
                });
    }

    static void furnacePersistence(GameTestHelper helper) {
        helper.setBlock(SINK, ModMachines.ELECTRIC_FURNACE.get());
        var sink = helper.getBlockEntity(SINK, ElectricFurnaceBlockEntity.class);
        sink.energy().insert(300);
        sink.inventory().set(0, ItemResource.of(Items.RAW_IRON), 1);
        // Drive a detached instance to test the disk boundary without concurrent world ticks.
        var loaded =
                (ElectricFurnaceBlockEntity)
                        BlockEntity.loadStatic(
                                sink.getBlockPos(),
                                sink.getBlockState(),
                                sink.saveWithFullMetadata(helper.getLevel().registryAccess()),
                                helper.getLevel().registryAccess());
        helper.assertTrue(loaded != null, "Block entity must decode its registered type");
        for (int tick = 0; tick < 40; tick++) loaded.serverTick(helper.getLevel());
        helper.assertTrue(
                loaded.progress() == 40 && loaded.energy().stored() == 180,
                "Forty furnace ticks must consume 120 EU");
        var restored =
                (ElectricFurnaceBlockEntity)
                        BlockEntity.loadStatic(
                                loaded.getBlockPos(),
                                loaded.getBlockState(),
                                loaded.saveWithFullMetadata(helper.getLevel().registryAccess()),
                                helper.getLevel().registryAccess());
        helper.assertTrue(
                restored.progress() == 40 && restored.energy().stored() == 180,
                "Energy and progress must survive disk reload");
        restored.inventory().set(1, ItemResource.of(Items.COBBLESTONE), 64);
        restored.serverTick(helper.getLevel());
        helper.assertTrue(
                restored.progress() == 40
                        && restored.energy().stored() == 180
                        && restored.inventory().stack(0).getCount() == 1,
                "Blocked output must pause without consuming input or energy");
        restored.inventory().set(1, ItemResource.EMPTY, 0);
        for (int tick = 0; tick < 60; tick++) restored.serverTick(helper.getLevel());
        helper.assertTrue(
                restored.inventory().stack(1).is(Items.IRON_INGOT)
                        && restored.energy().stored() == 0,
                "Reloaded furnace must finish once with exactly 300 EU total");
        helper.succeed();
    }

    static void generatorPersistence(GameTestHelper helper) {
        helper.setBlock(SOURCE, ModMachines.GENERATOR.get());
        var source = helper.getBlockEntity(SOURCE, GeneratorBlockEntity.class);
        source.inventory().set(0, ItemResource.of(Items.LAVA_BUCKET), 1);
        source.serverTick(helper.getLevel());
        helper.assertTrue(
                source.inventory().stack(0).is(Items.BUCKET), "Fuel container must be retained");
        var loaded =
                (GeneratorBlockEntity)
                        BlockEntity.loadStatic(
                                source.getBlockPos(),
                                source.getBlockState(),
                                source.saveWithFullMetadata(helper.getLevel().registryAccess()),
                                helper.getLevel().registryAccess());
        helper.assertTrue(
                loaded != null
                        && loaded.progress() == source.progress()
                        && loaded.energy().stored() == 10,
                "Generator fuel and energy must survive reload");
        loaded.energy().restore(4000);
        int before = loaded.progress();
        loaded.serverTick(helper.getLevel());
        helper.assertTrue(
                loaded.progress() == before - 1 && loaded.energy().stored() == 4000,
                "Burning continues with a full buffer, matching legacy behavior");
        helper.succeed();
    }

    static void batteryAndMenu(GameTestHelper helper) {
        helper.setBlock(SINK, ModMachines.ELECTRIC_FURNACE.get());
        var sink = helper.getBlockEntity(SINK, ElectricFurnaceBlockEntity.class);
        var battery = new ItemStack(ModItems.RE_BATTERY.get());
        ElectricItemEnergy.charge(battery, 300, 1, true, false);
        sink.inventory().set(2, ItemResource.of(battery), 1);
        sink.serverTick(helper.getLevel());
        helper.assertTrue(
                sink.energy().stored() == 100
                        && ElectricItemEnergy.charge(sink.inventory().stack(2)) == 200,
                "Battery transfer must conserve charge and obey transfer limit");
        var player = helper.makeMockPlayer(GameType.CREATIVE);
        player.setPos(
                sink.getBlockPos().getX() + .5,
                sink.getBlockPos().getY(),
                sink.getBlockPos().getZ() + .5);
        var menu = new MachineMenu(1, player.getInventory(), sink);
        helper.assertTrue(menu.stillValid(player), "Nearby owner may interact");
        sink.inventory().set(1, ItemResource.of(Items.IRON_INGOT), 8);
        menu.quickMoveStack(player, 1);
        helper.assertTrue(
                sink.inventory().stack(1).isEmpty(),
                "Shift extraction must update copy-backed slots");
        int total = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++)
            if (player.getInventory().getItem(i).is(Items.IRON_INGOT))
                total += player.getInventory().getItem(i).getCount();
        helper.assertTrue(total == 8, "Shift extraction must neither duplicate nor lose items");
        try (var transaction = Transaction.openRoot()) {
            helper.assertTrue(
                    sink.automation(Direction.UP)
                                    .insert(1, ItemResource.of(Items.DIAMOND), 1, transaction)
                            == 0,
                    "Automation cannot insert into output");
            helper.assertTrue(
                    sink.automation(Direction.DOWN)
                                    .extract(
                                            2,
                                            ItemResource.of(sink.inventory().stack(2)),
                                            1,
                                            transaction)
                            == 0,
                    "Automation cannot steal the installed battery");
        }
        player.setPos(player.getX() + 20, player.getY(), player.getZ());
        helper.assertTrue(!menu.stillValid(player), "Distant interactions must be rejected");
        helper.succeed();
    }

    static void ironFurnace(GameTestHelper helper) {
        helper.setBlock(SINK, ModMachines.block(MachineKind.IRON_FURNACE));
        var machine = helper.getBlockEntity(SINK, IronFurnaceBlockEntity.class);
        machine.inventory().set(0, ItemResource.of(Items.RAW_IRON), 2);
        machine.inventory().set(2, ItemResource.of(Items.COAL), 1);
        var detached =
                (IronFurnaceBlockEntity)
                        BlockEntity.loadStatic(
                                machine.getBlockPos(),
                                machine.getBlockState(),
                                machine.saveWithFullMetadata(helper.getLevel().registryAccess()),
                                helper.getLevel().registryAccess());
        for (int tick = 0; tick < 80; tick++) detached.serverTick(helper.getLevel());
        helper.assertTrue(
                detached.progress() == 80 && detached.fuelRemaining() == 1520,
                "Iron furnace must use vanilla fuel duration and 160-tick processing");
        var restored =
                (IronFurnaceBlockEntity)
                        BlockEntity.loadStatic(
                                detached.getBlockPos(),
                                detached.getBlockState(),
                                detached.saveWithFullMetadata(helper.getLevel().registryAccess()),
                                helper.getLevel().registryAccess());
        for (int tick = 0; tick < 80; tick++) restored.serverTick(helper.getLevel());
        helper.assertTrue(
                restored.inventory().stack(1).is(Items.IRON_INGOT)
                        && restored.inventory().stack(0).getCount() == 1,
                "Iron furnace must resume saved processing without loss or duplication");
        var player = helper.makeMockPlayer(GameType.CREATIVE);
        player.getInventory().setItem(0, new ItemStack(Items.COAL, 8));
        var menu = new MachineMenu(1, player.getInventory(), machine);
        menu.quickMoveStack(player, machine.inventory().size() + 27);
        helper.assertTrue(
                machine.inventory().stack(2).getCount() == 9
                        && machine.inventory().stack(0).getCount() == 2,
                "Shift-click fuel must target fuel slot, not recipe input");
        helper.succeed();
    }

    private MachineTests() {}
}
