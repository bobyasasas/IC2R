package ic2.neoforge.test;

import ic2.neoforge.entity.DynamiteEntity;
import ic2.neoforge.registration.ModEntities;
import ic2.neoforge.registration.ModExplosives;
import ic2.neoforge.world.DynamiteBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.util.List;

final class DynamiteTests {
    private static final BlockPos POSITION = new BlockPos(8, 8, 8);
    private static final BlockPos ORIGIN = new BlockPos(17, 17, 17);

    static void placesWithFacingAndSupport(GameTestHelper helper) {
        var dynamite = ModExplosives.DYNAMITE.get();
        var state = dynamite.defaultBlockState().setValue(DynamiteBlock.FACING, Direction.NORTH);
        // A north-facing stick attaches to its support at the south side
        helper.setBlock(POSITION.south(), Blocks.STONE);
        helper.setBlock(POSITION, state);
        var placed = helper.getBlockState(POSITION);
        helper.assertTrue(
                placed.getValue(DynamiteBlock.FACING) == Direction.NORTH,
                "The dynamite keeps its placement facing");
        helper.assertTrue(
                placed.getValue(DynamiteBlock.LINKED) == false, "A fresh stick starts unlinked");
        // Remove the support: the stick pops off and drops an item
        helper.setBlock(POSITION.south(), Blocks.AIR);
        helper.assertTrue(
                helper.getBlockState(POSITION).isAir(), "Losing support pops the dynamite");
        var support = helper.absolutePos(POSITION.south());
        helper.assertTrue(
                !helper.getLevel()
                        .getEntities(
                                EntityType.ITEM,
                                new AABB(support).inflate(1.5),
                                entity -> entity.isAlive())
                        .isEmpty(),
                "The popped stick drops a dynamite item");
        helper.succeed();
    }

    static void linkedStateToggles(GameTestHelper helper) {
        var dynamite = ModExplosives.DYNAMITE.get();
        var unlinked =
                dynamite.defaultBlockState()
                        .setValue(DynamiteBlock.FACING, Direction.UP)
                        .setValue(DynamiteBlock.LINKED, false);
        helper.setBlock(POSITION, unlinked);
        helper.assertFalse(
                helper.getBlockState(POSITION).getValue(DynamiteBlock.LINKED),
                "A fresh stick starts unlinked");
        var linked =
                dynamite.defaultBlockState()
                        .setValue(DynamiteBlock.FACING, Direction.UP)
                        .setValue(DynamiteBlock.LINKED, true);
        helper.setBlock(POSITION, linked);
        helper.assertTrue(
                helper.getBlockState(POSITION).getValue(DynamiteBlock.LINKED),
                "The linked state persists on the block");
        helper.succeed();
    }

    static void redstonePrimesFuse(GameTestHelper helper) {
        helper.setBlock(ORIGIN.below(), Blocks.STONE);
        helper.setBlock(
                ORIGIN,
                ModExplosives.DYNAMITE
                        .get()
                        .defaultBlockState()
                        .setValue(DynamiteBlock.FACING, Direction.UP));
        helper.setBlock(ORIGIN.east(), Blocks.REDSTONE_BLOCK);
        helper.assertTrue(
                helper.getBlockState(ORIGIN).isAir(),
                "A redstone signal primes the stick in place");
        DynamiteEntity charge = soleCharge(helper);
        helper.assertTrue(charge != null, "The primed stick becomes a charge entity");
        if (charge != null) {
            helper.assertTrue(
                    charge.getFuse() <= DynamiteBlock.FUSE_TICKS,
                    "Redstone priming uses the forty-tick fuse");
        }
        helper.startSequence()
                .thenExecuteAfter(
                        45,
                        () -> {
                            helper.assertTrue(
                                    soleCharge(helper) == null,
                                    "The charge detonates once its fuse runs out");
                            helper.assertTrue(
                                    helper.getBlockState(ORIGIN.below()).isAir(),
                                    "The point explosion destroys the support below");
                        })
                .thenSucceed();
    }

    static void playerBreakPrimesFuse(GameTestHelper helper) {
        helper.setBlock(ORIGIN.below(), Blocks.STONE);
        helper.setBlock(
                ORIGIN,
                ModExplosives.DYNAMITE
                        .get()
                        .defaultBlockState()
                        .setValue(DynamiteBlock.FACING, Direction.UP));
        var player =
                new net.neoforged.neoforge.common.util.FakePlayer(
                        helper.getLevel(),
                        new com.mojang.authlib.GameProfile(
                                java.util.UUID.randomUUID(), "ic2-dynamite-test"));
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        player.gameMode.destroyBlock(helper.absolutePos(ORIGIN));
        helper.assertTrue(helper.getBlockState(ORIGIN).isAir(), "Breaking the stick removes it");
        DynamiteEntity charge = soleCharge(helper);
        helper.assertTrue(charge != null, "Breaking the stick primes it instead of dropping it");
        if (charge != null) {
            helper.assertTrue(
                    charge.getFuse() <= DynamiteBlock.FUSE_TICKS,
                    "Breaking priming uses the forty-tick fuse");
        }
        helper.succeed();
    }

    static void explosionChainsFuse(GameTestHelper helper) {
        // An obsidian support survives the blast, so the stick itself is what explodes.
        helper.setBlock(ORIGIN.below(), Blocks.OBSIDIAN);
        helper.setBlock(
                ORIGIN,
                ModExplosives.DYNAMITE
                        .get()
                        .defaultBlockState()
                        .setValue(DynamiteBlock.FACING, Direction.UP));
        var center = helper.absolutePos(ORIGIN.east(2));
        helper.getLevel()
                .explode(
                        null,
                        center.getX() + 0.5,
                        center.getY() + 0.5,
                        center.getZ() + 0.5,
                        3.0F,
                        Level.ExplosionInteraction.TNT);
        helper.assertTrue(
                helper.getBlockState(ORIGIN).isAir(),
                "The neighbouring explosion consumes the stick");
        DynamiteEntity charge = soleCharge(helper);
        helper.assertTrue(charge != null, "The stick chains into a primed charge");
        if (charge != null) {
            helper.assertTrue(
                    charge.getFuse() <= DynamiteBlock.CHAIN_FUSE_TICKS,
                    "Chain priming uses the five-tick short fuse");
        }
        helper.startSequence()
                .thenExecuteAfter(
                        10,
                        () ->
                                helper.assertTrue(
                                        soleCharge(helper) == null,
                                        "The short-fuse charge detonates almost immediately"))
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

    private DynamiteTests() {}
}
