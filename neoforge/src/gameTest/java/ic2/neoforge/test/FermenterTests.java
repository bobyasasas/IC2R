package ic2.neoforge.test;

import ic2.neoforge.fluid.FluidDefinition;
import ic2.neoforge.item.UpgradeItem;
import ic2.neoforge.machine.ElectricWorkBlockEntity;
import ic2.neoforge.machine.FermenterBlockEntity;
import ic2.neoforge.machine.MachineBlock;
import ic2.neoforge.machine.MachineKind;
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

final class FermenterTests {
    private static final BlockPos POSITION = new BlockPos(3, 1, 2);

    static void heatChainAndReload(GameTestHelper helper) {
        var fermenter = machine(helper);
        var heater = heater(helper, 4000);
        fill(fermenter, FluidDefinition.BIOMASS, 20);
        helper.startSequence()
                .thenWaitUntil(
                        () ->
                                helper.assertTrue(
                                        fermenter.fuelRemaining() >= 1000,
                                        "The fermenter must accumulate paid heat"))
                .thenExecute(
                        () -> {
                            var restored = restore(helper, fermenter);
                            helper.assertTrue(
                                    restored.fuelRemaining() == fermenter.fuelRemaining()
                                            && restored.inputTank().getAmountAsInt(0) == 20,
                                    "Reload must retain heat and unconsumed biomass");
                            helper.getLevel().setBlockEntity(restored);
                        })
                .thenWaitUntil(
                        () ->
                                helper.assertTrue(
                                        helper.getBlockEntity(POSITION, FermenterBlockEntity.class)
                                                        .outputTank()
                                                        .getAmountAsInt(0)
                                                == 400,
                                        "A default batch must produce exactly 400 mB biogas"))
                .thenExecute(
                        () -> {
                            var restored =
                                    helper.getBlockEntity(POSITION, FermenterBlockEntity.class);
                            helper.assertTrue(
                                    restored.inputTank().getAmountAsInt(0) == 0
                                            && restored.fuelRemaining() == 0
                                            && restored.progress() == 20
                                            && heater.energy().stored() == 0
                                            && heater.menuValue(0) == 0,
                                    "The batch must conserve all 4000 HU and 20 mB biomass across"
                                        + " reload");
                        })
                .thenSucceed();
    }

    static void blockedGas(GameTestHelper helper) {
        var fermenter = machine(helper);
        var heater = heater(helper, 4000);
        fill(fermenter, FluidDefinition.BIOMASS, 20);
        try (var transaction = Transaction.openRoot()) {
            fermenter.outputTank().insert(0, fluid(FluidDefinition.BIOGAS), 2000, transaction);
            transaction.commit();
        }
        helper.runAtTickTime(
                10,
                () -> {
                    helper.assertTrue(
                            fermenter.fuelRemaining() == 0
                                    && fermenter.inputTank().getAmountAsInt(0) == 20
                                    && heater.energy().stored() + heater.menuValue(0) == 4000,
                            "A full gas output must not consume biomass or paid heat");
                    helper.succeed();
                });
    }

    static void blockedFertilizer(GameTestHelper helper) {
        var fermenter = machine(helper);
        fill(fermenter, FluidDefinition.BIOMASS, 20);
        var tag = fermenter.saveWithFullMetadata(helper.getLevel().registryAccess());
        tag.putInt("heat", 4000);
        tag.putInt("processed", 480);
        var restored =
                (FermenterBlockEntity)
                        BlockEntity.loadStatic(
                                fermenter.getBlockPos(),
                                fermenter.getBlockState(),
                                tag,
                                helper.getLevel().registryAccess());
        restored.setLevel(helper.getLevel());
        var fertilizer =
                ItemResource.of(ModItems.MATERIALS.get(MaterialDefinition.FERTILIZER).get());
        restored.inventory().set(FermenterBlockEntity.FERTILIZER, fertilizer, 64);
        restored.serverTick(helper.getLevel());
        helper.assertTrue(
                restored.fuelRemaining() == 4000
                        && restored.inputTank().getAmountAsInt(0) == 20
                        && restored.outputTank().getAmountAsInt(0) == 0
                        && restored.progress() == 480,
                "A blocked fertilizer slot must roll back the entire conversion");
        restored.inventory().set(FermenterBlockEntity.FERTILIZER, ItemResource.EMPTY, 0);
        restored.serverTick(helper.getLevel());
        helper.assertTrue(
                restored.fuelRemaining() == 0
                        && restored.outputTank().getAmountAsInt(0) == 400
                        && restored.progress() == 0
                        && restored.inventory().getAmountAsInt(FermenterBlockEntity.FERTILIZER)
                                == 1,
                "Clearing fertilizer output spends saved heat exactly once, even without an"
                    + " attached heater");
        helper.succeed();
    }

