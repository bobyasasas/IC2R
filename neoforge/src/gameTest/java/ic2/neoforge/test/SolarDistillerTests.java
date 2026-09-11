package ic2.neoforge.test;

import ic2.neoforge.fluid.FluidDefinition;
import ic2.neoforge.machine.MachineBlock;
import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.machine.SolarDistillerBlockEntity;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.menu.MachineMenu;
import ic2.neoforge.registration.ModFluids;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

final class SolarDistillerTests {
    private static final BlockPos POSITION = new BlockPos(2, 1, 2);
    private static final BlockPos OPEN_SKY = new BlockPos(8, 1, 8);

    static void specs(GameTestHelper helper) {
        var machine = machine(helper);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var menu = new MachineMenu(1, player.getInventory(), machine);
        var client =
                new MachineMenu(1, player.getInventory(), machine.getBlockPos(), machine.kind());
        MenuTestLink.connect(menu, client);
        helper.assertTrue(
                client.slots.size() == 42,
                "Menu carries four container slots, two upgrade slots and the player inventory");
        for (var side : Direction.values()) {
            var port =
                    helper.getLevel()
                            .getCapability(Capabilities.Fluid.BLOCK, machine.getBlockPos(), side);
            helper.assertTrue(port != null, "Native sided fluid capability is registered");
            try (var transaction = Transaction.openRoot()) {
                helper.assertValueEqual(
                        port.insert(
                                0,
                                fluid(FluidDefinition.DISTILLED_WATER),
                                SolarDistillerBlockEntity.CAPACITY_MB,
                                transaction),
                        0,
                        "Only water enters the still");
                helper.assertValueEqual(
                        port.insert(0, FluidResource.of(Fluids.WATER), 10, transaction),
                        10,
                        "Water fills the 10,000 mB input tank");
                helper.assertValueEqual(
                        port.insert(1, fluid(FluidDefinition.DISTILLED_WATER), 1, transaction),
                        0,
                        "Output tank is extract-only");
                helper.assertValueEqual(
                        port.extract(1, fluid(FluidDefinition.DISTILLED_WATER), 1, transaction),
                        0,
                        "Fresh stills hold nothing to drain");
            }
        }
        try (var transaction = Transaction.openRoot()) {
            helper.assertValueEqual(
                    machine.inventory()
                            .insert(0, ItemResource.of(Items.DIRT), 1, transaction),
                    0,
                    "Inlet slots take fluid containers only");
            helper.assertValueEqual(
                    machine.inventory()
                            .insert(
                                    4,
                                    ItemResource.of(
                                            new ItemStack(
                                                    ic2.neoforge.registration.ModUpgrades.ALL
                                                            .get(
                                                                    ic2.neoforge.item.UpgradeItem.Kind.TRANSFORMER)
                                                            .get())),
                                    1,
                                    transaction),
                    1,
                    "Transformer upgrade seats in the first legacy upgrade slot");
        }
        fill(machine.inputTank(), FluidResource.of(Fluids.WATER), 77);
        var restored =
                (SolarDistillerBlockEntity)
                        BlockEntity.loadStatic(
                                machine.getBlockPos(),
                                machine.getBlockState(),
                                machine.saveWithFullMetadata(helper.getLevel().registryAccess()),
                                helper.getLevel().registryAccess());
        helper.getLevel().removeBlockEntity(machine.getBlockPos());
        helper.getLevel().setBlockEntity(restored);
        helper.assertValueEqual(
                restored.inputTank().getAmountAsInt(0),
                77,
                "Tank contents survive a real block entity replacement");
        helper.succeed();
    }

