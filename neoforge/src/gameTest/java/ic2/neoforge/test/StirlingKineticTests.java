package ic2.neoforge.test;

import ic2.neoforge.fluid.FluidDefinition;
import ic2.neoforge.item.UpgradeItem;
import ic2.neoforge.machine.ElectricWorkBlockEntity;
import ic2.neoforge.machine.EnergyStorageBlockEntity;
import ic2.neoforge.machine.MachineBlock;
import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.machine.StirlingKineticGeneratorBlockEntity;
import ic2.neoforge.menu.MachineMenu;
import ic2.neoforge.registration.MaterialDefinition;
import ic2.neoforge.registration.ModFluids;
import ic2.neoforge.registration.ModItems;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.registration.ModUpgrades;

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

final class StirlingKineticTests {
    private static final BlockPos POSITION = new BlockPos(2, 1, 2);

    static void heatsFluidsAndBanksKu(GameTestHelper helper) {
        var machine = machine(helper);
        var coil = source(helper, POSITION.west());
        try (var transaction = Transaction.openRoot()) {
            machine.inputTank().insert(0, FluidResource.of(Fluids.WATER), 100, transaction);
            transaction.commit();
        }
        var level = helper.getLevel();
        // Ten coils give the coil a 100 HU same-tick budget, so ten manual ticks inside one
        // game tick still draw the full ten HU per conversion tick.
        for (int tick = 0; tick < 10; tick++) {
            coil.serverTick(level);
            machine.serverTick(level);
        }
        helper.assertTrue(
                machine.inputTank().getAmountAsInt(0) == 99
                        && machine.outputTank().getAmountAsInt(0) == 1
                        && machine.outputTank()
                                .getResource(0)
                                .equals(fluid(FluidDefinition.HOT_WATER)),
                "Pulled heat must convert water into hot water one whole millibucket at a time");
        helper.assertTrue(
                machine.fuelRemaining() == 300,
                "Ten HU per tick must bank twelve KU per four HU");
        helper.assertTrue(
                machine.output(Direction.EAST).available() == 300,
                "Banked KU is offered through the facing");
        helper.assertTrue(
                machine.output(Direction.WEST).available() == 0,
                "The heat-draw side must never offer KU");
        var restored =
                (StirlingKineticGeneratorBlockEntity)
                        BlockEntity.loadStatic(
                                machine.getBlockPos(),
                                machine.getBlockState(),
                                machine.saveWithFullMetadata(level.registryAccess()),
                                level.registryAccess());
        restored.setLevel(level);
        helper.assertTrue(
                restored.fuelRemaining() == 300
                        && restored.inputTank().getAmountAsInt(0) == 99
                        && restored
                                .outputTank()
                                .getResource(0)
                                .equals(fluid(FluidDefinition.HOT_WATER)),
                "Reload must preserve KU, liquid heat accounting and both tanks");
        helper.succeed();
    }

