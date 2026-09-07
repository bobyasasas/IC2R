package ic2.neoforge.test;

import ic2.neoforge.api.WorkCapabilities;
import ic2.neoforge.machine.ElectricWorkBlockEntity;
import ic2.neoforge.machine.EnergyStorageBlockEntity;
import ic2.neoforge.machine.MachineBlock;
import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.machine.WorkConversionBlockEntity;
import ic2.neoforge.menu.MachineMenu;
import ic2.neoforge.registration.MaterialDefinition;
import ic2.neoforge.registration.ModItems;
import ic2.neoforge.registration.ModMachines;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

final class WorkEnergyTests {
    static void heatTransactions(GameTestHelper helper) {
        var pos = new BlockPos(2, 1, 2);
        var machine = source(helper, pos, MachineKind.ELECTRIC_HEAT_GENERATOR);
        machine.serverTick(helper.getLevel());
        var output =
                helper.getLevel()
                        .getCapability(
                                WorkCapabilities.HEAT, helper.absolutePos(pos), Direction.EAST);
        helper.assertTrue(
                output != null && output.available() == 10,
                "One coil supplies 10 HU through its facing capability");
        try (var transaction = Transaction.openRoot()) {
            helper.assertTrue(
                    output.extract(7, transaction) == 7,
                    "Heat extraction is simulated in a transaction");
        }
        helper.assertTrue(
                output.available() == 10 && machine.energy().stored() == 990,
                "Rollback restores both reservoir and extraction budget");
        try (var transaction = Transaction.openRoot()) {
            output.extract(6, transaction);
            transaction.commit();
        }
        machine.serverTick(helper.getLevel());
        try (var transaction = Transaction.openRoot()) {
            helper.assertTrue(
                    output.extract(100, transaction) == 4,
                    "Refilling does not reset the same-tick output budget");
            transaction.commit();
        }
        helper.setBlock(pos, machine.getBlockState().setValue(MachineBlock.FACING, Direction.WEST));
        helper.assertTrue(
                output.available() == 0,
                "A previously obtained capability must respect changed facing");
        helper.succeed();
    }

    static void kineticPersistence(GameTestHelper helper) {
        var machine = source(helper, new BlockPos(2, 1, 2), MachineKind.ELECTRIC_KINETIC_GENERATOR);
        machine.serverTick(helper.getLevel());
        helper.assertTrue(
                machine.energy().stored() == 750 && machine.progress() == 1000,
                "Electric kinetic conversion is four KU per EU");
        try (var transaction = Transaction.openRoot()) {
            helper.assertTrue(
                    machine.output(Direction.EAST).extract(1000, transaction) == 100,
                    "One motor limits total extraction to 100 KU per tick");
            transaction.commit();
        }
        var restored =
                (ElectricWorkBlockEntity)
                        BlockEntity.loadStatic(
                                machine.getBlockPos(),
                                machine.getBlockState(),
                                machine.saveWithFullMetadata(helper.getLevel().registryAccess()),
                                helper.getLevel().registryAccess());
        restored.setLevel(helper.getLevel());
        helper.assertTrue(
                restored.progress() == 900
                        && restored.energy().stored() == 750
                        && restored.output(Direction.EAST).available() == 0,
                "Reload must preserve KU, EU and the already consumed tick budget");
        helper.assertTrue(
                !restored.acceptsFrom(Direction.EAST) && restored.acceptsFrom(Direction.WEST),
                "Kinetic output face cannot accept EU");
        helper.succeed();
    }

