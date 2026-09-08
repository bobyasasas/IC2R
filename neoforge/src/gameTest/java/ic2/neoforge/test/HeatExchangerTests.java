package ic2.neoforge.test;

import ic2.neoforge.fluid.FluidDefinition;
import ic2.neoforge.item.UpgradeItem;
import ic2.neoforge.machine.EnergyStorageBlockEntity;
import ic2.neoforge.machine.LiquidHeatExchangerBlockEntity;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

final class HeatExchangerTests {
    private static final BlockPos POSITION = new BlockPos(2, 1, 2);

    static void nearlyFullOutput(GameTestHelper helper) {
        var machine = machine(helper, 10);
        try (var transaction = Transaction.openRoot()) {
            machine.inputTank().insert(0, fluid(FluidDefinition.HOT_COOLANT), 100, transaction);
            machine.outputTank().insert(0, fluid(FluidDefinition.COOLANT), 1999, transaction);
            transaction.commit();
        }
        machine.serverTick(helper.getLevel());
        helper.assertTrue(
                machine.inputTank().getAmountAsInt(0) == 99
                        && machine.outputTank().getAmountAsInt(0) == 2000
                        && machine.fuelRemaining() == 20,
                "One mB of output space must convert exactly one mB and 20 HU, never drain twenty"
                    + " mB");
        machine.serverTick(helper.getLevel());
        helper.assertTrue(
                machine.inputTank().getAmountAsInt(0) == 99 && machine.fuelRemaining() == 20,
                "A full cooling output must pause conversion");
        helper.succeed();
    }

    static void partsAndBudget(GameTestHelper helper) {
        var machine = machine(helper, 0);
        var conductor = ModItems.MATERIALS.get(MaterialDefinition.HEAT_CONDUCTOR).get();
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var menu = new MachineMenu(1, player.getInventory(), machine);
        player.getInventory().setItem(9, new ItemStack(conductor, 10));
        menu.quickMoveStack(player, 17);
        for (int slot = 4; slot < 14; slot++)
            helper.assertTrue(
                    machine.inventory().getAmountAsInt(slot) == 1,
                    "Shift installation must distribute ten single conductors");
        for (int slot = 5; slot < 14; slot++) machine.inventory().set(slot, ItemResource.EMPTY, 0);
        try (var transaction = Transaction.openRoot()) {
            machine.inputTank().insert(0, FluidResource.of(Fluids.LAVA), 10, transaction);
            helper.assertTrue(
                    machine.inventory().insert(5, ItemResource.of(Items.DIAMOND), 1, transaction)
                            == 0,
                    "Only heat conductors can enter installed-part slots");
            transaction.commit();
        }
        machine.serverTick(helper.getLevel());
        helper.assertTrue(
                machine.fuelRemaining() == 0 && machine.inputTank().getAmountAsInt(0) == 10,
                "One 10-HU conductor cannot cool a whole 20-HU millibucket");
        machine.inventory().set(5, ItemResource.of(conductor), 1);
        machine.serverTick(helper.getLevel());
        helper.assertTrue(
                machine.fuelRemaining() == 20 && machine.inputTank().getAmountAsInt(0) == 9,
                "Two conductors allow one whole-mB cooling operation");
        var retained = machine.output(Direction.EAST);
        try (var transaction = Transaction.openRoot()) {
            retained.extract(20, transaction);
        }
        helper.assertTrue(
                retained.available() == 20 && machine.output(Direction.WEST).available() == 0,
                "Heat is front-sided and aborted draws restore the budget");
        try (var transaction = Transaction.openRoot()) {
            retained.extract(10, transaction);
            transaction.commit();
        }
        machine.inventory().set(5, ItemResource.EMPTY, 0);
        var restored =
                (LiquidHeatExchangerBlockEntity)
                        BlockEntity.loadStatic(
                                machine.getBlockPos(),
                                machine.getBlockState(),
                                machine.saveWithFullMetadata(helper.getLevel().registryAccess()),
                                helper.getLevel().registryAccess());
        restored.setLevel(helper.getLevel());
        helper.assertTrue(
                restored.fuelRemaining() == 10 && restored.output(Direction.EAST).available() == 0,
                "Removing a conductor and reloading must retain paid heat without resetting this"
                    + " tick's ten-HU allowance");
        helper.setBlock(
                POSITION, machine.getBlockState().setValue(MachineBlock.FACING, Direction.WEST));
        helper.assertTrue(
                retained.available() == 0,
                "Retained heat capabilities must follow live facing changes");
        helper.succeed();
    }