    static void containersAndPorts(GameTestHelper helper) {
        var fermenter = machine(helper);
        fermenter
                .inventory()
                .set(
                        0,
                        ItemResource.of(
                                ModFluids.FAMILIES.get(FluidDefinition.BIOMASS).bucket().get()),
                        1);
        fermenter.inventory().set(2, ItemResource.of(Items.BUCKET), 1);
        try (var transaction = Transaction.openRoot()) {
            fermenter.outputTank().insert(0, fluid(FluidDefinition.BIOGAS), 1000, transaction);
            transaction.commit();
        }
        fermenter.serverTick(helper.getLevel());
        helper.assertTrue(
                fermenter.inputTank().getAmountAsInt(0) == 1000
                        && fermenter.inventory().stack(1).is(Items.BUCKET)
                        && fermenter.outputTank().getAmountAsInt(0) == 0
                        && fermenter
                                .inventory()
                                .stack(3)
                                .is(ModFluids.FAMILIES.get(FluidDefinition.BIOGAS).bucket().get()),
                "Fluid containers must move through separate input and return slots without"
                    + " duplication");
        try (var transaction = Transaction.openRoot()) {
            var port = fermenter.fluidAutomation(Direction.NORTH);
            helper.assertTrue(
                    port.insert(0, FluidResource.of(Fluids.LAVA), 1000, transaction) == 0
                            && port.extract(0, fluid(FluidDefinition.BIOMASS), 1000, transaction)
                                    == 0
                            && port.insert(1, fluid(FluidDefinition.BIOGAS), 1000, transaction)
                                    == 0,
                    "Fluid capabilities must reject non-recipes, input extraction and output"
                        + " insertion");
        }
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var menu = new MachineMenu(1, player.getInventory(), fermenter);
        helper.assertTrue(
                menu.getSlot(5)
                                .mayPlace(
                                        ModUpgrades.ALL
                                                .get(UpgradeItem.Kind.FLUID_EJECTOR)
                                                .get()
                                                .getDefaultInstance())
                        && !menu.getSlot(5)
                                .mayPlace(
                                        ModUpgrades.ALL
                                                .get(UpgradeItem.Kind.OVERCLOCKER)
                                                .get()
                                                .getDefaultInstance()),
                "Fermenters accept transfer upgrades and reject electrical upgrades");
        helper.succeed();
    }

    static void dataPackRecipe(GameTestHelper helper) {
        var fermenter = machine(helper);
        heater(helper, 120);
        fill(fermenter, FluidDefinition.DISTILLED_WATER, 3);
        helper.startSequence()
                .thenWaitUntil(
                        () ->
                                helper.assertTrue(
                                        fermenter.outputTank().getAmountAsInt(0) == 6,
                                        "A test datapack must define an additional fluid conversion"
                                            + " without code changes"))
                .thenExecute(
                        () ->
                                helper.assertTrue(
                                        fermenter.inputTank().getAmountAsInt(0) == 0
                                                && fermenter.fuelRemaining() == 0
                                                && fermenter.progress() == 0
                                                && fermenter.inventory().getAmountAsInt(4) == 1,
                                        "Custom heat cost, fluid ratio and fertilizer interval must"
                                            + " all apply"))
                .thenSucceed();
    }