    static void installedPartsMenu(GameTestHelper helper) {
        var machine = source(helper, new BlockPos(2, 1, 2), MachineKind.ELECTRIC_HEAT_GENERATOR);
        machine.inventory().set(0, ItemResource.EMPTY, 0);
        var coil = ModItems.MATERIALS.get(MaterialDefinition.COIL).get();
        try (var transaction = Transaction.openRoot()) {
            helper.assertTrue(
                    machine.inventory().insert(0, ItemResource.of(coil), 64, transaction) == 1,
                    "Installed-part capacity is enforced by the native inventory");
        }
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var menu = new MachineMenu(1, player.getInventory(), machine);
        player.getInventory().setItem(9, new net.minecraft.world.item.ItemStack(coil, 10));
        menu.quickMoveStack(player, 11);
        for (int slot = 0; slot < 10; slot++)
            helper.assertTrue(
                    machine.inventory().stack(slot).is(coil)
                            && machine.inventory().stack(slot).getCount() == 1,
                    "Shift-click distributes one coil into each of ten installed-part slots");
        helper.assertTrue(
                player.getInventory().getItem(9).isEmpty(), "All ten coils were installed");
        helper.succeed();
    }

    static void heatChain(GameTestHelper helper) {
        chain(helper, false);
    }

    static void kineticChain(GameTestHelper helper) {
        chain(helper, true);
    }

    private static void chain(GameTestHelper helper, boolean kinetic) {
        var pos = new BlockPos(2, 1, 2);
        var machine =
                source(
                        helper,
                        pos,
                        kinetic
                                ? MachineKind.ELECTRIC_KINETIC_GENERATOR
                                : MachineKind.ELECTRIC_HEAT_GENERATOR);
        helper.setBlock(
                pos.east(),
                ModMachines.block(
                                kinetic
                                        ? MachineKind.KINETIC_GENERATOR
                                        : MachineKind.STIRLING_GENERATOR)
                        .defaultBlockState()
                        .setValue(MachineBlock.FACING, Direction.WEST));
        helper.setBlock(pos.east(2), ModMachines.block(MachineKind.BATBOX));
        var converter = helper.getBlockEntity(pos.east(), WorkConversionBlockEntity.class);
        var battery = helper.getBlockEntity(pos.east(2), EnergyStorageBlockEntity.class);
        helper.startSequence()
                .thenWaitUntil(
                        () ->
                                helper.assertTrue(
                                        battery.energy().stored() >= 64,
                                        "Work conversion must power a real BatBox in both network"
                                            + " modes"))
                .thenExecute(
                        () -> {
                            double accounted =
                                    machine.energy().stored()
                                            + machine.progress() / (kinetic ? 4.0 : 1.0)
                                            + (converter.energy().stored()
                                                            + battery.energy().stored())
                                                    / (kinetic ? 1.0 : .5);
                            helper.assertTrue(
                                    Math.abs(accounted - 1000) < .000001,
                                    "Source EU, work reservoir, converter EU and receiver EU"
                                        + " conserve the original input");
                            var restored =
                                    (WorkConversionBlockEntity)
                                            BlockEntity.loadStatic(
                                                    converter.getBlockPos(),
                                                    converter.getBlockState(),
                                                    converter.saveWithFullMetadata(
                                                            helper.getLevel().registryAccess()),
                                                    helper.getLevel().registryAccess());
                            helper.assertTrue(
                                    restored.energy().stored() == converter.energy().stored()
                                            && restored.menuValue(1) == converter.menuValue(1),
                                    "Converter buffer and electrical tier reload together");
                        })
                .thenSucceed();
    }

    private static ElectricWorkBlockEntity source(
            GameTestHelper helper, BlockPos pos, MachineKind kind) {
        helper.setBlock(
                pos,
                ModMachines.block(kind)
                        .defaultBlockState()
                        .setValue(MachineBlock.FACING, Direction.EAST));
        var machine = helper.getBlockEntity(pos, ElectricWorkBlockEntity.class);
        machine.inventory()
                .set(
                        0,
                        ItemResource.of(
                                ModItems.MATERIALS.get(ElectricWorkBlockEntity.part(kind)).get()),
                        1);
        machine.energy().insert(1000);
        return machine;
    }

    private WorkEnergyTests() {}
}
