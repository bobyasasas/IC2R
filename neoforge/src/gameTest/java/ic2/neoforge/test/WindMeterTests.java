package ic2.neoforge.test;

import ic2.core.machine.RotorOperation;
import ic2.neoforge.item.ElectricItemEnergy;
import ic2.neoforge.item.WindMeterItem;
import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.machine.WindGeneratorBlockEntity;
import ic2.neoforge.machine.WindTurbineBlockEntity;
import ic2.neoforge.registration.ModMaterialBlocks;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.registration.ModTools;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Legacy ItemWindMeter and the two plain blocks shipped alongside it this slice. */
final class WindMeterTests {
    private static ItemStack chargedMeter() {
        var stack = new ItemStack(ModTools.WIND_METER.get());
        ElectricItemEnergy.charge(stack, 10000.0, 1, true, false);
        return stack;
    }

    private static InteractionResult useOn(
            GameTestHelper helper, Player player, BlockPos pos) {
        var hit = new BlockHitResult(
                Vec3.atCenterOf(helper.absolutePos(pos)), Direction.UP, helper.absolutePos(pos),
                false);
        var context = new net.minecraft.world.item.context.UseOnContext(
                player, InteractionHand.MAIN_HAND, hit);
        return ModTools.WIND_METER
                .get()
                .onItemUseFirst(player.getItemInHand(InteractionHand.MAIN_HAND), context);
    }

    static void bareUseReadsWindAndPays(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack meter = chargedMeter();
        player.setItemInHand(InteractionHand.MAIN_HAND, meter);

        var result = ModTools.WIND_METER
                .get()
                .use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(result == InteractionResult.SUCCESS, "A charged meter must read");
        helper.assertTrue(
                ElectricItemEnergy.charge(meter) == 10000.0 - WindMeterItem.OPERATION_ENERGY_COST,
                "One reading must cost exactly 50 EU");
        helper.succeed();
    }

    static void lowChargePasses(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack meter = chargedMeter();
        ElectricItemEnergy.discharge(meter, 10000.0 - 20.0, Integer.MAX_VALUE, true, false, false);
        player.setItemInHand(InteractionHand.MAIN_HAND, meter);

        var result = ModTools.WIND_METER
                .get()
                .use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(
                result == InteractionResult.PASS && ElectricItemEnergy.charge(meter) == 20.0,
                "Below 50 EU the meter must pass without draining");
        helper.succeed();
    }

    static void readsWindGeneratorEffectiveWind(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack meter = chargedMeter();
        player.setItemInHand(InteractionHand.MAIN_HAND, meter);
        var pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModMachines.block(MachineKind.WIND_GENERATOR).defaultBlockState());
        var generator =
                (WindGeneratorBlockEntity)
                        helper.getBlockEntity(pos, WindGeneratorBlockEntity.class);
        generator.sampleObstructions(helper.getLevel());
        int obstructions = generator.obstructions();

        var result = useOn(helper, player, pos);
        helper.assertTrue(
                result == InteractionResult.SUCCESS,
                "Using the meter on a wind generator must report its effective wind");
        helper.assertTrue(
                ElectricItemEnergy.charge(meter) == 10000.0 - WindMeterItem.OPERATION_ENERGY_COST,
                "A generator reading costs 50 EU");
        helper.assertTrue(
                obstructions >= 0,
                "A fresh generator in an open test cell must report a non-negative obstruction"
                        + " count");
        helper.succeed();
    }

    static void readsStoppedTurbineWithoutDraining(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack meter = chargedMeter();
        player.setItemInHand(InteractionHand.MAIN_HAND, meter);
        var pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModMachines.block(MachineKind.WIND_KINETIC_GENERATOR)
                .defaultBlockState());
        // No rotor installed and never ticked: inactive with NO_ROTOR. Legacy returns FAIL and
        // spends no energy on that path.
        var result = useOn(helper, player, pos);
        var operation =
                ((WindTurbineBlockEntity) helper.getBlockEntity(pos, WindTurbineBlockEntity.class))
                        .operation();
        helper.assertTrue(
                result == InteractionResult.FAIL,
                "An inactive turbine must reject the meter reading");
        helper.assertTrue(
                operation.status() == RotorOperation.Status.NO_ROTOR,
                "A turbine without a rotor must report NO_ROTOR");
        helper.assertTrue(
                ElectricItemEnergy.charge(meter) == 10000.0,
                "The rejection path must not consume energy");
        helper.succeed();
    }

    static void refractoryBricksDropThemselves(GameTestHelper helper) {
        var pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModMaterialBlocks.REFRACTORY_BRICKS.get().defaultBlockState());
        helper.assertTrue(
                helper.getBlockState(pos)
                        .getBlock()
                        == ModMaterialBlocks.REFRACTORY_BRICKS.get(),
                "The block must register under its legacy id");
        helper.getLevel().destroyBlock(helper.absolutePos(pos), true);
        boolean dropped = helper.getLevel()
                .getEntitiesOfClass(
                        net.minecraft.world.entity.item.ItemEntity.class,
                        new net.minecraft.world.phys.AABB(helper.absolutePos(pos)).inflate(2))
                .stream()
                .anyMatch(entity -> entity.getItem().is(
                        ModMaterialBlocks.REFRACTORY_BRICKS.get().asItem()));
        helper.assertTrue(dropped, "Breaking refractory bricks must drop the block item");
        helper.succeed();
    }

    static void reinforcedDoorHalfSemantics(GameTestHelper helper) {
        var lower = new BlockPos(1, 1, 1);
        var upper = lower.above();
        var state = ModMaterialBlocks.REINFORCED_DOOR
                .get()
                .defaultBlockState()
                .setValue(DoorBlock.FACING, Direction.NORTH);
        helper.setBlock(lower, state.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        helper.setBlock(upper, state.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));

        // Vanilla iron doors only open by redstone; the half-anchoring still applies.
        helper.getLevel().destroyBlock(helper.absolutePos(lower), true);
        helper.assertTrue(
                helper.getBlockState(upper).isAir(),
                "Breaking the lower half must take the upper half with it");
        boolean dropped = helper.getLevel()
                .getEntitiesOfClass(
                        net.minecraft.world.entity.item.ItemEntity.class,
                        new net.minecraft.world.phys.AABB(helper.absolutePos(lower)).inflate(2))
                .stream()
                .anyMatch(entity -> entity.getItem().is(
                        ModMaterialBlocks.REINFORCED_DOOR.get().asItem()));
        helper.assertTrue(
                dropped,
                "Only the lower half must drop the door item (legacy block_state_property loot)");
        helper.succeed();
    }

    private WindMeterTests() {}
}
