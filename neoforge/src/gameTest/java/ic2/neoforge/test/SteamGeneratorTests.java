package ic2.neoforge.test;

import ic2.core.machine.SteamBoiler;
import ic2.neoforge.explosion.HeatExplosion;
import ic2.neoforge.fluid.FluidDefinition;
import ic2.neoforge.machine.*;
import ic2.neoforge.menu.MachineMenu;
import ic2.neoforge.registration.MaterialDefinition;
import ic2.neoforge.registration.ModFluids;
import ic2.neoforge.registration.ModItems;
import ic2.neoforge.registration.ModMachines;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

final class SteamGeneratorTests {
    private static final BlockPos POSITION = new BlockPos(2, 1, 2);

    static void warmupAndPorts(GameTestHelper helper) {
        var machine = machine(helper, 25, 10, 0, 0);
        heater(helper, POSITION.west(), Direction.EAST);
        helper.setBlock(POSITION.east(), ModMachines.block(MachineKind.TANK));
        var target = helper.getBlockEntity(POSITION.east(), TankBlockEntity.class);
        fill(target.tank(), fluid(FluidDefinition.DISTILLED_WATER), 23999);
        fill(machine.waterTank(), fluid(FluidDefinition.DISTILLED_WATER), 100);
        machine.serverTick(helper.getLevel());
        helper.assertTrue(
                target.tank().getAmountAsInt(0) == 24000
                        && machine.waterTank().getAmountAsInt(0) == 99,
                "Cold zero-pressure boiler transfers only accepted water, without losing the"
                    + " rejected remainder");
        helper.assertTrue(
                Math.abs(machine.boilerState().temperature() - 25.05) < 1e-8
                        && machine.progress() == 0,
                "Warmup consumes heat but does not calcify passed-through water");
        for (var side : Direction.values()) {
            var port =
                    helper.getLevel()
                            .getCapability(Capabilities.Fluid.BLOCK, machine.getBlockPos(), side);
            try (var tx = Transaction.openRoot()) {
                helper.assertValueEqual(
                        port.extract(0, fluid(FluidDefinition.DISTILLED_WATER), 1, tx),
                        0,
                        "Water cannot be externally extracted");
                helper.assertValueEqual(
                        port.insert(0, fluid(FluidDefinition.STEAM), 1, tx),
                        0,
                        "Input rejects steam");
            }
        }
        helper.succeed();
    }

    static void condenserChain(GameTestHelper helper) {
        var machine = machine(helper, 100, 1, 0, 0);
        heater(helper, POSITION.west(), Direction.EAST);
        // Ten thousand HU covers boiling alone; native tick order also requires warmup heat.
        helper.setBlock(
                POSITION.west(2),
                ModMachines.block(MachineKind.MFE)
                        .defaultBlockState()
                        .setValue(MachineBlock.FACING, Direction.EAST));
        helper.getBlockEntity(POSITION.west(2), EnergyStorageBlockEntity.class)
                .energy()
                .insert(1024);
        helper.setBlock(POSITION.east(), ModMachines.block(MachineKind.CONDENSER));
        fill(machine.waterTank(), fluid(FluidDefinition.DISTILLED_WATER), 100);
        machine.serverTick(helper.getLevel());
        helper.runAtTickTime(
                20,
                () ->
                        replace(
                                helper,
                                helper.getBlockEntity(POSITION, SteamGeneratorBlockEntity.class)));
        helper.runAfterDelay(
                115,
                () -> {
                    var restored = helper.getBlockEntity(POSITION, SteamGeneratorBlockEntity.class);
                    var condenser =
                            helper.getBlockEntity(POSITION.east(), CondenserBlockEntity.class);
                    helper.assertTrue(
                            restored.waterTank().getAmountAsInt(0) == 0
                                    && restored.progress() == 0
                                    && condenser.inputTank().getAmountAsInt(0) == 0
                                    && condenser.progress() == 0
                                    && condenser.outputTank().getAmountAsInt(0) == 100,
                            "Electric heat -> boiler -> passive condenser returns all 100"
                                + " distilled-water mB after mid-run reload: water="
                                    + restored.waterTank().getAmountAsInt(0)
                                    + ", temperature="
                                    + restored.boilerState().temperature()
                                    + ", steam="
                                    + condenser.inputTank().getAmountAsInt(0)
                                    + ", credit="
                                    + condenser.progress()
                                    + ", output="
                                    + condenser.outputTank().getAmountAsInt(0));
                    helper.succeed();
                });
    }

