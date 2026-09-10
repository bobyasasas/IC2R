package ic2.neoforge.test;

import ic2.neoforge.component.ModDataComponents;
import ic2.neoforge.component.RemoteLinks;
import ic2.neoforge.entity.DynamiteEntity;
import ic2.neoforge.registration.ModEntities;
import ic2.neoforge.registration.ModExplosives;
import ic2.neoforge.world.DynamiteBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;

import java.util.List;

final class RemoteTests {
    private static final BlockPos ORIGIN = new BlockPos(17, 17, 17);

    private static ItemStack linkedRemoteWithDynamite(GameTestHelper helper) {
        var remote = ModExplosives.REMOTE.get().getDefaultInstance();
        var dynamite = ModExplosives.DYNAMITE.get();
        var linked =
                dynamite.defaultBlockState()
                        .setValue(DynamiteBlock.FACING, Direction.UP)
                        .setValue(DynamiteBlock.LINKED, true);
        helper.setBlock(ORIGIN, linked);
        var links =
                new RemoteLinks(
                        List.of(
                                GlobalPos.of(
                                        helper.getLevel().dimension(),
                                        helper.absolutePos(ORIGIN))));
        remote.set(ModDataComponents.REMOTE_LINKS, links);
        return remote;
    }

    static void remoteDetonatesLinkedDynamite(GameTestHelper helper) {
        var level = helper.getLevel();
        var remote = linkedRemoteWithDynamite(helper);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, remote);
        remote.use(level, player, InteractionHand.MAIN_HAND);
        helper.assertTrue(
                helper.getBlockState(ORIGIN).isAir(),
                "The linked dynamite clears its block when triggered");
        DynamiteEntity charge = soleCharge(helper);
        helper.assertTrue(charge != null, "Remote triggering spawns the fuse charge");
        if (charge != null) {
            helper.assertTrue(
                    charge.getFuse() <= DynamiteBlock.FUSE_TICKS,
                    "Remote triggering uses the forty-tick fuse");
        }
        helper.startSequence()
                .thenExecuteAfter(
                        45,
                        () ->
                                helper.assertTrue(
                                        soleCharge(helper) == null,
                                        "The charge detonates once its fuse runs out"))
                .thenSucceed();
    }

    private static DynamiteEntity soleCharge(GameTestHelper helper) {
        List<DynamiteEntity> found =
                helper.getLevel()
                        .getEntities(
                                ModEntities.STICKY_DYNAMITE.get(),
                                new AABB(helper.absolutePos(ORIGIN)).inflate(4.0),
                                entity -> entity.isAlive());
        return found.isEmpty() ? null : found.getFirst();
    }

    private RemoteTests() {}
}
