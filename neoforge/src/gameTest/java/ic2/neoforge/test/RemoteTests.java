package ic2.neoforge.test;

import ic2.neoforge.registration.ModExplosives;
import ic2.neoforge.world.DynamiteBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;

final class RemoteTests {
    private static final BlockPos POSITION = new BlockPos(8, 8, 8);

    private static ItemStack remote() {
        return ModExplosives.REMOTE.get().getDefaultInstance();
    }

    private static ItemStack linkedRemoteWithDynamite(GameTestHelper helper) {
        var remote = remote();
        var dynamite = ModExplosives.DYNAMITE.get();
        var linked =
                dynamite.defaultBlockState()
                        .setValue(DynamiteBlock.FACING, net.minecraft.core.Direction.UP)
                        .setValue(DynamiteBlock.LINKED, true);
        helper.setBlock(POSITION, linked);
        var links =
                new ic2.neoforge.component.RemoteLinks(
                        java.util.List.of(
                                net.minecraft.core.GlobalPos.of(
                                        helper.getLevel().dimension(), POSITION.immutable())));
        remote.set(ic2.neoforge.component.ModDataComponents.REMOTE_LINKS, links);
        return remote;
    }

    static void remoteDetonatesLinkedDynamite(GameTestHelper helper) {
        var level = helper.getLevel();
        var remote = linkedRemoteWithDynamite(helper);
        remote.use(
                level,
                helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL),
                net.minecraft.world.InteractionHand.MAIN_HAND);
        helper.assertTrue(
                helper.getLevel().getBlockState(POSITION).isAir(),
                "The linked dynamite detonates and clears its block");
        helper.succeed();
    }

    private RemoteTests() {}
}
