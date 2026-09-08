package ic2.neoforge.test;

import ic2.core.machine.RotorMaterial;
import ic2.core.machine.RotorOperation;
import ic2.neoforge.machine.EnergyStorageBlockEntity;
import ic2.neoforge.machine.MachineBlock;
import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.machine.WindTurbineBlockEntity;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.registration.ModRotors;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

final class WindTurbineTests {
    private static final BlockPos POSITION = new BlockPos(17, 180, 17);

    static void operationAndWear(GameTestHelper helper) {
        var turbine = machine(helper);
        turbine.serverTick(helper.getLevel());
        int rate = turbine.menuValue(0), damage = turbine.inventory().stack(0).getDamageValue();
        helper.assertTrue(
                rate > 0 && turbine.rotorDiameter() == 5 && turbine.rotorDegreesPerTick() > 0,
                "A wooden rotor in strong unobstructed wind must operate");
        helper.assertTrue(
                turbine.output(Direction.NORTH).available() == 0,
                "Turbines emit kinetic work through the back face");
        try (var transaction = Transaction.openRoot()) {
            helper.assertTrue(
                    turbine.output(Direction.SOUTH).extract(rate, transaction) == rate,
                    "Native turbine flow can be drawn transactionally");
        }
        helper.assertTrue(
                turbine.output(Direction.SOUTH).available() == rate,
                "A rolled-back draw must restore the flow budget");
        try (var transaction = Transaction.openRoot()) {
            turbine.output(Direction.SOUTH).extract(rate, transaction);
            transaction.commit();
        }
        var restored =
                (WindTurbineBlockEntity)
                        BlockEntity.loadStatic(
                                turbine.getBlockPos(),
                                turbine.getBlockState(),
                                turbine.saveWithFullMetadata(helper.getLevel().registryAccess()),
                                helper.getLevel().registryAccess());
        restored.setLevel(helper.getLevel());
        restored.serverTick(helper.getLevel());
        helper.assertTrue(
                restored.output(Direction.SOUTH).available() == 0
                        && restored.inventory().stack(0).getDamageValue() == damage,
                "Reload cannot duplicate this tick's output or wear");
        var update = turbine.getUpdateTag(helper.getLevel().registryAccess());
        helper.assertTrue(
                update.contains("rotor")
                        && !update.contains("inventory")
                        && !update.contains("outputExtracted"),
                "Observers receive only rotor visual state");
        helper.runAtTickTime(
                40,
                () -> {
                    helper.assertTrue(
                            turbine.inventory().stack(0).getDamageValue() > damage,
                            "A running rotor must wear at its fixed world-time interval");
                    helper.succeed();
                });
    }

    static void obstructions(GameTestHelper helper) {
        var turbine = machine(helper);
        try (var transaction = Transaction.openRoot()) {
            helper.assertTrue(
                    turbine.inventory().insert(0, ItemResource.of(Items.DIAMOND), 1, transaction)
                            == 0,
                    "Non-rotors cannot enter the native rotor slot");
        }
        helper.setBlock(POSITION.north(), Blocks.STONE);
        helper.startSequence()
                .thenWaitUntil(
                        () ->
                                helper.assertTrue(
                                        turbine.menuValue(1)
                                                        == RotorOperation.Status.NO_SPACE.ordinal()
                                                && turbine.menuValue(0) == 0,
                                        "A blocked rotor plane must stop production"))
                .thenExecute(() -> helper.setBlock(POSITION.north(), Blocks.AIR))
                .thenWaitUntil(
                        () ->
                                helper.assertTrue(
                                        turbine.menuValue(0) > 0,
                                        "Clearing the plane must resume production"))
                .thenExecute(
                        () ->
                                helper.setBlock(
                                        POSITION.north(10),
                                        ModMachines.block(MachineKind.WIND_KINETIC_GENERATOR)))
                .thenWaitUntil(
                        () ->
                                helper.assertTrue(
                                        turbine.menuValue(1)
                                                        == RotorOperation.Status.INTERFERENCE
                                                                .ordinal()
                                                && turbine.menuValue(0) == 0,
                                        "A second turbine in the air channel must cause"
                                            + " interference"))
                .thenSucceed();
    }

    static void networkSupply(GameTestHelper helper) {
        machine(helper);
        helper.setBlock(
                POSITION.south(),
                ModMachines.block(MachineKind.KINETIC_GENERATOR)
                        .defaultBlockState()
                        .setValue(MachineBlock.FACING, Direction.NORTH));
        helper.setBlock(
                POSITION.south(2),
                ModMachines.block(MachineKind.MFE)
                        .defaultBlockState()
                        .setValue(MachineBlock.FACING, Direction.SOUTH));
        var battery = helper.getBlockEntity(POSITION.south(2), EnergyStorageBlockEntity.class);
        helper.startSequence()
                .thenWaitUntil(
                        () ->
                                helper.assertTrue(
                                        battery.energy().stored() >= 256,
                                        "Wind-driven KU must pass through a real kinetic generator"
                                            + " into electrical storage"))
                .thenSucceed();
    }

    private static WindTurbineBlockEntity machine(GameTestHelper helper) {
        helper.setBlock(
                POSITION,
                ModMachines.block(MachineKind.WIND_KINETIC_GENERATOR)
                        .defaultBlockState()
                        .setValue(MachineBlock.FACING, Direction.NORTH));
        var turbine = helper.getBlockEntity(POSITION, WindTurbineBlockEntity.class);
        turbine.inventory()
                .set(0, ItemResource.of(ModRotors.ROTORS.get(RotorMaterial.WOODEN).get()), 1);
        return turbine;
    }

    private WindTurbineTests() {}
}
