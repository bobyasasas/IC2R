package ic2.neoforge.test;

import ic2.neoforge.fluid.FluidDefinition;
import ic2.neoforge.machine.EnergyStorageBlockEntity;
import ic2.neoforge.machine.FuelHeatBlockEntity;
import ic2.neoforge.machine.MachineBlock;
import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.machine.WorkConversionBlockEntity;
import ic2.neoforge.registration.MaterialDefinition;
import ic2.neoforge.registration.ModFluids;
import ic2.neoforge.registration.ModItems;
import ic2.neoforge.registration.ModMachines;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

final class FuelHeatTests {
    static void solidReserve(GameTestHelper helper) {
        var heater = machine(helper, MachineKind.SOLID_HEAT_GENERATOR);
        heater.inventory().set(0, ItemResource.of(Items.COAL), 1);
        for (int tick = 0; tick < 400; tick++) heater.serverTick(helper.getLevel());
        helper.assertTrue(
                heater.storedHeat() == 8000
                        && heater.progress() == 0
                        && heater.inventory().stack(0).isEmpty(),
                "One coal burns for 400 ticks into exactly 8000 HU even without a consumer");
        var restored = restore(helper, heater);
        helper.assertTrue(
                restored.storedHeat() == 8000 && restored.progressMaximum() == 400,
                "Reload must retain the full hidden reserve and original fuel duration");
        restored.inventory().set(0, ItemResource.of(Items.COAL), 1);
        for (int tick = 0; tick < 100; tick++) restored.serverTick(helper.getLevel());
        helper.assertTrue(
                restored.storedHeat() == 8000 && restored.inventory().stack(0).is(Items.COAL),
                "A full paid reserve prevents consuming another fuel batch");
        helper.succeed();
    }

    static void solidOutputAndFuel(GameTestHelper helper) {
        var heater = machine(helper, MachineKind.SOLID_HEAT_GENERATOR);
        heater.inventory().set(0, ItemResource.of(Items.COAL), 1);
        heater.inventory()
                .set(
                        1,
                        ItemResource.of(ModItems.MATERIALS.get(MaterialDefinition.ASHES).get()),
                        64);
        heater.serverTick(helper.getLevel());
        helper.assertTrue(
                heater.inventory().stack(0).is(Items.COAL) && heater.storedHeat() == 0,
                "A blocked ash output must prevent starting a new fuel batch");
        heater.inventory().set(1, ItemResource.EMPTY, 0);
        heater.inventory().set(0, ItemResource.of(Items.LAVA_BUCKET), 1);
        heater.serverTick(helper.getLevel());
        helper.assertTrue(
                heater.inventory().stack(0).is(Items.LAVA_BUCKET) && heater.storedHeat() == 0,
                "Solid heaters preserve the legacy lava exclusion");
        helper.succeed();
    }

    static void fluidPrepayment(GameTestHelper helper) {
        var heater = machine(helper, MachineKind.FLUID_HEAT_GENERATOR);
        var biogas =
                FluidResource.of(ModFluids.FAMILIES.get(FluidDefinition.BIOGAS).source().get());
        try (var transaction = Transaction.openRoot()) {
            heater.tank().insert(0, biogas, 9, transaction);
            transaction.commit();
        }
        for (int tick = 0; tick < 40; tick++) heater.serverTick(helper.getLevel());
        helper.assertTrue(
                heater.storedHeat() == 0 && heater.tank().getAmountAsInt(0) == 9,
                "An incomplete ten-mB batch cannot produce unpaid heat");
        try (var transaction = Transaction.openRoot()) {
            heater.tank().insert(0, biogas, 1, transaction);
            transaction.commit();
        }
        heater.serverTick(helper.getLevel());
        helper.assertTrue(
                heater.storedHeat() == 640 && heater.tank().getAmountAsInt(0) == 0,
                "Ten mB is charged before its 640-HU batch begins");
        var restored = restore(helper, heater);
        for (int tick = 0; tick < 40; tick++) restored.serverTick(helper.getLevel());
        helper.assertTrue(
                restored.storedHeat() == 640 && restored.menuValue(1) == 32,
                "Paid heat survives reload and a full output buffer even when the tank is empty");
        helper.succeed();
    }

