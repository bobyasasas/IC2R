package ic2.neoforge.test;

import ic2.neoforge.machine.GeneratorBlockEntity;
import ic2.neoforge.machine.MachineBlock;
import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.registration.ModMachines;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.Level;

/** AE2 acceptor bridge: EU flows into an energy handler at two AE per EU. */
final class Ae2BridgeTests {
    private static final BlockPos GENERATOR = new BlockPos(1, 1, 2),
            DIRECT_ACCEPTOR = new BlockPos(2, 1, 2),
            CABLE = new BlockPos(2, 1, 2),
            ACCEPTOR = new BlockPos(3, 1, 2);

    private static GeneratorBlockEntity generator(GameTestHelper helper) {
        helper.setBlock(
                GENERATOR,
                ModMachines.block(MachineKind.GENERATOR)
                        .defaultBlockState()
                        .setValue(MachineBlock.FACING, Direction.EAST));
        return helper.getBlockEntity(GENERATOR, GeneratorBlockEntity.class);
    }

    private static TestEnergyStorage sink(GameTestHelper helper, BlockPos relativePos) {
        Level level = helper.getLevel();
        return TestEnergyStorage.at(level, helper.absolutePos(relativePos));
    }

    static void bridgeFeedsAcceptorOverCable(GameTestHelper helper) {
        GeneratorBlockEntity generator = generator(helper);
        helper.setBlock(CABLE, ModMachines.CABLES.get("tin_cable").get());
        helper.setBlock(ACCEPTOR, RegistrationTests.TEST_ACCEPTOR.get());
        TestEnergyStorage sink = sink(helper, ACCEPTOR);
        generator.energy().restore(640);
        helper.runAtTickTime(
                30,
                () -> {
                    helper.assertTrue(
                            sink.stored() > 0,
                            "The acceptor handler receives EU forwarded at two AE per EU");
                    helper.assertTrue(
                            generator.energy().stored() < 640,
                            "The generator supplies the bridged acceptor through the cable");
                    helper.succeed();
                });
    }

    static void bridgeChargesTwoAePerEuExactly(GameTestHelper helper) {
        GeneratorBlockEntity generator = generator(helper);
        helper.setBlock(DIRECT_ACCEPTOR, RegistrationTests.TEST_ACCEPTOR.get());
        TestEnergyStorage sink = sink(helper, DIRECT_ACCEPTOR);
        generator.energy().restore(640);
        helper.runAtTickTime(
                30,
                () -> {
                    double delivered = 640 - generator.energy().stored();
                    helper.assertTrue(
                            delivered > 0, "A directly adjacent acceptor draws from the generator");
                    helper.assertTrue(
                            sink.stored() == Math.round(delivered * 2),
                            "Two AE land per EU with no loss on the direct hop");
                    helper.succeed();
                });
    }

    static void bridgeWithoutPathDrawsNothing(GameTestHelper helper) {
        GeneratorBlockEntity generator = generator(helper);
        helper.setBlock(ACCEPTOR, RegistrationTests.TEST_ACCEPTOR.get());
        TestEnergyStorage sink = sink(helper, ACCEPTOR);
        generator.energy().restore(640);
        helper.runAtTickTime(
                30,
                () -> {
                    helper.assertTrue(
                            sink.stored() == 0,
                            "An unreachable acceptor never receives energy");
                    helper.assertTrue(
                            generator.energy().stored() == 640,
                            "Without a path the generator keeps its charge");
                    helper.succeed();
                });
    }
}