    static void biofuelChain(GameTestHelper helper) {
        var fermenter = machine(helper);
        var heater = heater(helper, 4000);
        fill(fermenter, FluidDefinition.BIOMASS, 20);
        helper.setBlock(POSITION.north(), ModMachines.block(MachineKind.SEMIFLUID_GENERATOR));
        helper.setBlock(POSITION.north().east(), ModMachines.block(MachineKind.MFE));
        var generator =
                helper.getBlockEntity(
                        POSITION.north(), ic2.neoforge.machine.FluidGeneratorBlockEntity.class);
        var storage =
                helper.getBlockEntity(
                        POSITION.north().east(),
                        ic2.neoforge.machine.EnergyStorageBlockEntity.class);
        var ejector =
                ModUpgrades.ALL.get(UpgradeItem.Kind.FLUID_EJECTOR).get().getDefaultInstance();
        ejector.set(ic2.neoforge.component.ModDataComponents.UPGRADE_DIRECTION, Direction.NORTH);
        fermenter.inventory().set(5, ItemResource.of(ejector), 1);
        helper.startSequence()
                .thenWaitUntil(
                        () ->
                                helper.assertTrue(
                                        storage.energy().stored() == 6400,
                                        "20 mB biomass and 4000 HU must yield 400 mB biogas, then"
                                            + " 6400 EU through native fluid upgrades and the real"
                                            + " electrical network"))
                .thenExecute(
                        () ->
                                helper.assertTrue(
                                        fermenter.outputTank().getAmountAsInt(0) == 0
                                                && generator.tank().getAmountAsInt(0) == 0
                                                && generator.energy().stored() == 0
                                                && heater.energy().stored() == 0
                                                && fermenter.fuelRemaining() == 0,
                                        "The complete biofuel chain must consume its inputs without"
                                            + " stranded fluid or energy"))
                .thenSucceed();
    }

    private static FluidResource fluid(FluidDefinition definition) {
        return FluidResource.of(ModFluids.FAMILIES.get(definition).source().get());
    }

    private static void fill(FermenterBlockEntity fermenter, FluidDefinition fluid, int amount) {
        try (var transaction = Transaction.openRoot()) {
            fermenter.inputTank().insert(0, fluid(fluid), amount, transaction);
            transaction.commit();
        }
    }

    private static FermenterBlockEntity machine(GameTestHelper helper) {
        helper.setBlock(
                POSITION,
                ModMachines.block(MachineKind.FERMENTER)
                        .defaultBlockState()
                        .setValue(MachineBlock.FACING, Direction.WEST));
        return helper.getBlockEntity(POSITION, FermenterBlockEntity.class);
    }

    private static ElectricWorkBlockEntity heater(GameTestHelper helper, int energy) {
        helper.setBlock(
                POSITION.west(),
                ModMachines.block(MachineKind.ELECTRIC_HEAT_GENERATOR)
                        .defaultBlockState()
                        .setValue(MachineBlock.FACING, Direction.EAST));
        var heater = helper.getBlockEntity(POSITION.west(), ElectricWorkBlockEntity.class);
        for (int slot = 0; slot < 10; slot++)
            heater.inventory()
                    .set(
                            slot,
                            ItemResource.of(ModItems.MATERIALS.get(MaterialDefinition.COIL).get()),
                            1);
        heater.energy().insert(energy);
        return heater;
    }

    private static FermenterBlockEntity restore(
            GameTestHelper helper, FermenterBlockEntity fermenter) {
        var restored =
                (FermenterBlockEntity)
                        BlockEntity.loadStatic(
                                fermenter.getBlockPos(),
                                fermenter.getBlockState(),
                                fermenter.saveWithFullMetadata(helper.getLevel().registryAccess()),
                                helper.getLevel().registryAccess());
        restored.setLevel(helper.getLevel());
        return restored;
    }

    private FermenterTests() {}
}
