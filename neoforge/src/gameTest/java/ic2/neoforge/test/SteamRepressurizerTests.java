package ic2.neoforge.test;

import ic2.neoforge.fluid.FluidDefinition;
import ic2.neoforge.machine.*;
import ic2.neoforge.registration.BalanceConfig;
import ic2.neoforge.registration.MaterialDefinition;
import ic2.neoforge.registration.ModFluids;
import ic2.neoforge.registration.ModItems;
import ic2.neoforge.registration.ModMachines;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.List;

final class SteamRepressurizerTests {
    private static final BlockPos POSITION = new BlockPos(2, 1, 2);

    static void idleWithoutCandidate(GameTestHelper helper) {
        var machine = machine(helper);
        fill(machine.inputTank(), fluid(FluidDefinition.STEAM), 1000);
        var source = heatSource(helper, POSITION.west());
        for (int tick = 0; tick < 20; tick++) {
            source.serverTick(helper.getLevel());
            machine.serverTick(helper.getLevel());
        }
        helper.assertTrue(
                machine.inputTank().getAmountAsInt(0) == 1000
                        && machine.heatState().reserve() == 0
                        && machine.outputTank().getAmountAsInt(0) == 0
                        && !machine.getBlockState().getValue(MachineBlock.ACTIVE),
                "Without a tagged external steam the machine never starts or draws heat");
        helper.assertValueEqual(
                source.energy().stored(),
                9990.0,
                "An idle re-pressurizer leaves the neighbouring heat source untouched");
        helper.succeed();
    }

    static void ratiosAndConservation(GameTestHelper helper) {
        var machine = machine(helper);
        var source = heatSource(helper, POSITION.west());
        try {
            machine.overrideExternalSteam(() -> List.of(Fluids.WATER));
            fill(machine.inputTank(), fluid(FluidDefinition.STEAM), 1000);
            helper.runAfterDelay(
                    40,
                    () -> {
                        helper.assertValueEqual(
                                machine.outputTank().getAmountAsInt(0), 1600, "ordinary output");
                        helper.assertValueEqual(
                                machine.inputTank().getAmountAsInt(0), 0, "ordinary input");
                        helper.assertValueEqual(
                                machine.heatState().reserve(), 0, "ordinary reserve");
                        fill(machine.inputTank(), fluid(FluidDefinition.SUPERHEATED_STEAM), 1000);
                        helper.runAfterDelay(
                                40,
                                () -> {
                                    try {
                                        helper.assertValueEqual(
                                                machine.outputTank().getAmountAsInt(0),
                                                4800,
                                                "superheated output");
                                        helper.assertValueEqual(
                                                machine.inputTank().getAmountAsInt(0),
                                                0,
                                                "superheated input");
                                        helper.assertValueEqual(
                                                machine.heatState().reserve(),
                                                0,
                                                "superheated reserve");
                                        double produced = 10000 - source.energy().stored();
                                        helper.assertTrue(
                                                produced >= 200 && produced < 229,
                                                "Two hundred converted batches cost two hundred"
                                                        + " HU plus idle coil ticks");
                                    } finally {
                                        machine.overrideExternalSteam(null);
                                    }
                                    helper.succeed();
                                });
                    });
        } catch (RuntimeException e) {
            machine.overrideExternalSteam(null);
            throw e;
        }
    }

    static void blockedOutputPrepaysHeat(GameTestHelper helper) {
        var machine = machine(helper);
        var source = heatSource(helper, POSITION.west());
        try {
            machine.overrideExternalSteam(() -> List.of(Fluids.WATER));
            fill(machine.outputTank(), FluidResource.of(Fluids.WATER), 9985);
            fill(machine.inputTank(), fluid(FluidDefinition.STEAM), 1000);
            source.serverTick(helper.getLevel());
            machine.serverTick(helper.getLevel());
            helper.assertTrue(
                    machine.heatState().reserve() == 10
                            && machine.inputTank().getAmountAsInt(0) == 1000,
                    "A missing batch of output space still stores the drawn heat");
            try (var transaction = Transaction.openRoot()) {
                helper.assertValueEqual(
                        machine.outputTank()
                                .extract(0, FluidResource.of(Fluids.WATER), 16, transaction),
                        16,
                        "Test frees one batch of output space");
                transaction.commit();
            }
            helper.runAfterDelay(
                    40,
                    () -> {
                        try {
                            helper.assertValueEqual(
                                    machine.outputTank().getAmountAsInt(0), 9985, "freed output");
                            helper.assertValueEqual(
                                    machine.inputTank().getAmountAsInt(0), 990, "freed input");
                            helper.assertValueEqual(
                                    machine.heatState().reserve(),
                                    99,
                                    "freed reserve refills to the new whole input demand");
                        } finally {
                            machine.overrideExternalSteam(null);
                        }
                        helper.succeed();
                    });
        } catch (RuntimeException e) {
            machine.overrideExternalSteam(null);
            throw e;
        }
    }

