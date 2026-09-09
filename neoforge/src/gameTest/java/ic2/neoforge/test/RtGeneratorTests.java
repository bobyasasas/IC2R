package ic2.neoforge.test;

import ic2.neoforge.api.WorkCapabilities;
import ic2.neoforge.item.ElectricItemEnergy;
import ic2.neoforge.machine.EnergyStorageBlockEntity;
import ic2.neoforge.machine.MachineBlock;
import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.machine.RtGeneratorBlockEntity;
import ic2.neoforge.machine.RtHeatGeneratorBlockEntity;
import ic2.neoforge.registration.ModItems;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.registration.ModReactorItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

final class RtGeneratorTests {
    private static final BlockPos POSITION = new BlockPos(2, 1, 2);

    static void heatOutputCurve(GameTestHelper helper) {
        var machine = heatMachine(helper);
        var front =
                helper.getLevel()
                        .getCapability(
                                WorkCapabilities.HEAT,
                                helper.absolutePos(POSITION),
                                Direction.EAST);
        var rear =
                helper.getLevel()
                        .getCapability(
                                WorkCapabilities.HEAT,
                                helper.absolutePos(POSITION),
                                Direction.WEST);
        helper.assertTrue(front != null && rear != null, "Both faces expose the heat source");
        helper.assertTrue(
                front.available() == 0 && machine.installed() == 0,
                "Without pellets the heat source is silent");
        install(machine, 1);
        machine.serverTick(helper.getLevel());
        helper.assertValueEqual(front.available(), 2, "One pellet emits two HU");
        install(machine, 4);
        machine.serverTick(helper.getLevel());
        helper.assertValueEqual(front.available(), 16, "Four pellets emit sixteen HU");
        install(machine, 6);
        machine.serverTick(helper.getLevel());
        helper.assertValueEqual(front.available(), 64, "Six pellets emit sixty-four HU");
        try (var transaction = Transaction.openRoot()) {
            helper.assertValueEqual(
                    rear.extract(64, transaction),
                    0,
                    "Only the front face transmits radioisotope heat");
        }
        try (var transaction = Transaction.openRoot()) {
            helper.assertValueEqual(
                    front.extract(60, transaction), 60, "The front face supplies its buffer");
            transaction.commit();
        }
        machine.serverTick(helper.getLevel());
        try (var transaction = Transaction.openRoot()) {
            helper.assertValueEqual(
                    front.extract(64, transaction),
                    4,
                    "The same-tick budget limits extraction to one emission");
        }
        helper.succeed();
    }

    static void heatSurvivesReload(GameTestHelper helper) {
        var machine = heatMachine(helper);
        install(machine, 2);
        machine.serverTick(helper.getLevel());
        helper.assertValueEqual(machine.progress(), 4, "Two pellets accumulate four HU");
        var restored =
                (RtHeatGeneratorBlockEntity)
                        BlockEntity.loadStatic(
                                machine.getBlockPos(),
                                machine.getBlockState(),
                                machine.saveWithFullMetadata(helper.getLevel().registryAccess()),
                                helper.getLevel().registryAccess());
        helper.getLevel().removeBlockEntity(machine.getBlockPos());
        helper.getLevel().setBlockEntity(restored);
        helper.assertTrue(
                restored.progress() == 4 && restored.installed() == 2,
                "Buffered heat and installed pellets survive a block entity reload");
        helper.succeed();
    }

    static void generatorProduces(GameTestHelper helper) {
        helper.setBlock(POSITION, ModMachines.block(MachineKind.RT_GENERATOR));
        var generator = helper.getBlockEntity(POSITION, RtGeneratorBlockEntity.class);
        helper.assertTrue(
                generator.storedEnergy() == 0 && generator.installed() == 0,
                "An empty radioisotope generator stores nothing");
        installGenerator(generator, 3);
        for (int tick = 0; tick < 10; tick++) generator.serverTick(helper.getLevel());
        helper.assertValueEqual(generator.storedEnergy(), 40.0, "Three pellets make four EU/t");
        helper.setBlock(POSITION.east(), ModMachines.block(MachineKind.BATBOX));
        var battery = helper.getBlockEntity(POSITION.east(), EnergyStorageBlockEntity.class);
        helper.assertTrue(battery != null, "A neighbouring storage connects");
        helper.runAfterDelay(
                40,
                () -> {
                    helper.assertTrue(
                            battery.storedEnergy() > 0,
                            "The native LV source feeds a real storage block");
                    helper.succeed();
                });
    }

