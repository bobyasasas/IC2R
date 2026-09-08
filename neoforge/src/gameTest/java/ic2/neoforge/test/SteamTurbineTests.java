package ic2.neoforge.test;

import ic2.neoforge.fluid.FluidDefinition;
import ic2.neoforge.item.UpgradeItem;
import ic2.neoforge.machine.*;
import ic2.neoforge.menu.MachineMenu;
import ic2.neoforge.registration.*;

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

final class SteamTurbineTests {
    private static final BlockPos POSITION = new BlockPos(2, 1, 2);

    static void consumerTicksFirst(GameTestHelper helper) {
        // Insertion order puts the converter before the steam-consuming turbine in native ticking.
        helper.setBlock(
                POSITION.east(),
                ModMachines.block(MachineKind.KINETIC_GENERATOR)
                        .defaultBlockState()
                        .setValue(MachineBlock.FACING, Direction.WEST));
        helper.setBlock(POSITION.east(2), ModMachines.block(MachineKind.MFE));
        var turbine = turbine(helper, POSITION, Direction.EAST);
        helper.setBlock(POSITION.north(), ModMachines.block(MachineKind.CONDENSER));
        fill(turbine.steamTank(), fluid(FluidDefinition.STEAM), 1000);
        turbine.serverTick(helper.getLevel());
        var converter = helper.getBlockEntity(POSITION.east(), WorkConversionBlockEntity.class);
        converter.serverTick(helper.getLevel());
        helper.assertValueEqual(
                converter.storedEnergy(),
                500.0,
                "One thousand steam mB pay for 2000 KU and 500 EU");
        helper.runAfterDelay(
                4,
                () -> {
                    var storage =
                            helper.getBlockEntity(POSITION.east(2), EnergyStorageBlockEntity.class);
                    helper.assertValueEqual(
                            converter.storedEnergy() + storage.storedEnergy(),
                            500.0,
                            "A consumer ticking first on the next tick cannot recreate the already"
                                    + " exhausted KU batch");
                    helper.succeed();
                });
    }

    static void drawBudgetAndReload(GameTestHelper helper) {
        var turbine = turbine(helper, POSITION, Direction.EAST);
        fill(turbine.steamTank(), fluid(FluidDefinition.STEAM), 1000);
        turbine.serverTick(helper.getLevel());
        var source = turbine.output(Direction.EAST);
        try (var tx = Transaction.openRoot()) {
            helper.assertValueEqual(
                    source.extract(1000, tx), 1000, "Requested KU is capped at the request");
        }
        helper.assertValueEqual(
                source.available(), 2000, "Aborted KU consumption restores paid work");
        try (var tx = Transaction.openRoot()) {
            source.extract(1000, tx);
            tx.commit();
        }
        var restored = replace(helper, turbine);
        restored.serverTick(helper.getLevel());
        helper.assertTrue(
                restored.steamTank().getAmountAsInt(0) == 0
                        && restored.output(Direction.EAST).available() == 1000,
                "Same-tick reload preserves both consumed steam and remaining KU");
        var retained = restored.output(Direction.EAST);
        restored.inventory().set(0, ItemResource.EMPTY, 0);
        helper.assertValueEqual(
                retained.available(), 0, "Removing the rotor disconnects a retained work view");
        restored.inventory().set(0, rotor(), 1);
        helper.setBlock(
                POSITION, restored.getBlockState().setValue(MachineBlock.FACING, Direction.WEST));
        helper.assertValueEqual(
                retained.available(), 0, "Retained work view follows live rotation");
        try (var tx = Transaction.openRoot()) {
            helper.assertValueEqual(
                    restored.output(Direction.WEST).extract(Integer.MAX_VALUE, tx),
                    1000,
                    "Rotation cannot multiply the remaining paid work");
            tx.commit();
        }
        helper.succeed();
    }

