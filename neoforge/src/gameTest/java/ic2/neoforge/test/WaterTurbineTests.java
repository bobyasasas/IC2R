package ic2.neoforge.test;

import ic2.core.machine.RotorMaterial;
import ic2.core.machine.RotorOperation;
import ic2.neoforge.machine.EnergyStorageBlockEntity;
import ic2.neoforge.machine.MachineBlock;
import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.machine.WaterTurbineBlockEntity;
import ic2.neoforge.menu.MachineMenu;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.registration.ModRotors;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.commands.FillBiomeCommand;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

final class WaterTurbineTests {
    private static final BlockPos POSITION = new BlockPos(27, 10, 27);

    static void operationAndWear(GameTestHelper helper) {
        var turbine = machine(helper, Biomes.RIVER);
        turbine.inventory().set(0, ItemResource.EMPTY, 0);
        try (var transaction = Transaction.openRoot()) {
            helper.assertTrue(
                    turbine.inventory().insert(0, ItemResource.of(Items.DIAMOND), 1, transaction)
                                    == 0
                            && turbine.inventory()
                                            .insert(
                                                    0,
                                                    ItemResource.of(
                                                            ModRotors.ROTORS
                                                                    .get(RotorMaterial.WOODEN)
                                                                    .get()),
                                                    1,
                                                    transaction)
                                    == 0,
                    "Water rotor slots must reject ordinary items and wooden rotors");
            helper.assertTrue(
                    turbine.inventory()
                                    .insert(
                                            0,
                                            ItemResource.of(
                                                    ModRotors.ROTORS.get(RotorMaterial.IRON).get()),
                                            2,
                                            transaction)
                            == 1,
                    "Native rotor slots accept exactly one compatible rotor");
            transaction.commit();
        }
        var menu =
                new MachineMenu(
                        1, helper.makeMockPlayer(GameType.SURVIVAL).getInventory(), turbine);
        helper.assertTrue(
                !menu.getSlot(0)
                                .mayPlace(
                                        ModRotors.ROTORS
                                                .get(RotorMaterial.WOODEN)
                                                .get()
                                                .getDefaultInstance())
                        && menu.getSlot(0)
                                .mayPlace(
                                        ModRotors.ROTORS
                                                .get(RotorMaterial.IRON)
                                                .get()
                                                .getDefaultInstance()),
                "The visible rotor slot must apply the same water compatibility rule as"
                    + " automation");
        turbine.serverTick(helper.getLevel());
        int rate = turbine.menuValue(0), damage = turbine.inventory().stack(0).getDamageValue();
        helper.assertTrue(
                rate > 0
                        && rate <= 100
                        && turbine.rotorDiameter() == 5
                        && turbine.rotorDegreesPerTick() < 0,
                "A river uses the smaller rotor and north-facing reverse animation");
        helper.assertTrue(
                turbine.output(Direction.NORTH).available() == 0, "Water KU exits the back face");
        try (var transaction = Transaction.openRoot()) {
            helper.assertTrue(
                    turbine.output(Direction.SOUTH).extract(rate, transaction) == rate,
                    "Water KU supports native transactions");
        }
        helper.assertTrue(
                turbine.output(Direction.SOUTH).available() == rate,
                "Aborted output restores this tick's budget");
        try (var transaction = Transaction.openRoot()) {
            turbine.output(Direction.SOUTH).extract(rate, transaction);
            transaction.commit();
        }
        var restored =
                (WaterTurbineBlockEntity)
                        BlockEntity.loadStatic(
                                turbine.getBlockPos(),
                                turbine.getBlockState(),
                                turbine.saveWithFullMetadata(helper.getLevel().registryAccess()),
                                helper.getLevel().registryAccess());
        restored.setLevel(helper.getLevel());
        restored.serverTick(helper.getLevel());
        helper.assertTrue(
                restored.menuValue(0) == rate
                        && restored.output(Direction.SOUTH).available() == 0
                        && restored.inventory().stack(0).getDamageValue() == damage,
                "Reload must neither reroll river turbulence nor duplicate output or wear");
        helper.runAtTickTime(
                25,
                () -> {
                    helper.assertTrue(
                            turbine.inventory().stack(0).getDamageValue() > damage,
                            "A river rotor wears every 20 ticks");
                    helper.succeed();
                });
    }