    static void fluidChain(GameTestHelper helper) {
        var heater = machine(helper, MachineKind.FLUID_HEAT_GENERATOR);
        var pos = new BlockPos(2, 1, 2);
        var biogas =
                FluidResource.of(ModFluids.FAMILIES.get(FluidDefinition.BIOGAS).source().get());
        try (var transaction = Transaction.openRoot()) {
            heater.tank().insert(0, biogas, 10, transaction);
            transaction.commit();
        }
        helper.setBlock(
                pos.east(),
                ModMachines.block(MachineKind.STIRLING_GENERATOR)
                        .defaultBlockState()
                        .setValue(MachineBlock.FACING, Direction.WEST));
        helper.setBlock(pos.east(2), ModMachines.block(MachineKind.BATBOX));
        var battery = helper.getBlockEntity(pos.east(2), EnergyStorageBlockEntity.class);
        var converter = helper.getBlockEntity(pos.east(), WorkConversionBlockEntity.class);
        helper.startSequence()
                .thenWaitUntil(
                        () ->
                                helper.assertTrue(
                                        battery.energy().stored() == 320,
                                        "A ten-mB heat batch must deliver exactly 320 EU through"
                                                + " Stirling and either network mode"))
                .thenExecute(
                        () ->
                                helper.assertTrue(
                                        heater.storedHeat() == 0
                                                && converter.energy().stored() == 0
                                                && heater.tank().getAmountAsInt(0) == 0,
                                        "No unpaid or stranded heat remains after full conversion"))
                .thenSucceed();
    }

    static void menuTransport(GameTestHelper helper) {
        var heater = machine(helper, MachineKind.SOLID_HEAT_GENERATOR);
        var tag = heater.saveWithFullMetadata(helper.getLevel().registryAccess());
        tag.putInt("heatEmission", 19980000);
        tag.putInt("fuelTotal", 4000);
        tag.putLong("heatReserve", 79920000000L);
        var restored =
                (FuelHeatBlockEntity)
                        BlockEntity.loadStatic(
                                heater.getBlockPos(),
                                heater.getBlockState(),
                                tag,
                                helper.getLevel().registryAccess());
        restored.setLevel(helper.getLevel());
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var server = new ic2.neoforge.menu.MachineMenu(1, player.getInventory(), restored);
        var client =
                new ic2.neoforge.menu.MachineMenu(
                        1, player.getInventory(), restored.getBlockPos(), restored.kind());
        MenuTestLink.connect(server, client);
        helper.assertTrue(
                client.familyValue(1) == 19980000
                        && client.familyFloat(0) == (float) 79920000000L
                        && client.fuelMaximum() == 4000,
                "Real signed-short menu packets must preserve large family values and compact heat"
                        + " quantities");
        helper.succeed();
    }

    private static FuelHeatBlockEntity machine(GameTestHelper helper, MachineKind kind) {
        var pos = new BlockPos(2, 1, 2);
        helper.setBlock(
                pos,
                ModMachines.block(kind)
                        .defaultBlockState()
                        .setValue(MachineBlock.FACING, Direction.EAST));
        return helper.getBlockEntity(pos, FuelHeatBlockEntity.class);
    }

    private static FuelHeatBlockEntity restore(GameTestHelper helper, FuelHeatBlockEntity heater) {
        var restored =
                (FuelHeatBlockEntity)
                        BlockEntity.loadStatic(
                                heater.getBlockPos(),
                                heater.getBlockState(),
                                heater.saveWithFullMetadata(helper.getLevel().registryAccess()),
                                helper.getLevel().registryAccess());
        restored.setLevel(helper.getLevel());
        return restored;
    }

    private FuelHeatTests() {}
}