    static void condensateBacklog(GameTestHelper helper) {
        var turbine = turbine(helper, POSITION, Direction.EAST);
        var downstream = turbine(helper, POSITION.below(), Direction.EAST);
        helper.setBlock(POSITION.north(), ModMachines.block(MachineKind.CONDENSER));
        fill(turbine.steamTank(), fluid(FluidDefinition.STEAM), 21000);
        turbine.serverTick(helper.getLevel());
        helper.assertTrue(
                turbine.waterTank().getAmountAsInt(0) == 1 && turbine.condensedSteam() == 2000,
                "A large batch emits one water mB and retains the rest as steam credit");
        helper.assertTrue(
                downstream.steamTank().getAmountAsInt(0) == 0
                        && helper.getBlockEntity(POSITION.north(), CondenserBlockEntity.class)
                                        .inputTank()
                                        .getAmountAsInt(0)
                                == 18900,
                "Ordinary exhaust skips an earlier adjacent turbine and only enters the condenser");
        var restored = replace(helper, turbine);
        restored.inventory().set(0, ItemResource.EMPTY, 0);
        helper.runAfterDelay(
                22,
                () -> {
                    helper.assertTrue(
                            restored.waterTank().getAmountAsInt(0) == 21
                                    && restored.condensedSteam() == 0,
                            "Already condensed steam can finish one water mB per tick after reload,"
                                    + " without new steam or a rotor");
                    helper.succeed();
                });
    }

    static void throttleAndPorts(GameTestHelper helper) {
        var turbine = turbine(helper, POSITION, Direction.EAST);
        fill(turbine.waterTank(), fluid(FluidDefinition.DISTILLED_WATER), 999);
        fill(turbine.steamTank(), fluid(FluidDefinition.STEAM), 21000);
        turbine.serverTick(helper.getLevel());
        helper.assertValueEqual(
                turbine.menuValue(0), 42, "Water throttling uses exact integer fill fractions");
        try (var tx = Transaction.openRoot()) {
            helper.assertValueEqual(
                    turbine.fluidAutomation(Direction.UP)
                            .insert(1, FluidResource.of(Fluids.WATER), 1, tx),
                    0,
                    "Water tank cannot mix fluids or exceed capacity");
            helper.assertValueEqual(
                    turbine.fluidAutomation(Direction.UP)
                            .extract(0, fluid(FluidDefinition.STEAM), 1, tx),
                    0,
                    "Steam input is not extractable");
            helper.assertValueEqual(
                    turbine.inventory()
                            .insert(
                                    1,
                                    ItemResource.of(
                                            ModUpgrades.ALL.get(UpgradeItem.Kind.EJECTOR).get()),
                                    1,
                                    tx),
                    0,
                    "Valid rotors are not item outputs");
        }
        helper.runAfterDelay(
                2,
                () -> {
                    helper.assertTrue(
                            turbine.waterTank().getAmountAsInt(0) == 1000
                                    && turbine.condensedSteam() == 2000
                                    && turbine.menuValue(0) == 0,
                            "Full condensate tank pauses production and preserves the backlog");
                    helper.succeed();
                });
    }

    static void rotorAndDisabledMode(GameTestHelper helper) {
        var turbine = turbine(helper, POSITION, Direction.EAST);
        turbine.inventory().set(0, ItemResource.EMPTY, 0);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var menu = new MachineMenu(1, player.getInventory(), turbine);
        player.getInventory().setItem(9, rotor().toStack(64));
        menu.quickMoveStack(player, 2);
        helper.assertTrue(
                turbine.inventory().getAmountAsInt(0) == 1
                        && player.getInventory().getItem(9).getCount() == 63,
                "Only one ordinary steam turbine item fits the installed rotor slot");
        helper.assertTrue(
                !turbine.inventory().stack(0).isDamageableItem(),
                "Recovered steam turbine item has no durability; do not invent wear");
        try (var tx = Transaction.openRoot()) {
            helper.assertValueEqual(
                    turbine.inventory().insert(0, ItemResource.of(Items.DIAMOND), 1, tx),
                    0,
                    "Backend rejects unrelated rotor items");
        }
        fill(turbine.steamTank(), fluid(FluidDefinition.STEAM), 1000);
        double previous = GenerationConfig.STEAM_KINETIC.get();
        try {
            GenerationConfig.STEAM_KINETIC.set(0.0);
            turbine.serverTick(helper.getLevel());
            helper.assertTrue(
                    turbine.steamTank().getAmountAsInt(0) == 1000
                            && turbine.output(Direction.EAST).available() == 0,
                    "Disabled conversion preserves steam instead of consuming it for no work");
        } finally {
            GenerationConfig.STEAM_KINETIC.set(previous);
        }
        helper.succeed();
    }

