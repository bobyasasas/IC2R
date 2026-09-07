package ic2.neoforge.test;

import ic2.neoforge.fluid.FluidDefinition;
import ic2.neoforge.machine.FluidGeneratorBlockEntity;
import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.machine.SolarGeneratorBlockEntity;
import ic2.neoforge.registration.ModFluids;
import ic2.neoforge.registration.ModMachines;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

final class GenerationTests {
    static void geothermal(GameTestHelper helper) {
        var machine = fluidMachine(helper, MachineKind.GEO_GENERATOR);
        var lava = FluidResource.of(Fluids.LAVA);
        machine.inventory().set(0, ItemResource.of(Items.LAVA_BUCKET), 1);
        machine.serverTick(helper.getLevel());
        helper.assertTrue(
                machine.tank().getAmountAsInt(0) == 998
                        && machine.energy().stored() == 20
                        && machine.inventory().stack(0).isEmpty()
                        && machine.inventory().stack(1).is(Items.BUCKET),
                "Lava bucket must return its bucket and consume two mB for twenty EU");
        machine.energy().insert(machine.energy().free());
        machine.serverTick(helper.getLevel());
        helper.assertTrue(
                machine.tank().getAmountAsInt(0) == 998, "Full EU buffer must preserve lava");
        try (var tx = Transaction.openRoot()) {
            helper.assertTrue(
                    machine.fluidAutomation(Direction.NORTH).extract(0, lava, 1, tx) == 0,
                    "Fuel tank is externally insert-only");
            helper.assertTrue(
                    machine.fluidAutomation(Direction.NORTH)
                                    .insert(0, FluidResource.of(Fluids.WATER), 1000, tx)
                            == 0,
                    "Geothermal generator must reject water");
        }
        machine.tank().set(0, FluidResource.EMPTY, 0);
        machine.inventory().set(0, ItemResource.of(Items.LAVA_BUCKET), 1);
        machine.inventory().set(1, ItemResource.of(Items.COBBLESTONE), 64);
        machine.serverTick(helper.getLevel());
        helper.assertTrue(
                machine.tank().getAmountAsInt(0) == 0
                        && machine.inventory().stack(0).is(Items.LAVA_BUCKET),
                "Blocked container output must roll back fluid transfer");
        helper.succeed();
    }

    static void semifluidPersistence(GameTestHelper helper) {
        var machine = fluidMachine(helper, MachineKind.SEMIFLUID_GENERATOR);
        machine.tank()
                .set(
                        0,
                        FluidResource.of(
                                ModFluids.FAMILIES.get(FluidDefinition.BIOGAS).source().get()),
                        10);
        for (int tick = 0; tick < 5; tick++) machine.serverTick(helper.getLevel());
        helper.assertTrue(
                machine.energy().stored() == 80
                        && machine.tank().getAmountAsInt(0) == 0
                        && machine.progress() == 5,
                "Ten mB biogas must last ten ticks");
        var restored =
                (FluidGeneratorBlockEntity)
                        BlockEntity.loadStatic(
                                machine.getBlockPos(),
                                machine.getBlockState(),
                                machine.saveWithFullMetadata(helper.getLevel().registryAccess()),
                                helper.getLevel().registryAccess());
        for (int tick = 0; tick < 10; tick++) restored.serverTick(helper.getLevel());
        helper.assertTrue(
                restored.energy().stored() == 160 && restored.progress() == 0,
                "Reloaded biogas batch must finish exactly once");
        helper.succeed();
    }

    static void solar(GameTestHelper helper) {
        helper.setTime(6000);
        var pos = new BlockPos(8, 1, 8);
        helper.setBlock(pos, ModMachines.block(MachineKind.SOLAR_GENERATOR));
        var machine = helper.getBlockEntity(pos, SolarGeneratorBlockEntity.class);
        helper.runAtTickTime(
                8,
                () -> {
                    machine.sampleSunlight(helper.getLevel());
                    double before = machine.energy().stored();
                    machine.serverTick(helper.getLevel());
                    helper.assertTrue(
                            machine.progress() > 900 && machine.energy().stored() > before,
                            "Daylight must produce real EU through the native sun attribute");
                    helper.setBlock(pos.above(), Blocks.STONE);
                });
        helper.runAtTickTime(
                16,
                () -> {
                    machine.sampleSunlight(helper.getLevel());
                    helper.assertTrue(
                            machine.progress() == 0, "Opaque cover must remove solar input");
                    helper.setBlock(pos.above(), Blocks.AIR);
                    helper.setTime(18000);
                });
        helper.runAtTickTime(
                24,
                () -> {
                    machine.sampleSunlight(helper.getLevel());
                    helper.assertTrue(machine.progress() == 0, "Night must remove solar input");
                    helper.setTime(6000);
                    helper.succeed();
                });
    }

    private static FluidGeneratorBlockEntity fluidMachine(GameTestHelper helper, MachineKind kind) {
        var pos = new BlockPos(5, 1, 5);
        helper.setBlock(pos, ModMachines.block(kind));
        return helper.getBlockEntity(pos, FluidGeneratorBlockEntity.class);
    }

    private GenerationTests() {}
}
