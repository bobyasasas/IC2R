package ic2.neoforge.test;

import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.machine.MagnetizerBlockEntity;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.registration.ModMaterialBlocks;
import ic2.neoforge.world.IronFenceBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.GameType;

final class MagnetizerTests {
    private static final BlockPos POSITION = new BlockPos(2, 2, 2);
    private static final BlockPos FENCE = new BlockPos(2, 2, 2);

    static void poweredLift(GameTestHelper helper) {
        var magnetizer = magnetizer(helper);
        helper.setBlock(POSITION, ModMaterialBlocks.IRON_FENCE.get());
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        magnetizer.energy().insert(100);
        var state = helper.getLevel().getBlockState(helper.absolutePos(POSITION));
        helper.assertTrue(
                state.getBlock() instanceof IronFenceBlock, "The test places an iron fence");
        boolean lifted =
                IronFenceBlock.lift(helper.getLevel(), helper.absolutePos(POSITION), player);
        helper.assertTrue(lifted, "A powered magnetizer lifts the climber");
        helper.assertTrue(
                player.getDeltaMovement().y > 0, "The lift adds upward velocity against gravity");
        helper.assertTrue(
                magnetizer.storedEnergy() == 98.0, "One climber share costs two EU per boost");
        helper.succeed();
    }

    static void unpoweredStays(GameTestHelper helper) {
        var magnetizer = magnetizer(helper);
        helper.setBlock(POSITION, ModMaterialBlocks.IRON_FENCE.get());
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        boolean lifted =
                IronFenceBlock.lift(helper.getLevel(), helper.absolutePos(POSITION), player);
        helper.assertTrue(
                !lifted && player.getDeltaMovement().y == 0.0,
                "Without stored EU the fence never lifts anyone");
        magnetizer.energy().insert(1);
        var poor = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.assertTrue(
                !IronFenceBlock.lift(helper.getLevel(), helper.absolutePos(POSITION), poor),
                "One EU cannot cover the two-EU boost share");
        magnetizer.energy().insert(1);
        var climber = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.assertTrue(
                IronFenceBlock.lift(helper.getLevel(), helper.absolutePos(POSITION), climber),
                "Two EU are exactly one boost share");
        helper.succeed();
    }

    private static MagnetizerBlockEntity magnetizer(GameTestHelper helper) {
        var pos = new BlockPos(1, 2, 2);
        helper.setBlock(pos, ModMachines.block(MachineKind.MAGNETIZER));
        return helper.getBlockEntity(pos, MagnetizerBlockEntity.class);
    }

    private MagnetizerTests() {}
}
