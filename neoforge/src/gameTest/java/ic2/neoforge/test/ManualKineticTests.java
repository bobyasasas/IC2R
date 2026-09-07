package ic2.neoforge.test;

import com.mojang.authlib.GameProfile;

import ic2.neoforge.machine.EnergyStorageBlockEntity;
import ic2.neoforge.machine.MachineBlock;
import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.machine.ManualKineticBlockEntity;
import ic2.neoforge.registration.ModMachines;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.UUID;

final class ManualKineticTests {
    static void interaction(GameTestHelper helper) {
        var machine = machine(helper);
        var player = player(helper, machine);
        var hit =
                new BlockHitResult(
                        Vec3.atCenterOf(machine.getBlockPos()),
                        Direction.NORTH,
                        machine.getBlockPos(),
                        false);
        for (int i = 0; i < 11; i++)
            machine.getBlockState().useWithoutItem(helper.getLevel(), player, hit);
        var food = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
        player.getFoodData().addAdditionalSaveData(food);
        helper.assertTrue(
                machine.progress() == 200
                        && food.buildResult().getFloatOr("foodExhaustionLevel", 0) == 2.5f,
                "Native right-click accepts ten simulated-player turns at 20 KU and .25 exhaustion"
                    + " each");
        helper.assertTrue(
                player.containerMenu == player.inventoryMenu,
                "Turning the crank must not open a machine menu");
        var restored =
                (ManualKineticBlockEntity)
                        BlockEntity.loadStatic(
                                machine.getBlockPos(),
                                machine.getBlockState(),
                                machine.saveWithFullMetadata(helper.getLevel().registryAccess()),
                                helper.getLevel().registryAccess());
        restored.setLevel(helper.getLevel());
        helper.assertTrue(
                restored.progress() == 200 && !restored.turn(player),
                "Reload retains KU and cannot reset the current tick's ten-click limit");
        helper.succeed();
    }

    static void hungerAndOutputs(GameTestHelper helper) {
        var machine = machine(helper);
        var player = player(helper, machine);
        player.getFoodData().setFoodLevel(6);
        helper.assertTrue(
                !machine.turn(player) && machine.progress() == 0,
                "Hungry players cannot operate the crank");
        player.getFoodData().setFoodLevel(20);
        helper.assertTrue(machine.turn(player), "Fed players can operate the crank");
        try (var transaction = Transaction.openRoot()) {
            helper.assertTrue(
                    machine.output(Direction.UP).extract(20, transaction) == 20,
                    "Manual drives expose every side");
        }
        helper.assertTrue(
                machine.output(Direction.DOWN).available() == 20
                        && machine.output(null).available() == 20,
                "Aborted extraction is restored across all sided views");
        try (var transaction = Transaction.openRoot()) {
            machine.output(Direction.WEST).extract(20, transaction);
            transaction.commit();
        }
        helper.assertTrue(
                machine.output(Direction.EAST).available() == 0,
                "Every direction shares one reservoir");
        player.setPos(
                machine.getBlockPos().getX() + 100,
                machine.getBlockPos().getY(),
                machine.getBlockPos().getZ());
        helper.assertTrue(
                !machine.turn(player), "Out-of-reach direct requests cannot generate work");
        helper.succeed();
    }

    static void networkSupply(GameTestHelper helper) {
        var machine = machine(helper);
        var player = player(helper, machine);
        var pos = new BlockPos(2, 1, 2);
        helper.setBlock(
                pos.east(),
                ModMachines.block(MachineKind.KINETIC_GENERATOR)
                        .defaultBlockState()
                        .setValue(MachineBlock.FACING, Direction.WEST));
        helper.setBlock(pos.east(2), ModMachines.block(MachineKind.MFE));
        var battery = helper.getBlockEntity(pos.east(2), EnergyStorageBlockEntity.class);
        for (int tick = 1; tick <= 30; tick++)
            helper.runAtTickTime(
                    tick,
                    () -> {
                        for (int click = 0; click < 10; click++) machine.turn(player);
                    });
        helper.startSequence()
                .thenWaitUntil(
                        () ->
                                helper.assertTrue(
                                        battery.energy().stored() >= 1024,
                                        "Manual KU must produce repeated whole HV packets through"
                                            + " the kinetic generator"))
                .thenSucceed();
    }

    private static ManualKineticBlockEntity machine(GameTestHelper helper) {
        var pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, ModMachines.block(MachineKind.MANUAL_KINETIC_GENERATOR));
        return helper.getBlockEntity(pos, ManualKineticBlockEntity.class);
    }

    private static FakePlayer player(GameTestHelper helper, ManualKineticBlockEntity machine) {
        var player =
                new FakePlayer(
                        helper.getLevel(), new GameProfile(UUID.randomUUID(), "ic2-crank-test"));
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        player.getFoodData().setFoodLevel(20);
        var pos = machine.getBlockPos();
        player.setPos(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
        return player;
    }

    private ManualKineticTests() {}
}