    static void daylight(GameTestHelper helper) {
        helper.setTime(6000);
        helper.setBlock(OPEN_SKY, ModMachines.block(MachineKind.SOLAR_DISTILLER));
        var machine = helper.getBlockEntity(OPEN_SKY, SolarDistillerBlockEntity.class);
        fill(machine.inputTank(), FluidResource.of(Fluids.WATER), 100);
        // The block ticker keeps running while sequences wait, so every phase pins the delta
        // over its own work period instead of absolute tank levels.
        int[] before = {0};
        helper.startSequence()
                .thenWaitUntil(
                        () -> {
                            machine.updateSunVisibility(helper.getLevel());
                            helper.assertTrue(
                                    machine.progress() > 900, "Daylight must reach the still");
                        })
                .thenExecute(
                        () -> {
                            before[0] = machine.outputTank().getAmountAsInt(0);
                            runPeriod(helper, machine);
                            helper.assertTrue(
                                    machine.outputTank().getAmountAsInt(0) == before[0] + 1,
                                    "One full work period distils exactly one millibucket");
                        })
                .thenExecute(() -> helper.setTime(18000))
                .thenWaitUntil(
                        () -> {
                            machine.updateSunVisibility(helper.getLevel());
                            helper.assertTrue(machine.progress() == 0, "Night must stop the still");
                        })
                .thenExecute(
                        () -> {
                            before[0] = machine.outputTank().getAmountAsInt(0);
                            runPeriod(helper, machine);
                            helper.assertTrue(
                                    machine.outputTank().getAmountAsInt(0) == before[0],
                                    "Darkness produces nothing but keeps the water");
                        })
                .thenExecute(() -> helper.setTime(6000))
                .thenExecute(() -> helper.setBlock(OPEN_SKY.above(), Blocks.STONE))
                .thenWaitUntil(
                        () -> {
                            machine.updateSunVisibility(helper.getLevel());
                            helper.assertTrue(
                                    machine.progress() == 0,
                                    "Opaque cover must remove solar input");
                        })
                .thenExecute(
                        () -> {
                            before[0] = machine.outputTank().getAmountAsInt(0);
                            runPeriod(helper, machine);
                            helper.assertTrue(
                                    machine.outputTank().getAmountAsInt(0) == before[0],
                                    "A shaded still waits without draining water");
                            helper.setBlock(OPEN_SKY.above(), Blocks.AIR);
                        })
                .thenWaitUntil(
                        () ->
                                helper.assertTrue(
                                        helper.getLevel()
                                                        .getBrightness(
                                                                net.minecraft.world.level
                                                                        .LightLayer.SKY,
                                                                OPEN_SKY.above())
                                                == 15,
                                        "Removing cover must restore sky light"))
                .thenWaitUntil(
                        () -> {
                            machine.updateSunVisibility(helper.getLevel());
                            helper.assertTrue(
                                    machine.progress() > 900, "Clear sky must resume the still");
                        })
                .thenExecute(
                        () -> {
                            before[0] = machine.outputTank().getAmountAsInt(0);
                            runPeriod(helper, machine);
                            helper.assertTrue(
                                    machine.outputTank().getAmountAsInt(0) == before[0] + 1,
                                    "Restored light distils exactly one further millibucket");
                        })
                .thenSucceed();
    }

    static void containers(GameTestHelper helper) {
        var machine = machine(helper);
        machine.inventory()
                .set(0, ItemResource.of(new ItemStack(Items.WATER_BUCKET)), 1);
        machine.serverTick(helper.getLevel());
        helper.assertTrue(
                machine.inputTank().getAmountAsInt(0) == 1000,
                "Tank " + machine.inputTank().getAmountAsInt(0) + " slot0 "
                        + machine.inventory().stack(0) + " slot1 "
                        + machine.inventory().stack(1));
        helper.assertTrue(
                machine.inventory().stack(1).is(Items.BUCKET),
                "Empties must move to the output slot, saw "
                        + machine.inventory().stack(1));
        fill(machine.outputTank(), fluid(FluidDefinition.DISTILLED_WATER), 1000);
        machine.inventory().set(2, ItemResource.of(new ItemStack(Items.BUCKET)), 1);
        machine.serverTick(helper.getLevel());
        helper.assertTrue(
                machine.outputTank().getAmountAsInt(0) == 0
                        && machine.inventory()
                                .stack(3)
                                .is(
                                        ModFluids.FAMILIES
                                                .get(FluidDefinition.DISTILLED_WATER)
                                                .bucket()
                                                .get()),
                "Distilled water fills containers from the output tank");
        try (var transaction = Transaction.openRoot()) {
            helper.assertValueEqual(
                    machine.automation(Direction.UP)
                            .insert(
                                    0,
                                    ItemResource.of(new ItemStack(Items.WATER_BUCKET)),
                                    1,
                                    transaction),
                    1,
                    "Water containers enter from above");
            helper.assertValueEqual(
                    machine.automation(Direction.DOWN)
                            .insert(
                                    2,
                                    ItemResource.of(new ItemStack(Items.BUCKET)),
                                    1,
                                    transaction),
                    1,
                    "Fill containers enter from below");
        }
        machine.inventory().set(0, ItemResource.of(new ItemStack(Items.WATER_BUCKET)), 1);
        machine.serverTick(helper.getLevel());
        helper.assertTrue(
                machine.inputTank().getAmountAsInt(0) == 2000,
                "Second bucket drains through the same native port");
        helper.succeed();
    }

    private static void runPeriod(GameTestHelper helper, SolarDistillerBlockEntity machine) {
        for (int tick = 0; tick < SolarDistillerBlockEntity.TICK_RATE; tick++)
            machine.serverTick(helper.getLevel());
    }

    private static SolarDistillerBlockEntity machine(GameTestHelper helper) {
        helper.setBlock(POSITION, ModMachines.block(MachineKind.SOLAR_DISTILLER));
        return helper.getBlockEntity(POSITION, SolarDistillerBlockEntity.class);
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

    private SolarDistillerTests() {}
}