    static void generatorChargesTool(GameTestHelper helper) {
        helper.setBlock(POSITION, ModMachines.block(MachineKind.RT_GENERATOR));
        var generator = helper.getBlockEntity(POSITION, RtGeneratorBlockEntity.class);
        installGenerator(generator, 1);
        for (int tick = 0; tick < 30; tick++) generator.serverTick(helper.getLevel());
        var stack = new ItemStack(ModItems.RE_BATTERY.get());
        generator.inventory().set(6, ItemResource.of(stack), 1);
        generator.serverTick(helper.getLevel());
        var stored = ElectricItemEnergy.charge(generator.inventory().stack(6));
        helper.assertTrue(
                stored > 0, "The battery slot charges a real battery from radioisotope output");
        helper.assertValueEqual(generator.outputRate(), 1, "One pellet produces one EU per tick");
        try (var transaction = Transaction.openRoot()) {
            helper.assertValueEqual(
                    generator.inventory().insert(0, ItemResource.of(Items.DIAMOND), 1, transaction),
                    0,
                    "Pellet slots reject unrelated items in the backend");
        }
        helper.succeed();
    }

    static void pelletsAutomate(GameTestHelper helper) {
        var machine = heatMachine(helper);
        var port =
                helper.getLevel()
                        .getCapability(
                                Capabilities.Item.BLOCK,
                                helper.absolutePos(POSITION),
                                Direction.UP);
        helper.assertTrue(port != null, "Item automation is registered");
        try (var transaction = Transaction.openRoot()) {
            helper.assertValueEqual(
                    port.insert(
                            0, ItemResource.of(ModReactorItems.RTG_PELLET.get()), 2, transaction),
                    1,
                    "Every slot accepts a single pellet");
            helper.assertValueEqual(
                    port.insert(0, ItemResource.of(Items.DIAMOND), 1, transaction),
                    0,
                    "Unrelated items are rejected by the backend");
            transaction.commit();
        }
        helper.assertValueEqual(machine.installed(), 1, "The automated pellet is installed");
        helper.succeed();
    }

    private static RtHeatGeneratorBlockEntity heatMachine(GameTestHelper helper) {
        helper.setBlock(
                POSITION,
                ModMachines.block(MachineKind.RT_HEAT_GENERATOR)
                        .defaultBlockState()
                        .setValue(MachineBlock.FACING, Direction.EAST));
        return helper.getBlockEntity(POSITION, RtHeatGeneratorBlockEntity.class);
    }

    /** Tops the machine up to the requested pellet count, mirroring real installation. */
    private static void install(RtHeatGeneratorBlockEntity machine, int pellets) {
        while (machine.installed() < pellets) {
            int slot = -1;
            for (int candidate = 0; candidate < 6; candidate++) {
                if (machine.inventory().getResource(candidate).isEmpty()) {
                    slot = candidate;
                    break;
                }
            }
            try (var transaction = Transaction.openRoot()) {
                if (slot < 0
                        || machine.inventory()
                                        .insert(
                                                slot,
                                                ItemResource.of(ModReactorItems.RTG_PELLET.get()),
                                                1,
                                                transaction)
                                != 1)
                    throw new IllegalStateException("Test pellet insertion failed");
                transaction.commit();
            }
        }
    }

    private static void installGenerator(RtGeneratorBlockEntity machine, int pellets) {
        while (machine.installed() < pellets) {
            int slot = -1;
            for (int candidate = 0; candidate < 6; candidate++) {
                if (machine.inventory().getResource(candidate).isEmpty()) {
                    slot = candidate;
                    break;
                }
            }
            try (var transaction = Transaction.openRoot()) {
                if (slot < 0
                        || machine.inventory()
                                        .insert(
                                                slot,
                                                ItemResource.of(ModReactorItems.RTG_PELLET.get()),
                                                1,
                                                transaction)
                                != 1)
                    throw new IllegalStateException("Test pellet insertion failed");
                transaction.commit();
            }
        }
    }
}
