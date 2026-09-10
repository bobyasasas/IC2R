package ic2.neoforge.test;

import ic2.neoforge.registration.ModExplosives;
import ic2.neoforge.world.DynamiteBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

final class DynamiteTests {
    private static final BlockPos POSITION = new BlockPos(8, 8, 8);

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

    private DynamiteTests() {}
}