    static void superheatedSteam(GameTestHelper helper) {
        var machine = machine(helper, 374, 1, 220, 0);
        heater(helper, POSITION.west(), Direction.EAST);
        heater(helper, POSITION.above(), Direction.DOWN);
        helper.setBlock(POSITION.east(), ModMachines.block(MachineKind.TANK));
        fill(machine.waterTank(), fluid(FluidDefinition.DISTILLED_WATER), 10);
        machine.serverTick(helper.getLevel());
        var target = helper.getBlockEntity(POSITION.east(), TankBlockEntity.class);
        helper.assertTrue(
                target.tank().getResource(0).equals(fluid(FluidDefinition.SUPERHEATED_STEAM))
                        && target.tank().getAmountAsInt(0) == 100
                        && machine.waterTank().getAmountAsInt(0) == 9
                        && machine.fuelRemaining() == 200
                        && Math.abs(machine.boilerState().temperature() - 374) < 1e-8,
                "Two native heat sources deliver 200 HU for one water mB at 220 bar and 374"
                    + " degrees");
        helper.succeed();
    }

    static void scaleStopsHeat(GameTestHelper helper) {
        var machine = machine(helper, 100, 1, 0, 99999);
        var source = heater(helper, POSITION.west(), Direction.EAST);
        helper.setBlock(POSITION.east(), ModMachines.block(MachineKind.TANK));
        fill(machine.waterTank(), FluidResource.of(Fluids.WATER), 100);
        machine.serverTick(helper.getLevel());
        helper.assertValueEqual(
                machine.progress(),
                100000,
                "Ordinary water finishes the last unit of calcification");
        helper.runAfterDelay(
                2,
                () -> {
                    helper.assertTrue(
                            machine.waterTank().getAmountAsInt(0) == 99
                                    && machine.fuelRemaining() == 0
                                    && source.output(Direction.EAST).available() == 100,
                            "Calcified boiler leaves prepaid heat in its source and stops consuming"
                                + " water");
                    helper.succeed();
                });
    }

    static void overheatingAndReload(GameTestHelper helper) {
        var machine = machine(helper, 499.99, 0, 0, 0);
        heater(helper, POSITION.west(), Direction.EAST);
        var attempts = new AtomicInteger();
        Consumer<ExplosionEvent.Start> guard =
                event -> {
                    if (event.getExplosion() instanceof HeatExplosion
                            && event.getExplosion()
                                    .center()
                                    .equals(machine.getBlockPos().getCenter())) {
                        attempts.incrementAndGet();
                        event.setCanceled(true);
                    }
                };
        NeoForge.EVENT_BUS.addListener(guard);
        try {
            machine.serverTick(helper.getLevel());
            var restored = replace(helper, machine);
            restored.serverTick(helper.getLevel());
            helper.assertTrue(
                    attempts.get() == 1
                            && helper.getBlockState(POSITION)
                                    .is(ModMachines.block(MachineKind.STEAM_GENERATOR))
                            && restored.boilerState().temperature() == 500,
                    "Overheat protection preserves the boiler and a same-tick reload cannot repeat"
                        + " the heat draw or burst");
        } finally {
            helper.setBlock(POSITION, Blocks.AIR);
            helper.setBlock(POSITION.west(), Blocks.AIR);
            NeoForge.EVENT_BUS.unregister(guard);
        }
        helper.succeed();
    }

