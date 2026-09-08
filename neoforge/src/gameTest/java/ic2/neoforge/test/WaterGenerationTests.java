package ic2.neoforge.test;

import ic2.neoforge.machine.EnergyStorageBlockEntity;
import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.machine.WaterGeneratorBlockEntity;
import ic2.neoforge.registration.ModCells;
import ic2.neoforge.registration.ModMachines;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

final class WaterGenerationTests {
    static void bucketPersistence(GameTestHelper helper) {
        var machine = machine(helper);
        machine.inventory().set(0, ItemResource.of(Items.WATER_BUCKET), 1);
        double produced = 0;
        for (int i = 0; i < 100; i++) {
            machine.serverTick(helper.getLevel());
            produced += machine.energy().extract(machine.energy().stored());
        }
        helper.assertTrue(
                machine.progress() == 400 && machine.inventory().stack(0).is(Items.BUCKET),
                "A water bucket returns to its input slot and powers a 500-tick batch");
        var restored =
                (WaterGeneratorBlockEntity)
                        BlockEntity.loadStatic(
                                machine.getBlockPos(),
                                machine.getBlockState(),
                                machine.saveWithFullMetadata(helper.getLevel().registryAccess()),
                                helper.getLevel().registryAccess());
        for (int i = 0; i < 500; i++) {
            restored.serverTick(helper.getLevel());
            produced += restored.energy().extract(restored.energy().stored());
        }
        helper.assertTrue(
                produced == 500 && restored.progress() == 0,
                "Reloading a bucket batch must preserve one-EU production rather than the two-EU"
                        + " consumable rate");
        helper.succeed();
    }

    static void cellAndAutomation(GameTestHelper helper) {
        var machine = machine(helper);
        machine.inventory().set(0, ItemResource.of(ModCells.WATER.get()), 1);
        machine.serverTick(helper.getLevel());
        helper.assertTrue(
                machine.inventory().stack(0).isEmpty()
                        && machine.progress() == 499
                        && machine.energy().stored() == 2,
                "A water cell without a crafting remainder is consumed at two EU per tick");
        try (var transaction = Transaction.openRoot()) {
            helper.assertTrue(
                    machine.automation(Direction.UP)
                                    .insert(0, ItemResource.of(Items.WATER_BUCKET), 1, transaction)
                            == 0,
                    "Watermill automation defaults to disabled");
        }
        helper.assertTrue(
                !WaterGeneratorBlockEntity.containsWater(ItemResource.of(Items.LAVA_BUCKET)),
                "Water mill must reject other fluids");
        helper.succeed();
    }

    static void ambientAndRotor(GameTestHelper helper) {
        var pos = new BlockPos(3, 1, 3);
        helper.setBlock(pos, ModMachines.block(MachineKind.WATER_GENERATOR));
        helper.setBlock(pos.east(), Blocks.WATER);
        helper.setBlock(pos.west(), Blocks.WATER);
        helper.setBlock(pos.north(), Blocks.WATER);
        var machine = helper.getBlockEntity(pos, WaterGeneratorBlockEntity.class);
        machine.sampleWater(helper.getLevel());
        machine.serverTick(helper.getLevel());
        helper.assertTrue(
                machine.menuValue(0) == 3 && Math.abs(machine.energy().stored() - .03) < .000001,
                "Native neighboring source water must produce hundredth-EU power");
        helper.assertTrue(
                Math.abs(machine.rotorSpeed() - .12f) < .000001,
                "Ambient rotor speed follows the sampled water count");
        var update = machine.getUpdateTag(helper.getLevel().registryAccess());
        helper.assertTrue(
                update.contains("rotorSpeed")
                        && !update.contains("energy")
                        && !update.contains("inventory"),
                "Observer updates must contain only rotor state");
        machine.inventory().set(0, ItemResource.of(Items.BUCKET), 1);
        machine.handleUpdateTag(
                net.minecraft.world.level.storage.TagValueInput.create(
                        net.minecraft.util.ProblemReporter.DISCARDING,
                        helper.getLevel().registryAccess(),
                        update));
        helper.assertTrue(
                machine.inventory().stack(0).is(Items.BUCKET)
                        && Math.abs(machine.energy().stored() - .03) < .000001,
                "Applying a visual observer update must not clear inventory or energy state");
        helper.succeed();
    }

    static void networkSupply(GameTestHelper helper) {
        var pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, ModMachines.block(MachineKind.WATER_GENERATOR));
        helper.setBlock(pos.east(), ModMachines.block(MachineKind.BATBOX));
        var generator = helper.getBlockEntity(pos, WaterGeneratorBlockEntity.class);
        var battery = helper.getBlockEntity(pos.east(), EnergyStorageBlockEntity.class);
        generator.inventory().set(0, ItemResource.of(Items.WATER_BUCKET), 1);
        helper.runAtTickTime(
                45,
                () -> {
                    helper.assertTrue(
                            battery.energy().stored() >= 32,
                            "A water mill must supply a real adjacent BatBox in both IC2 and"
                                    + " whole-packet GT modes");
                    helper.succeed();
                });
    }

    private static WaterGeneratorBlockEntity machine(GameTestHelper helper) {
        return new WaterGeneratorBlockEntity(
                helper.absolutePos(new BlockPos(1, 1, 1)),
                ModMachines.block(MachineKind.WATER_GENERATOR).defaultBlockState());
    }

    private WaterGenerationTests() {}
}
