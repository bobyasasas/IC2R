package ic2.neoforge.test;

import ic2.neoforge.machine.FluidRegulatorBlockEntity;
import ic2.neoforge.machine.MachineBlock;
import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.machine.TankBlockEntity;
import ic2.neoforge.menu.MachineMenu;
import ic2.neoforge.registration.ModMachines;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

final class FluidRegulatorTests {
    private static final BlockPos POSITION = new BlockPos(2, 1, 2);
    private static final FluidResource WATER = FluidResource.of(Fluids.WATER);

    static void partialTarget(GameTestHelper helper) {
        var regulator = machine(helper);
        var target = helper.getBlockEntity(POSITION.east(), TankBlockEntity.class);
        fill(regulator, 1000);
        regulator.energy().insert(30);
        regulator.menuAction(2);
        regulator.menuAction(8);
        try (var transaction = Transaction.openRoot()) {
            target.tank().insert(0, WATER, 23999, transaction);
            transaction.commit();
        }
        regulator.serverTick(helper.getLevel());
        helper.assertTrue(
                target.tank().getAmountAsInt(0) == 24000
                        && regulator.tank().getAmountAsInt(0) == 999
                        && regulator.energy().stored() == 20,
                "A target accepting only one mB must drain one mB and charge one ten-EU delivery");
        var restored =
                (FluidRegulatorBlockEntity)
                        BlockEntity.loadStatic(
                                regulator.getBlockPos(),
                                regulator.getBlockState(),
                                regulator.saveWithFullMetadata(helper.getLevel().registryAccess()),
                                helper.getLevel().registryAccess());
        restored.setLevel(helper.getLevel());
        try (var transaction = Transaction.openRoot()) {
            target.tank().extract(0, WATER, 1000, transaction);
            transaction.commit();
        }
        restored.serverTick(helper.getLevel());
        helper.assertTrue(
                restored.tank().getAmountAsInt(0) == 999 && restored.energy().stored() == 20,
                "Same-tick reload must not trigger a second paid delivery");
        helper.succeed();
    }

    static void cadence(GameTestHelper helper) {
        var regulator = machine(helper);
        fill(regulator, 1000);
        regulator.energy().insert(100);
        regulator.menuAction(1);
        var target = helper.getBlockEntity(POSITION.east(), TankBlockEntity.class);
        helper.runAtTickTime(
                1,
                () -> {
                    int startAmount = target.tank().getAmountAsInt(0);
                    double startEnergy = regulator.energy().stored();
                    helper.runAfterDelay(
                            60,
                            () -> {
                                helper.assertTrue(
                                        target.tank().getAmountAsInt(0) == startAmount + 30
                                                && regulator.energy().stored() == startEnergy - 30,
                                        "Every complete sixty-tick window contains three paid"
                                            + " per-second deliveries");
                                int beforeModeChange = target.tank().getAmountAsInt(0);
                                regulator.menuAction(8);
                                helper.runAfterDelay(
                                        5,
                                        () -> {
                                            helper.assertTrue(
                                                    target.tank().getAmountAsInt(0)
                                                            == beforeModeChange + 50,
                                                    "Five ticks in per-tick mode produce five"
                                                        + " deliveries");
                                            helper.succeed();
                                        });
                            });
                });
    }

    static void portsAndBlockedPower(GameTestHelper helper) {
        var regulator = machine(helper);
        regulator.menuAction(3);
        regulator.menuAction(8);
        regulator.inventory().set(0, ItemResource.of(Items.WATER_BUCKET), 1);
        regulator.serverTick(helper.getLevel());
        helper.assertTrue(
                regulator.tank().getAmountAsInt(0) == 1000
                        && regulator.inventory().stack(1).is(Items.BUCKET),
                "Container input must work while output lacks power");
        regulator.energy().insert(9);
        regulator.serverTick(helper.getLevel());
        helper.assertTrue(
                regulator.tank().getAmountAsInt(0) == 1000 && regulator.energy().stored() == 9,
                "Insufficient EU must not move or consume fluid");
        var front = regulator.fluidAutomation(Direction.EAST);
        try (var transaction = Transaction.openRoot()) {
            helper.assertTrue(
                    front.insert(0, WATER, 1, transaction) == 0
                            && regulator
                                            .fluidAutomation(Direction.WEST)
                                            .insert(0, WATER, 1, transaction)
                                    == 1
                            && regulator
                                            .fluidAutomation(Direction.WEST)
                                            .extract(0, WATER, 1, transaction)
                                    == 0,
                    "Regulators accept from non-front faces and expose no unmetered extraction");
        }
        helper.setBlock(
                POSITION, regulator.getBlockState().setValue(MachineBlock.FACING, Direction.WEST));
        try (var transaction = Transaction.openRoot()) {
            helper.assertTrue(
                    front.insert(0, WATER, 1, transaction) == 1,
                    "Retained fluid capability views must follow the current facing");
        }
        helper.succeed();
    }

    static void menuSettings(GameTestHelper helper) {
        var regulator = machine(helper);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(
                regulator.getBlockPos().getX(),
                regulator.getBlockPos().getY(),
                regulator.getBlockPos().getZ());
        var menu = new MachineMenu(1, player.getInventory(), regulator);
        player.containerMenu = menu;
        helper.assertTrue(
                menu.clickMenuButton(player, 3)
                        && menu.clickMenuButton(player, 0)
                        && regulator.menuValue(0) == 1000,
                "Menu adjustments clamp flow to one thousand mB");
        helper.assertTrue(
                !menu.clickMenuButton(player, 1001) && regulator.menuValue(2) == 0,
                "Unrecognized legacy event integers must not bypass explicit action IDs");
        helper.assertTrue(
                menu.clickMenuButton(player, 8) && regulator.menuValue(2) == 1,
                "Explicit mode action selects per-tick operation");
        player.containerMenu = player.inventoryMenu;
        helper.assertTrue(
                !menu.clickMenuButton(player, 9) && regulator.menuValue(2) == 1,
                "Closed menus cannot change flow settings");
        helper.succeed();
    }

    private static FluidRegulatorBlockEntity machine(GameTestHelper helper) {
        helper.setBlock(
                POSITION,
                ModMachines.block(MachineKind.FLUID_REGULATOR)
                        .defaultBlockState()
                        .setValue(MachineBlock.FACING, Direction.EAST));
        helper.setBlock(POSITION.east(), ModMachines.block(MachineKind.TANK));
        return helper.getBlockEntity(POSITION, FluidRegulatorBlockEntity.class);
    }

    private static void fill(FluidRegulatorBlockEntity regulator, int amount) {
        try (var transaction = Transaction.openRoot()) {
            regulator.tank().insert(0, WATER, amount, transaction);
            transaction.commit();
        }
    }

    private FluidRegulatorTests() {}
}