    static void partialSteamAndMenu(GameTestHelper helper) {
        var machine = machine(helper, 100, 1, 0, 0);
        heater(helper, POSITION.west(), Direction.EAST);
        helper.setBlock(POSITION.east(), ModMachines.block(MachineKind.TANK));
        var target = helper.getBlockEntity(POSITION.east(), TankBlockEntity.class);
        fill(target.tank(), fluid(FluidDefinition.STEAM), 23999);
        fill(machine.waterTank(), fluid(FluidDefinition.DISTILLED_WATER), 10);
        machine.serverTick(helper.getLevel());
        helper.assertTrue(
                target.tank().getAmountAsInt(0) == 24000
                        && machine.waterTank().getAmountAsInt(0) == 9
                        && machine.menuValue(5) == 1,
                "Partial steam delivery reports the actual one mB; sub-water-unit steam is vented");
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(machine.getBlockPos().getCenter());
        var server = new MachineMenu(1, player.getInventory(), machine);
        var client =
                new MachineMenu(1, player.getInventory(), machine.getBlockPos(), machine.kind());
        MenuTestLink.connect(server, client);
        helper.assertTrue(
                client.familyValue(5) == 1
                        && client.familyValue(6) == SteamBoiler.Output.STEAM.ordinal()
                        && client.familyFloat(4) == 100
                        && client.capacity() == 0,
                "Extended menu fields survive native signed-short packets");
        player.containerMenu = server;
        helper.assertTrue(
                server.clickMenuButton(player, 3)
                        && server.clickMenuButton(player, 10)
                        && machine.settings().waterPerTick() == 1000
                        && machine.settings().pressure() == 100,
                "Validated flow and pressure controls configure independent bounded settings");
        helper.assertTrue(
                !server.clickMenuButton(player, Integer.MAX_VALUE),
                "Unknown boiler requests are rejected");
        player.containerMenu = player.inventoryMenu;
        helper.assertTrue(!server.clickMenuButton(player, 7), "Closed menus cannot change flow");
        helper.succeed();
    }

    private static SteamGeneratorBlockEntity machine(
            GameTestHelper helper, double temperature, int flow, int pressure, int scale) {
        helper.setBlock(POSITION, ModMachines.block(MachineKind.STEAM_GENERATOR));
        var machine = helper.getBlockEntity(POSITION, SteamGeneratorBlockEntity.class);
        var data = machine.saveWithFullMetadata(helper.getLevel().registryAccess());
        data.putDouble("systemheat", temperature);
        data.putInt("inputmb", flow);
        data.putInt("pressurevalve", pressure);
        data.putInt("calcification", scale);
        var restored =
                (SteamGeneratorBlockEntity)
                        BlockEntity.loadStatic(
                                machine.getBlockPos(),
                                machine.getBlockState(),
                                data,
                                helper.getLevel().registryAccess());
        helper.getLevel().removeBlockEntity(machine.getBlockPos());
        helper.getLevel().setBlockEntity(restored);
        return restored;
    }

    private static SteamGeneratorBlockEntity replace(
            GameTestHelper helper, SteamGeneratorBlockEntity machine) {
        var restored =
                (SteamGeneratorBlockEntity)
                        BlockEntity.loadStatic(
                                machine.getBlockPos(),
                                machine.getBlockState(),
                                machine.saveWithFullMetadata(helper.getLevel().registryAccess()),
                                helper.getLevel().registryAccess());
        helper.getLevel().removeBlockEntity(machine.getBlockPos());
        helper.getLevel().setBlockEntity(restored);
        return restored;
    }

    private static ElectricWorkBlockEntity heater(
            GameTestHelper helper, BlockPos pos, Direction facing) {
        helper.setBlock(
                pos,
                ModMachines.block(MachineKind.ELECTRIC_HEAT_GENERATOR)
                        .defaultBlockState()
                        .setValue(MachineBlock.FACING, facing));
        var source = helper.getBlockEntity(pos, ElectricWorkBlockEntity.class);
        for (int slot = 0; slot < 10; slot++)
            source.inventory()
                    .set(
                            slot,
                            ItemResource.of(ModItems.MATERIALS.get(MaterialDefinition.COIL).get()),
                            1);
        source.energy().insert(10000);
        source.serverTick(helper.getLevel());
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

    private SteamGeneratorTests() {}
}
