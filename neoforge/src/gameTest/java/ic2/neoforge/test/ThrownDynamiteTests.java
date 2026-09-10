package ic2.neoforge.test;

import ic2.neoforge.entity.DynamiteEntity;
import ic2.neoforge.registration.ModEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

final class ThrownDynamiteTests {
    private static final BlockPos ORIGIN = new BlockPos(17, 17, 17);

    static void thrownDynamiteDetonates(GameTestHelper helper) {
        Level level = helper.getLevel();
        placeStonePad(helper);
        DynamiteEntity charge = spawn(helper, level, false);
        helper.assertTrue(charge.getFuse() == 100, "A thrown charge carries the 100-tick fuse");
        helper.startSequence()
                .thenExecuteAfter(
                        120,
                        () -> {
                            helper.assertTrue(
                                    soleCharge(helper) == null,
                                    "The charge detonates once its fuse runs out");
                            helper.assertTrue(
                                    destroyedPadBlocks(helper) >= 1,
                                    "The point explosion destroys the ground below");
                        })
                .thenSucceed();
    }

    static void stickyDynamiteFuseAccelerates(GameTestHelper helper) {
        Level level = helper.getLevel();
        placeStonePad(helper);
        DynamiteEntity plain = spawn(helper, level, false);
        Vec3 stickySpot = Vec3.atCenterOf(helper.absolutePos(ORIGIN.east(3)));
        DynamiteEntity sticky = new DynamiteEntity(ModEntities.STICKY_DYNAMITE.get(), level);
        sticky.setPos(stickySpot.x, stickySpot.y, stickySpot.z);
        level.addFreshEntity(sticky);
        helper.assertTrue(
                sticky.isSticky() && !plain.isSticky(),
                "The sticky entity type carries the anchoring behaviour");
        helper.startSequence()
                .thenExecuteAfter(
                        20,
                        () -> {
                            DynamiteEntity plainNow = findCharge(helper, false);
                            DynamiteEntity stickyNow = findCharge(helper, true);
                            helper.assertTrue(
                                    plainNow != null && stickyNow != null,
                                    "Both charges survive the first twenty ticks");
                            if (plainNow != null && stickyNow != null) {
                                helper.assertTrue(
                                        stickyNow.getFuse() < plainNow.getFuse() - 9,
                                        "The sticky charge burns three extra fuse ticks per "
                                                + "grounded tick");
                            }
                        })
                .thenExecuteAfter(
                        40,
                        () -> {
                            helper.assertTrue(
                                    findCharge(helper, true) == null,
                                    "The sticky charge detonates well before the plain one");
                            helper.assertTrue(
                                    findCharge(helper, false) != null,
                                    "The plain charge is still burning");
                        })
                .thenExecuteAfter(
                        80,
                        () -> {
                            helper.assertTrue(
                                    soleCharge(helper) == null,
                                    "The plain charge detonates around its hundred-tick fuse");
                        })
                .thenSucceed();
    }

    static void waterDisarmsFuse(GameTestHelper helper) {
        Level level = helper.getLevel();
        helper.setBlock(ORIGIN.below(), Blocks.STONE);
        helper.setBlock(ORIGIN, Blocks.WATER);
        DynamiteEntity charge = spawnAt(helper, level, false, ORIGIN.above());
        helper.startSequence()
                .thenExecuteAfter(
                        150,
                        () -> {
                            DynamiteEntity remaining = soleCharge(helper);
                            helper.assertTrue(
                                    remaining != null,
                                    "A charge dropped into water stays a harmless dud");
                            if (remaining != null) {
                                helper.assertTrue(
                                        remaining.getFuse() > 500,
                                        "Water saturates the fuse instead of burning it down");
                            }
                        })
                .thenSucceed();
    }

    private static DynamiteEntity spawn(GameTestHelper helper, Level level, boolean sticky) {
        return spawnAt(helper, level, sticky, ORIGIN);
    }

    private static DynamiteEntity spawnAt(
            GameTestHelper helper, Level level, boolean sticky, BlockPos pos) {
        Vec3 world = Vec3.atCenterOf(helper.absolutePos(pos));
        DynamiteEntity charge =
                new DynamiteEntity(
                        sticky ? ModEntities.STICKY_DYNAMITE.get() : ModEntities.DYNAMITE.get(),
                        level);
        charge.setPos(world.x, world.y, world.z);
        level.addFreshEntity(charge);
        return charge;
    }

    private static void placeStonePad(GameTestHelper helper) {
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                helper.setBlock(ORIGIN.offset(dx, -1, dz), Blocks.STONE);
            }
        }
    }

    private static int destroyedPadBlocks(GameTestHelper helper) {
        int destroyed = 0;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (helper.getBlockState(ORIGIN.offset(dx, -1, dz)).isAir()) {
                    destroyed++;
                }
            }
        }
        return destroyed;
    }

    private static DynamiteEntity findCharge(GameTestHelper helper, boolean sticky) {
        List<DynamiteEntity> found =
                helper.getLevel()
                        .getEntities(
                                sticky
                                        ? ModEntities.STICKY_DYNAMITE.get()
                                        : ModEntities.DYNAMITE.get(),
                                new AABB(helper.absolutePos(ORIGIN)).inflate(6.0),
                                entity -> entity.isAlive());
        return found.isEmpty() ? null : found.getFirst();
    }

    private static DynamiteEntity soleCharge(GameTestHelper helper) {
        DynamiteEntity plain = findCharge(helper, false);
        return plain != null ? plain : findCharge(helper, true);
    }

    private ThrownDynamiteTests() {}
}
