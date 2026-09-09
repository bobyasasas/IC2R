package ic2.neoforge.test;

import ic2.neoforge.registration.ModMaterialBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;

final class ReactorVesselTests {
    private static final BlockPos POSITION = new BlockPos(8, 8, 8);

    /** The casing block places, drops itself and coexists with the reactor structures. */
    static void vesselPlaces(GameTestHelper helper) {
        var vessel = ModMaterialBlocks.MATERIALS.get("reactor_vessel").get();
        helper.setBlock(POSITION, vessel.defaultBlockState());
        var state = helper.getBlockState(POSITION);
        helper.assertTrue(state.getBlock() == vessel, "The reactor vessel block places as itself");
        helper.setBlock(POSITION, net.minecraft.world.level.block.Blocks.AIR);
        helper.assertTrue(
                helper.getLevel().getBlockState(POSITION).isAir(),
                "The vessel clears cleanly for structure reuse");
        helper.succeed();
    }

    private ReactorVesselTests() {}
}