    static void candidateSelection(GameTestHelper helper) {
        var machine = machine(helper);
        var source = heatSource(helper, POSITION.west());
        try {
            // Registration order must not matter: lava sorts before water by registry id.
            machine.overrideExternalSteam(() -> List.of(Fluids.WATER, Fluids.LAVA));
            fill(machine.inputTank(), fluid(FluidDefinition.STEAM), 100);
            source.serverTick(helper.getLevel());
            machine.serverTick(helper.getLevel());
            helper.assertTrue(
                    machine.outputTank().getResource(0).getFluid() == Fluids.LAVA
                            && machine.outputTank().getAmountAsInt(0) == 160,
                    "The stable id order picks lava over water regardless of list order");
            // A tag reload that drops lava must strand the stored output instead of mixing.
            machine.overrideExternalSteam(() -> List.of(Fluids.WATER));
            source.serverTick(helper.getLevel());
            machine.serverTick(helper.getLevel());
            helper.assertTrue(
                    machine.outputTank().getAmountAsInt(0) == 160
                            && machine.heatState().reserve() == 0,
                    "A reload never swaps or dilutes an output that is no longer tagged");
            try (var transaction = Transaction.openRoot()) {
                machine.outputTank().extract(0, FluidResource.of(Fluids.LAVA), 10000, transaction);
                transaction.commit();
            }
            fill(machine.inputTank(), fluid(FluidDefinition.STEAM), 100);
            helper.runAfterDelay(
                    40,
                    () -> {
                        try {
                            helper.assertValueEqual(
                                    machine.outputTank().getAmountAsInt(0), 160, "adopted output");
                            helper.assertTrue(
                                    machine.outputTank().getResource(0).getFluid() == Fluids.WATER,
                                    "Once drained the machine adopts the reloaded candidate");
                        } finally {
                            machine.overrideExternalSteam(null);
                        }
                        helper.succeed();
                    });
        } catch (RuntimeException e) {
            machine.overrideExternalSteam(null);
            throw e;
        }
    }

    static void zeroRateStops(GameTestHelper helper) {
        var machine = machine(helper);
        var source = heatSource(helper, POSITION.west());
        int previous = BalanceConfig.STEAM_PER_STEAM.get();
        try {
            machine.overrideExternalSteam(() -> List.of(Fluids.WATER));
            BalanceConfig.STEAM_PER_STEAM.set(0);
            fill(machine.inputTank(), fluid(FluidDefinition.STEAM), 100);
            source.serverTick(helper.getLevel());
            machine.serverTick(helper.getLevel());
            helper.assertTrue(
                    machine.inputTank().getAmountAsInt(0) == 100
                            && machine.heatState().reserve() == 0
                            && machine.outputTank().getAmountAsInt(0) == 0,
                    "A zero rate is a stopped machine, not a steam incinerator");
        } finally {
            BalanceConfig.STEAM_PER_STEAM.set(previous);
            machine.overrideExternalSteam(null);
        }
        helper.succeed();
    }

    static void reserveSurvivesReload(GameTestHelper helper) {
        var machine = machine(helper);
        try {
            machine.overrideExternalSteam(() -> List.of(Fluids.WATER));
            fill(machine.outputTank(), FluidResource.of(Fluids.WATER), 9985);
            fill(machine.inputTank(), fluid(FluidDefinition.STEAM), 1000);
            var source = heatSource(helper, POSITION.west());
            source.serverTick(helper.getLevel());
            machine.serverTick(helper.getLevel());
            var restored =
                    (SteamRepressurizerBlockEntity)
                            BlockEntity.loadStatic(
                                    machine.getBlockPos(),
                                    machine.getBlockState(),
                                    machine.saveWithFullMetadata(
                                            helper.getLevel().registryAccess()),
                                    helper.getLevel().registryAccess());
            helper.getLevel().removeBlockEntity(machine.getBlockPos());
            helper.getLevel().setBlockEntity(restored);
            restored.serverTick(helper.getLevel());
            helper.assertTrue(
                    restored.heatState().reserve() == 10
                            && restored.inputTank().getAmountAsInt(0) == 1000,
                    "Prepaid heat is part of the saved state and needs no source after reload");
        } finally {
            machine.overrideExternalSteam(null);
        }
        helper.succeed();
    }

    private static SteamRepressurizerBlockEntity machine(GameTestHelper helper) {
        helper.setBlock(POSITION, ModMachines.block(MachineKind.STEAM_REPRESSURIZER));
        return helper.getBlockEntity(POSITION, SteamRepressurizerBlockEntity.class);
    }

    private static ElectricWorkBlockEntity heatSource(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(
                pos,
                ModMachines.block(MachineKind.ELECTRIC_HEAT_GENERATOR)
                        .defaultBlockState()
                        .setValue(MachineBlock.FACING, Direction.EAST));
        var source = helper.getBlockEntity(pos, ElectricWorkBlockEntity.class);
        source.inventory()
                .set(0, ItemResource.of(ModItems.MATERIALS.get(MaterialDefinition.COIL).get()), 1);
        source.energy().insert(10000);
        return source;
    }

    private static FluidResource fluid(FluidDefinition definition) {
        return FluidResource.of(ModFluids.FAMILIES.get(definition).source().get());
    }

    private static void fill(
            ic2.neoforge.transfer.MachineFluidTank tank, FluidResource fluid, int amount) {
        try (var transaction = Transaction.openRoot()) {
            if (tank.insert(0, fluid, amount, transaction) != amount)
                throw new IllegalStateException("Test tank cannot fit input");
            transaction.commit();
        }
    }

    private SteamRepressurizerTests() {}
}