    static void stirlingChain(GameTestHelper helper) {
        var machine = machine(helper, 10);
        try (var transaction = Transaction.openRoot()) {
            machine.inputTank().insert(0, fluid(FluidDefinition.HOT_COOLANT), 128, transaction);
            transaction.commit();
        }
        helper.setBlock(
                POSITION.east(),
                ModMachines.block(MachineKind.STIRLING_GENERATOR)
                        .defaultBlockState()
                        .setValue(MachineBlock.FACING, Direction.WEST));
        helper.setBlock(POSITION.east(2), ModMachines.block(MachineKind.MFE));
        var storage = helper.getBlockEntity(POSITION.east(2), EnergyStorageBlockEntity.class);
        helper.startSequence()
                .thenWaitUntil(
                        () ->
                                helper.assertTrue(
                                        storage.energy().stored() == 1280,
                                        "128 mB hot coolant must supply 2560 HU and exactly 1280 EU"
                                            + " through the real Stirling network"))
                .thenExecute(
                        () ->
                                helper.assertTrue(
                                        machine.inputTank().getAmountAsInt(0) == 0
                                                && machine.outputTank().getAmountAsInt(0) == 128
                                                && machine.fuelRemaining() == 0,
                                        "The complete cooling chain must preserve fluid volume and"
                                            + " spend all heat"))
                .thenSucceed();
    }

    static void containersAndPorts(GameTestHelper helper) {
        var machine = machine(helper, 0);
        machine.inventory().set(0, ItemResource.of(Items.LAVA_BUCKET), 1);
        machine.inventory().set(2, ItemResource.of(Items.BUCKET), 1);
        try (var transaction = Transaction.openRoot()) {
            machine.outputTank().insert(0, fluid(FluidDefinition.PAHOEHOE_LAVA), 1000, transaction);
            transaction.commit();
        }
        machine.serverTick(helper.getLevel());
        helper.assertTrue(
                machine.inputTank().getAmountAsInt(0) == 1000
                        && machine.inventory().stack(1).is(Items.BUCKET)
                        && machine.outputTank().getAmountAsInt(0) == 0
                        && machine.inventory()
                                .stack(3)
                                .is(
                                        ModFluids.FAMILIES
                                                .get(FluidDefinition.PAHOEHOE_LAVA)
                                                .bucket()
                                                .get()),
                "Hot and cooled containers must return through separate atomic item slots");
        try (var transaction = Transaction.openRoot()) {
            var port = machine.fluidAutomation(Direction.UP);
            helper.assertTrue(
                    port.insert(0, FluidResource.of(Fluids.WATER), 1, transaction) == 0
                            && port.insert(1, fluid(FluidDefinition.PAHOEHOE_LAVA), 1, transaction)
                                    == 0
                            && port.extract(0, FluidResource.of(Fluids.LAVA), 1, transaction) == 0,
                    "Cooling capabilities must enforce recipe and flow direction restrictions");
            helper.assertTrue(
                    machine.automation(Direction.UP)
                                    .insert(
                                            4,
                                            ItemResource.of(
                                                    ModItems.MATERIALS
                                                            .get(MaterialDefinition.HEAT_CONDUCTOR)
                                                            .get()),
                                            1,
                                            transaction)
                            == 0,
                    "External automation cannot replace installed conductors");
        }
        var menu =
                new MachineMenu(
                        1, helper.makeMockPlayer(GameType.SURVIVAL).getInventory(), machine);
        helper.assertTrue(
                menu.getSlot(14)
                                .mayPlace(
                                        ModUpgrades.ALL
                                                .get(UpgradeItem.Kind.FLUID_EJECTOR)
                                                .get()
                                                .getDefaultInstance())
                        && !menu.getSlot(14)
                                .mayPlace(
                                        ModUpgrades.ALL
                                                .get(UpgradeItem.Kind.OVERCLOCKER)
                                                .get()
                                                .getDefaultInstance()),
                "The exchanger's three upgrade slots accept transfer upgrades only");
        helper.succeed();
    }

    static void dataPackRecipe(GameTestHelper helper) {
        var machine = machine(helper, 2);
        try (var transaction = Transaction.openRoot()) {
            machine.inputTank().insert(0, fluid(FluidDefinition.STEAM), 5, transaction);
            transaction.commit();
        }
        machine.serverTick(helper.getLevel());
        helper.assertTrue(
                machine.inputTank().getAmountAsInt(0) == 1
                        && machine.outputTank().getAmountAsInt(0) == 4
                        && machine.outputTank()
                                .getResource(0)
                                .equals(fluid(FluidDefinition.DISTILLED_WATER))
                        && machine.fuelRemaining() == 20,
                "Test datapacks can supply a distinct five-HU-per-mB cooling recipe");
        helper.succeed();
    }

    private static FluidResource fluid(FluidDefinition definition) {
        return FluidResource.of(ModFluids.FAMILIES.get(definition).source().get());
    }

    private static LiquidHeatExchangerBlockEntity machine(GameTestHelper helper, int parts) {
        helper.setBlock(
                POSITION,
                ModMachines.block(MachineKind.LIQUID_HEAT_EXCHANGER)
                        .defaultBlockState()
                        .setValue(MachineBlock.FACING, Direction.EAST));
        var machine = helper.getBlockEntity(POSITION, LiquidHeatExchangerBlockEntity.class);
        for (int slot = 4; slot < 4 + parts; slot++)
            machine.inventory()
                    .set(
                            slot,
                            ItemResource.of(
                                    ModItems.MATERIALS
                                            .get(MaterialDefinition.HEAT_CONDUCTOR)
                                            .get()),
                            1);
        return machine;
    }

    private HeatExchangerTests() {}
}