    static void containersRouteBothWays(GameTestHelper helper) {
        var machine = machine(helper);
        try (var transaction = Transaction.openRoot()) {
            var port = machine.fluidAutomation(Direction.UP);
            helper.assertTrue(
                    port.insert(0, FluidResource.of(Fluids.WATER), 1, transaction) == 1,
                    "The input tank accepts water through the port");
            helper.assertTrue(
                    port.extract(0, FluidResource.of(Fluids.WATER), 1, transaction) == 0
                            && machine
                                    .inputTank()
                                    .extract(0, FluidResource.of(Fluids.WATER), 1, transaction)
                                    == 1,
                    "The cold side never releases through the port, only direct drains");
            helper.assertTrue(
                    port.insert(0, fluid(FluidDefinition.COOLANT), 1, transaction) == 1,
                    "The input tank accepts coolant, the other heatable fluid");
            helper.assertTrue(
                    port.insert(0, fluid(FluidDefinition.HOT_WATER), 1, transaction) == 0,
                    "The input tank rejects fluids with no heating recipe");
            helper.assertTrue(
                    port.insert(1, FluidResource.of(Fluids.WATER), 1, transaction) == 0,
                    "The output tank cannot be filled through the port");
            machine
                    .inputTank()
                    .extract(0, fluid(FluidDefinition.COOLANT), 1, transaction);
            transaction.commit();
        }
        try (var transaction = Transaction.openRoot()) {
            var port = machine.fluidAutomation(Direction.UP);
            machine.outputTank().insert(0, fluid(FluidDefinition.HOT_WATER), 1000, transaction);
            helper.assertTrue(
                    port.extract(1, fluid(FluidDefinition.HOT_WATER), 1, transaction) == 1,
                    "The output tank only releases");
            machine.outputTank().insert(0, fluid(FluidDefinition.HOT_WATER), 1, transaction);
            transaction.commit();
        }
        machine.inventory().set(0, ItemResource.of(Items.WATER_BUCKET), 1);
        machine.inventory().set(2, ItemResource.of(Items.BUCKET), 1);
        machine.serverTick(helper.getLevel());
        helper.assertTrue(
                machine.inputTank().getAmountAsInt(0) == 1000
                        && machine.inventory().stack(1).is(Items.BUCKET)
                        && machine.outputTank().getAmountAsInt(0) == 0
                        && machine.inventory()
                                .stack(3)
                                .is(
                                        ModFluids.FAMILIES
                                                .get(FluidDefinition.HOT_WATER)
                                                .bucket()
                                                .get()),
                "Water drains in on the cold side while the hot side fills buckets from the"
                    + " output tank");
        var menu = new MachineMenu(1, helper.makeMockPlayer(GameType.SURVIVAL).getInventory(), machine);
        helper.assertTrue(
                menu.getSlot(4)
                                .mayPlace(
                                        ModUpgrades.ALL
                                                .get(UpgradeItem.Kind.FLUID_EJECTOR)
                                                .get()
                                                .getDefaultInstance())
                        && menu.getSlot(4)
                                .mayPlace(
                                        ModUpgrades.ALL
                                                .get(UpgradeItem.Kind.FLUID_PULLING)
                                                .get()
                                                .getDefaultInstance())
                        && !menu.getSlot(4)
                                .mayPlace(
                                        ModUpgrades.ALL
                                                .get(UpgradeItem.Kind.OVERCLOCKER)
                                                .get()
                                                .getDefaultInstance()),
                "The three upgrade slots accept transfer upgrades only, matching legacy"
                    + " consuming/producing properties");
        helper.succeed();
    }

    static void chainChargesBatbox(GameTestHelper helper) {
        var machine = machine(helper);
        source(helper, POSITION.west());
        try (var transaction = Transaction.openRoot()) {
            machine.inputTank().insert(0, FluidResource.of(Fluids.WATER), 100, transaction);
            transaction.commit();
        }
        helper.setBlock(
                POSITION.east(),
                ModMachines.block(MachineKind.KINETIC_GENERATOR)
                        .defaultBlockState()
                        .setValue(MachineBlock.FACING, Direction.WEST));
        helper.setBlock(POSITION.east(2), ModMachines.block(MachineKind.BATBOX));
        var storage = helper.getBlockEntity(POSITION.east(2), EnergyStorageBlockEntity.class);
        helper.startSequence()
                .thenWaitUntil(
                        () ->
                                helper.assertTrue(
                                        storage.energy().stored() >= 64,
                                        "Pulled heat must reach a real BatBox through the"
                                            + " KU-to-EU converter in both network modes"))
                .thenExecute(
                        () ->
                                helper.assertTrue(
                                        machine.inputTank().getAmountAsInt(0) < 100,
                                        "Charging the BatBox must have heated real water"))
                .thenSucceed();
    }

    private static FluidResource fluid(FluidDefinition definition) {
        return FluidResource.of(ModFluids.FAMILIES.get(definition).source().get());
    }

    private static StirlingKineticGeneratorBlockEntity machine(GameTestHelper helper) {
        helper.setBlock(
                POSITION,
                ModMachines.block(MachineKind.STIRLING_KINETIC_GENERATOR)
                        .defaultBlockState()
                        .setValue(MachineBlock.FACING, Direction.EAST));
        return helper.getBlockEntity(POSITION, StirlingKineticGeneratorBlockEntity.class);
    }

    private static ElectricWorkBlockEntity source(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(
                pos,
                ModMachines.block(MachineKind.ELECTRIC_HEAT_GENERATOR)
                        .defaultBlockState()
                        .setValue(MachineBlock.FACING, Direction.EAST));
        var coil = helper.getBlockEntity(pos, ElectricWorkBlockEntity.class);
        var part =
                ItemResource.of(
                        ModItems.MATERIALS
                                .get(
                                        ElectricWorkBlockEntity.part(
                                                MachineKind.ELECTRIC_HEAT_GENERATOR))
                                .get());
        for (int slot = 0; slot < ElectricWorkBlockEntity.PARTS; slot++)
            coil.inventory().set(slot, part, 1);
        coil.energy().insert(1000);
        return coil;
    }

    private StirlingKineticTests() {}
}
