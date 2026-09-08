package ic2.neoforge.test;

import ic2.neoforge.fluid.FluidDefinition;
import ic2.neoforge.item.UpgradeItem;
import ic2.neoforge.machine.ElectrolyzerBlockEntity;
import ic2.neoforge.machine.EnergyStorageBlockEntity;
import ic2.neoforge.machine.MachineBlock;
import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.machine.TankBlockEntity;
import ic2.neoforge.menu.MachineMenu;
import ic2.neoforge.registration.ModFluids;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.registration.ModUpgrades;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

final class ElectrolyzerTests {
    private static final BlockPos POSITION = new BlockPos(2, 2, 2);

    static void networkAndReload(GameTestHelper helper) {
        var machine = machine(helper);
        fill(machine, FluidResource.of(Fluids.WATER), 40);
        helper.setBlock(
                POSITION.west(),
                ModMachines.block(MachineKind.CESU)
                        .defaultBlockState()
                        .setValue(MachineBlock.FACING, Direction.EAST));
        var battery = helper.getBlockEntity(POSITION.west(), EnergyStorageBlockEntity.class);
        battery.energy().insert(6400);
        helper.startSequence()
                .thenWaitUntil(
                        () ->
                                helper.assertTrue(
                                        machine.progress() >= 20,
                                        "The real MV energy network must start electrolysis"))
                .thenExecute(
                        () -> {
                            var restored = restore(helper, machine);
                            helper.assertTrue(
                                    restored.inputTank().getAmountAsInt(0) == 40
                                            && restored.progress() == machine.progress()
                                            && restored.energy().stored()
                                                    == machine.energy().stored(),
                                    "Reload must retain water, progress and paid energy");
                            helper.getLevel().setBlockEntity(restored);
                        })
                .thenWaitUntil(
                        () ->
                                helper.assertTrue(
                                        tank(helper, Direction.DOWN).tank().getAmountAsInt(0) == 26,
                                        "The complete electrolysis batch must produce hydrogen"
                                            + " below"))
                .thenExecute(
                        () -> {
                            var restored =
                                    helper.getBlockEntity(POSITION, ElectrolyzerBlockEntity.class);
                            helper.assertTrue(
                                    tank(helper, Direction.DOWN)
                                                    .tank()
                                                    .getResource(0)
                                                    .equals(fluid(FluidDefinition.HYDROGEN))
                                            && tank(helper, Direction.UP)
                                                    .tank()
                                                    .getResource(0)
                                                    .equals(fluid(FluidDefinition.OXYGEN))
                                            && tank(helper, Direction.UP).tank().getAmountAsInt(0)
                                                    == 13
                                            && restored.inputTank().getAmountAsInt(0) == 0
                                            && restored.energy().stored() == 0
                                            && battery.energy().stored() == 0,
                                    "Forty mB water and exactly 6400 EU yield 26 mB hydrogen plus"
                                        + " 13 mB oxygen across reload");
                        })
                .thenSucceed();
    }

    static void blockedOutput(GameTestHelper helper) {
        var machine = machine(helper);
        fill(machine, FluidResource.of(Fluids.WATER), 40);
        machine.energy().insert(6400);
        try (var transaction = Transaction.openRoot()) {
            tank(helper, Direction.UP)
                    .tank()
                    .insert(0, fluid(FluidDefinition.OXYGEN), 23988, transaction);
            transaction.commit();
        }
        machine.serverTick(helper.getLevel());
        helper.assertTrue(
                machine.energy().stored() == 6400
                        && machine.progress() == 0
                        && machine.inputTank().getAmountAsInt(0) == 40
                        && tank(helper, Direction.DOWN).tank().getAmountAsInt(0) == 0,
                "When the second output is one mB short, the first output and input probe must roll"
                    + " back without EU consumption");
        try (var transaction = Transaction.openRoot()) {
            tank(helper, Direction.UP)
                    .tank()
                    .extract(0, fluid(FluidDefinition.OXYGEN), 1, transaction);
            transaction.commit();
        }
        for (int tick = 0; tick < 200; tick++) machine.serverTick(helper.getLevel());
        helper.assertTrue(
                machine.energy().stored() == 0
                        && machine.inputTank().getAmountAsInt(0) == 0
                        && tank(helper, Direction.UP).tank().getAmountAsInt(0) == 24000
                        && tank(helper, Direction.DOWN).tank().getAmountAsInt(0) == 26,
                "After freeing exactly one mB, both outputs must commit together");
        helper.succeed();
    }