    static void twoStageWaterLoop(GameTestHelper helper) {
        var boilerPos = new BlockPos(5, 2, 5);
        helper.setBlock(boilerPos, ModMachines.block(MachineKind.STEAM_GENERATOR));
        var boiler = helper.getBlockEntity(boilerPos, SteamGeneratorBlockEntity.class);
        var data = boiler.saveWithFullMetadata(helper.getLevel().registryAccess());
        data.putDouble("systemheat", 374);
        data.putInt("inputmb", 1);
        data.putInt("pressurevalve", 220);
        var initialized =
                (SteamGeneratorBlockEntity)
                        BlockEntity.loadStatic(
                                boiler.getBlockPos(),
                                boiler.getBlockState(),
                                data,
                                helper.getLevel().registryAccess());
        helper.getLevel().removeBlockEntity(boiler.getBlockPos());
        helper.getLevel().setBlockEntity(initialized);
        heater(helper, boilerPos.west(), Direction.EAST, boilerPos.west(2));
        heater(helper, boilerPos.above(), Direction.DOWN, boilerPos.above(2));
        var hotPos = boilerPos.east();
        var hot = turbine(helper, hotPos, Direction.SOUTH);
        var cold = turbine(helper, hotPos.north(), Direction.NORTH);
        helper.setBlock(hotPos.north().east(), ModMachines.block(MachineKind.CONDENSER));
        var hotConverter =
                converter(
                        helper, hotPos.south(), Direction.NORTH, hotPos.south(2), Direction.SOUTH);
        var coldConverter =
                converter(
                        helper, hotPos.north(2), Direction.SOUTH, hotPos.north(3), Direction.NORTH);
        fill(initialized.waterTank(), fluid(FluidDefinition.DISTILLED_WATER), 1000);
        initialized.serverTick(helper.getLevel());
        helper.runAtTickTime(200, () -> replace(helper, cold));
        helper.runAfterDelay(
                1100,
                () -> {
                    var restoredCold =
                            helper.getBlockEntity(hotPos.north(), SteamTurbineBlockEntity.class);
                    var condenser =
                            helper.getBlockEntity(
                                    hotPos.north().east(), CondenserBlockEntity.class);
                    helper.assertTrue(
                            initialized.waterTank().getAmountAsInt(0) == 0
                                    && hot.steamTank().getAmountAsInt(0) == 0
                                    && hot.waterTank().getAmountAsInt(0) == 0
                                    && restoredCold.steamTank().getAmountAsInt(0) == 0
                                    && restoredCold.condensedSteam() == 0
                                    && restoredCold.waterTank().getAmountAsInt(0) == 100
                                    && condenser.inputTank().getAmountAsInt(0) == 0
                                    && condenser.progress() == 0
                                    && condenser.outputTank().getAmountAsInt(0) == 900,
                            "Two-stage superheated steam cycle returns all 1000 water mB across a"
                                    + " turbine reload");
                    double electricity =
                            hotConverter.storedEnergy()
                                    + coldConverter.storedEnergy()
                                    + helper.getBlockEntity(
                                                    hotPos.south(2), EnergyStorageBlockEntity.class)
                                            .storedEnergy()
                                    + helper.getBlockEntity(
                                                    hotPos.north(3), EnergyStorageBlockEntity.class)
                                            .storedEnergy();
                    helper.assertTrue(
                            electricity > 0 && electricity <= 150000,
                            "Both real kinetic converters produce EU within the paid"
                                    + " six-KU-per-steam upper bound");
                    helper.assertValueEqual(
                            condenser.storedEnergy(),
                            0.0,
                            "Passive condenser is electrically isolated from the output networks");
                    helper.succeed();
                });
    }

