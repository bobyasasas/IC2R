package ic2.neoforge.test;

import ic2.neoforge.entity.ItntEntity;
import ic2.neoforge.registration.ModEntities;
import ic2.neoforge.registration.ModExplosives;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.util.List;

final class ItntTests {
    private static final BlockPos ORIGIN = new BlockPos(17, 17, 17);

    static void redstonePrimesFusedCharge(GameTestHelper helper) {
        helper.setBlock(ORIGIN, ModExplosives.ITNT.get());
        helper.setBlock(ORIGIN.east(), Blocks.REDSTONE_BLOCK);
        helper.assertTrue(
                helper.getBlockState(ORIGIN).isAir(),
                "A redstone signal primes the charge in place");
        ItntEntity charge = soleCharge(helper);
        helper.assertTrue(charge != null, "The primed charge entity spawns");
        helper.assertTrue(charge.getFuse() <= 60, "The charge keeps the legacy sixty-tick fuse");
        helper.succeed();
    }

    static void fuseDetonates(GameTestHelper helper) {
        // A destructible pad inside the bedrock isolation room proves the blast power
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                helper.setBlock(ORIGIN.offset(dx, -1, dz), Blocks.STONE);
            }
        }
        helper.setBlock(ORIGIN, ModExplosives.ITNT.get());
        helper.setBlock(ORIGIN.east(), Blocks.REDSTONE_BLOCK);
        helper.startSequence()
                .thenExecuteAfter(
                        62,
                        () -> {
                            helper.assertTrue(
                                    soleCharge(helper) == null,
                                    "The charge detonates once its fuse runs out");
                            int destroyed = 0;
                            for (int dx = -2; dx <= 2; dx++) {
                                for (int dz = -2; dz <= 2; dz++) {
                                    if (helper.getBlockState(ORIGIN.offset(dx, -1, dz)).isAir()) {
                                        destroyed++;
                                    }
                                }
                            }
                            helper.assertTrue(
                                    destroyed >= 4,
                                    "The 5.5-strength blast destroys the ground below");
                        })
                .thenSucceed();
    }

    static void playerBreakPrimes(GameTestHelper helper) {
        helper.setBlock(ORIGIN, ModExplosives.ITNT.get());
        var player =
                new net.neoforged.neoforge.common.util.FakePlayer(
                        helper.getLevel(),
                        new com.mojang.authlib.GameProfile(
                                java.util.UUID.randomUUID(), "ic2-itnt-test"));
        player.gameMode.changeGameModeForPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.gameMode.destroyBlock(helper.absolutePos(ORIGIN));
        helper.assertTrue(
                helper.getBlockState(ORIGIN).isAir(),
                "Breaking the charge removes it without a drop");
        helper.assertTrue(
                soleCharge(helper) != null, "Breaking the charge primes it (explodeOnRemoval)");
        helper.succeed();
    }

    static void chainReaction(GameTestHelper helper) {
        helper.setBlock(ORIGIN, ModExplosives.ITNT.get());
        var center = helper.absolutePos(ORIGIN);
        helper.getLevel()
                .explode(
                        null,
                        center.getX() + 1.5,
                        center.getY() + 0.5,
                        center.getZ() + 0.5,
                        3.0F,
                        Level.ExplosionInteraction.TNT);
        ItntEntity charge = soleCharge(helper);
        helper.assertTrue(
                charge != null, "A neighbouring explosion primes the charge (chain reaction)");
        helper.assertTrue(charge.getFuse() < 60, "Chain-primed charges use the legacy short fuse");
        helper.succeed();
    }

    private static ItntEntity soleCharge(GameTestHelper helper) {
        List<ItntEntity> found =
                helper.getLevel()
                        .getEntities(
                                ModEntities.ITNT.get(),
                                new AABB(helper.absolutePos(ORIGIN)).inflate(4.0),
                                entity -> entity.isAlive());
        return found.isEmpty() ? null : found.getFirst();
    }
}