    static void interruptionAndRecipeIdentity(GameTestHelper helper) {
        var machine = machine(helper);
        fill(machine, FluidResource.of(Fluids.WATER), 40);
        machine.energy().insert(32);
        machine.serverTick(helper.getLevel());
        helper.assertTrue(machine.progress() == 1, "One paid tick advances once");
        machine.serverTick(helper.getLevel());
        helper.assertTrue(
                machine.progress() == 0 && machine.inputTank().getAmountAsInt(0) == 40,
                "Power loss resets progress while retaining unconsumed fluid, matching the"
                    + " recovered rule");
        machine.energy().insert(6400);
        var tag = machine.saveWithFullMetadata(helper.getLevel().registryAccess());
        tag.putString("recipe", "old-recipe-definition");
        tag.putInt("progress", 199);
        var restored =
                (ElectrolyzerBlockEntity)
                        BlockEntity.loadStatic(
                                machine.getBlockPos(),
                                machine.getBlockState(),
                                tag,
                                helper.getLevel().registryAccess());
        restored.setLevel(helper.getLevel());
        restored.serverTick(helper.getLevel());
        helper.assertTrue(
                restored.progress() == 1
                        && restored.inputTank().getAmountAsInt(0) == 40
                        && tank(helper, Direction.DOWN).tank().getAmountAsInt(0) == 0,
                "Progress from a different serialized recipe definition must not complete the"
                    + " current batch");
        helper.setBlock(POSITION.above(), Blocks.AIR);
        restored.serverTick(helper.getLevel());
        helper.assertTrue(
                restored.progress() == 0,
                "Removing a required output tank must reset the operation");
        helper.succeed();
    }

    static void dataPackAndPorts(GameTestHelper helper) {
        var machine = machine(helper);
        helper.setBlock(POSITION.east(), ModMachines.block(MachineKind.TANK));
        helper.setBlock(POSITION.west(), ModMachines.block(MachineKind.TANK));
        fill(machine, fluid(FluidDefinition.DISTILLED_WATER), 2);
        machine.energy().insert(28);
        for (int tick = 0; tick < 4; tick++) machine.serverTick(helper.getLevel());
        helper.assertTrue(
                tank(helper, Direction.EAST).tank().getAmountAsInt(0) == 3
                        && tank(helper, Direction.WEST).tank().getAmountAsInt(0) == 1
                        && machine.inputTank().getAmountAsInt(0) == 0
                        && machine.energy().stored() == 0,
                "Custom datapacks define distinct output directions, amounts, processing duration"
                    + " and power");
        try (var transaction = Transaction.openRoot()) {
            var port = machine.fluidAutomation(Direction.NORTH);
            helper.assertTrue(
                    port.insert(0, FluidResource.of(Fluids.LAVA), 1, transaction) == 0,
                    "Electrolyzers reject input without a recipe");
            machine.inputTank().insert(0, FluidResource.of(Fluids.WATER), 40, transaction);
            helper.assertTrue(
                    port.extract(0, FluidResource.of(Fluids.WATER), 40, transaction) == 0,
                    "The input tank cannot be drained by external automation");
        }
        var menu =
                new MachineMenu(
                        1, helper.makeMockPlayer(GameType.SURVIVAL).getInventory(), machine);
        helper.assertTrue(
                menu.getSlot(1)
                                .mayPlace(
                                        ModUpgrades.ALL
                                                .get(UpgradeItem.Kind.FLUID_PULLING)
                                                .get()
                                                .getDefaultInstance())
                        && !menu.getSlot(1)
                                .mayPlace(
                                        ModUpgrades.ALL
                                                .get(UpgradeItem.Kind.FLUID_EJECTOR)
                                                .get()
                                                .getDefaultInstance()),
                "Electrolyzers accept fluid pulling upgrades only");
        helper.succeed();
    }

    private static ElectrolyzerBlockEntity machine(GameTestHelper helper) {
        helper.setBlock(POSITION, ModMachines.block(MachineKind.ELECTROLYZER));
        helper.setBlock(POSITION.above(), ModMachines.block(MachineKind.TANK));
        helper.setBlock(POSITION.below(), ModMachines.block(MachineKind.TANK));
        return helper.getBlockEntity(POSITION, ElectrolyzerBlockEntity.class);
    }

    private static TankBlockEntity tank(GameTestHelper helper, Direction direction) {
        return helper.getBlockEntity(POSITION.relative(direction), TankBlockEntity.class);
    }

    private static FluidResource fluid(FluidDefinition definition) {
        return FluidResource.of(ModFluids.FAMILIES.get(definition).source().get());
    }

    private static void fill(ElectrolyzerBlockEntity machine, FluidResource resource, int amount) {
        try (var transaction = Transaction.openRoot()) {
            machine.inputTank().insert(0, resource, amount, transaction);
            transaction.commit();
        }
    }

    private static ElectrolyzerBlockEntity restore(
            GameTestHelper helper, ElectrolyzerBlockEntity machine) {
        var restored =
                (ElectrolyzerBlockEntity)
                        BlockEntity.loadStatic(
                                machine.getBlockPos(),
                                machine.getBlockState(),
                                machine.saveWithFullMetadata(helper.getLevel().registryAccess()),
                                helper.getLevel().registryAccess());
        restored.setLevel(helper.getLevel());
        return restored;
    }

    private ElectrolyzerTests() {}
}