    static void obstructions(GameTestHelper helper) {
        var turbine = machine(helper, Biomes.RIVER);
        helper.setBlock(POSITION.north(), Blocks.STONE);
        helper.startSequence()
                .thenWaitUntil(
                        () ->
                                helper.assertTrue(
                                        turbine.menuValue(1)
                                                == RotorOperation.Status.NO_SPACE.ordinal(),
                                        "A dry or solid rotor plane must stop the turbine"))
                .thenExecute(() -> helper.setBlock(POSITION.north(), Blocks.WATER))
                .thenWaitUntil(
                        () ->
                                helper.assertTrue(
                                        turbine.menuValue(0) > 0,
                                        "Restoring water must resume output"))
                .thenExecute(
                        () ->
                                helper.setBlock(
                                        POSITION.north(10),
                                        ModMachines.block(MachineKind.WATER_KINETIC_GENERATOR)))
                .thenWaitUntil(
                        () ->
                                helper.assertTrue(
                                        turbine.menuValue(1)
                                                        == RotorOperation.Status.INTERFERENCE
                                                                .ordinal()
                                                && turbine.menuValue(0) == 0,
                                        "Water turbines sharing a channel must interfere"))
                .thenExecute(
                        () -> {
                            helper.setBlock(POSITION.north(10), Blocks.WATER);
                            biome(helper, Biomes.PLAINS);
                        })
                .thenWaitUntil(
                        () ->
                                helper.assertTrue(
                                        turbine.menuValue(1)
                                                        == RotorOperation.Status.INVALID_BIOME
                                                                .ordinal()
                                                && turbine.menuValue(0) == 0,
                                        "Changing to a non-water biome must stop output without"
                                            + " requiring reload"))
                .thenSucceed();
    }

    static void tidesAndDeepOcean(GameTestHelper helper) {
        var turbine = machine(helper, Biomes.OCEAN);
        turbine.serverTick(helper.getLevel());
        int ocean = turbine.menuValue(0);
        helper.assertTrue(
                ocean > 0 && turbine.rotorDiameter() == 7, "Ocean rotors use their full diameter");
        biome(helper, Biomes.DEEP_OCEAN);
        helper.startSequence()
                .thenWaitUntil(
                        () ->
                                helper.assertTrue(
                                        turbine.menuValue(0) > ocean * 1.2
                                                && turbine.rotorDiameter() == 7,
                                        "Deep ocean must reach its distinct higher-output branch"
                                            + " with full diameter"))
                .thenExecute(() -> helper.setTime(9000))
                .thenWaitUntil(
                        () ->
                                helper.assertTrue(
                                        turbine.rotorDegreesPerTick() > 0
                                                && turbine.menuValue(0) > 0,
                                        "Ebb tide reverses the rotor while continuing to supply"
                                            + " positive KU"))
                .thenSucceed();
    }

    static void networkSupply(GameTestHelper helper) {
        machine(helper, Biomes.RIVER);
        helper.setBlock(
                POSITION.south(),
                ModMachines.block(MachineKind.KINETIC_GENERATOR)
                        .defaultBlockState()
                        .setValue(MachineBlock.FACING, Direction.NORTH));
        helper.setBlock(
                POSITION.south(2),
                ModMachines.block(MachineKind.MFE)
                        .defaultBlockState()
                        .setValue(MachineBlock.FACING, Direction.SOUTH));
        var battery = helper.getBlockEntity(POSITION.south(2), EnergyStorageBlockEntity.class);
        helper.startSequence()
                .thenWaitUntil(
                        () ->
                                helper.assertTrue(
                                        battery.energy().stored() >= 256,
                                        "Water KU must drive the real kinetic generator and"
                                            + " electrical network"))
                .thenSucceed();
    }

    private static WaterTurbineBlockEntity machine(
            GameTestHelper helper, ResourceKey<Biome> biome) {
        biome(helper, biome);
        helper.setBlock(
                POSITION,
                ModMachines.block(MachineKind.WATER_KINETIC_GENERATOR)
                        .defaultBlockState()
                        .setValue(MachineBlock.FACING, Direction.NORTH));
        var turbine = helper.getBlockEntity(POSITION, WaterTurbineBlockEntity.class);
        turbine.inventory()
                .set(0, ItemResource.of(ModRotors.ROTORS.get(RotorMaterial.IRON).get()), 1);
        return turbine;
    }

    private static void biome(GameTestHelper helper, ResourceKey<Biome> biome) {
        var result =
                FillBiomeCommand.fill(
                        helper.getLevel(),
                        helper.absolutePos(new BlockPos(22, 6, 3)),
                        helper.absolutePos(new BlockPos(32, 14, 51)),
                        helper.getLevel()
                                .registryAccess()
                                .lookupOrThrow(Registries.BIOME)
                                .getOrThrow(biome));
        helper.assertTrue(
                result.left().isPresent(),
                "The controlled biome channel must fit native fill limits");
    }

    private WaterTurbineTests() {}
}