    private static SteamTurbineBlockEntity turbine(
            GameTestHelper helper, BlockPos pos, Direction facing) {
        helper.setBlock(
                pos,
                ModMachines.block(MachineKind.STEAM_KINETIC_GENERATOR)
                        .defaultBlockState()
                        .setValue(MachineBlock.FACING, facing));
        var turbine = helper.getBlockEntity(pos, SteamTurbineBlockEntity.class);
        turbine.inventory().set(0, rotor(), 1);
        return turbine;
    }

    private static SteamTurbineBlockEntity replace(
            GameTestHelper helper, SteamTurbineBlockEntity turbine) {
        var restored =
                (SteamTurbineBlockEntity)
                        BlockEntity.loadStatic(
                                turbine.getBlockPos(),
                                turbine.getBlockState(),
                                turbine.saveWithFullMetadata(helper.getLevel().registryAccess()),
                                helper.getLevel().registryAccess());
        helper.getLevel().removeBlockEntity(turbine.getBlockPos());
        helper.getLevel().setBlockEntity(restored);
        return restored;
    }

    private static void heater(
            GameTestHelper helper, BlockPos pos, Direction facing, BlockPos supply) {
        helper.setBlock(
                pos,
                ModMachines.block(MachineKind.ELECTRIC_HEAT_GENERATOR)
                        .defaultBlockState()
                        .setValue(MachineBlock.FACING, facing));
        var heater = helper.getBlockEntity(pos, ElectricWorkBlockEntity.class);
        for (int slot = 0; slot < 10; slot++)
            heater.inventory()
                    .set(
                            slot,
                            ItemResource.of(ModItems.MATERIALS.get(MaterialDefinition.COIL).get()),
                            1);
        heater.energy().insert(10000);
        heater.serverTick(helper.getLevel());
        helper.setBlock(
                supply,
                ModMachines.block(MachineKind.MFE)
                        .defaultBlockState()
                        .setValue(MachineBlock.FACING, facing));
        helper.getBlockEntity(supply, EnergyStorageBlockEntity.class).energy().insert(100000);
    }

    private static WorkConversionBlockEntity converter(
            GameTestHelper helper,
            BlockPos pos,
            Direction facing,
            BlockPos storage,
            Direction output) {
        helper.setBlock(
                pos,
                ModMachines.block(MachineKind.KINETIC_GENERATOR)
                        .defaultBlockState()
                        .setValue(MachineBlock.FACING, facing));
        helper.setBlock(
                storage,
                ModMachines.block(MachineKind.MFE)
                        .defaultBlockState()
                        .setValue(MachineBlock.FACING, output));
        return helper.getBlockEntity(pos, WorkConversionBlockEntity.class);
    }

    private static ItemResource rotor() {
        return ItemResource.of(ModItems.MATERIALS.get(MaterialDefinition.STEAM_TURBINE).get());
    }

    private static FluidResource fluid(FluidDefinition definition) {
        return FluidResource.of(ModFluids.FAMILIES.get(definition).source().get());
    }

    private static void fill(
            ic2.neoforge.transfer.MachineFluidTank tank, FluidResource fluid, int amount) {
        try (var tx = Transaction.openRoot()) {
            if (tank.insert(0, fluid, amount, tx) != amount)
                throw new IllegalStateException("Test tank cannot fit input");
            tx.commit();
        }
    }

    private SteamTurbineTests() {}
}
