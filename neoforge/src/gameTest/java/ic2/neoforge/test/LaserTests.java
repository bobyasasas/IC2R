package ic2.neoforge.test;

import ic2.neoforge.component.ModDataComponents;
import ic2.neoforge.entity.LaserBulletEntity;
import ic2.neoforge.item.ElectricItemEnergy;
import ic2.neoforge.item.MiningLaserItem;
import ic2.neoforge.registration.ModTools;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

/**
 * Legacy mining laser: a mining shot bills 1250 EU and leaves the block's normal drop, the
 * super-heat shot bills 2500 EU and smelts the mined block in place (sand to glass) without a
 * drop, sneak + use cycles the eight modes free of charge, and the explosive shot bills 5000 EU
 * and detonates on impact.
 */
final class LaserTests {
    private static final BlockPos SHOOTER = new BlockPos(2, 2, 2);
    private static final int WALL_X = 6;

    static void miningShotBillsAndBreaks(GameTestHelper helper) {
        ItemStack laser = charged(2000.0, 0);
        Player shooter = aimDownRange(helper, laser, SHOOTER);
        List<BlockPos> wall = buildWall(helper, WALL_X, 3, 1, 2, Blocks.STONE);
        helper.assertTrue(
                ElectricItemEnergy.charge(laser) == 2000.0, "The laser holds the test charge");
        fire(helper, shooter);
        helper.startSequence()
                .thenWaitUntil(
                        () -> {
                            helper.assertTrue(
                                    brokenCount(helper, wall) >= 1,
                                    "The mining beam must break a wall block");
                            assertSingleDrop(helper, room(helper, WALL_X), Items.COBBLESTONE);
                            helper.assertTrue(
                                    ElectricItemEnergy.charge(laser) == 750.0,
                                    "The mining shot bills 1250 EU");
                            assertBeamSpent(helper, room(helper, WALL_X));
                        })
                .thenSucceed();
    }

    static void superheatSmeltsSandToGlass(GameTestHelper helper) {
        ItemStack laser = charged(4000.0, 4);
        Player shooter = aimDownRange(helper, laser, SHOOTER);
        // Sand obeys gravity: a stone pedestal keeps the wall where the beam expects it.
        for (int z = 1; z <= 3; z++) {
            helper.setBlock(new BlockPos(WALL_X, 1, z), Blocks.STONE);
            helper.setBlock(new BlockPos(WALL_X, 2, z), Blocks.STONE);
        }
        List<BlockPos> wall = buildWall(helper, WALL_X, 3, 1, 2, Blocks.SAND);
        fire(helper, shooter);
        helper.startSequence()
                .thenWaitUntil(
                        () -> {
                            long glass =
                                    wall.stream()
                                            .filter(
                                                    pos ->
                                                            helper.getBlockState(pos)
                                                                    .is(Blocks.GLASS))
                                            .count();
                            helper.assertTrue(
                                    glass == 1,
                                    "The super-heat beam must smelt exactly one sand block in "
                                            + "place");
                            long sand =
                                    wall.stream()
                                            .filter(pos -> helper.getBlockState(pos).is(Blocks.SAND))
                                            .count();
                            helper.assertTrue(
                                    sand == wall.size() - 1,
                                    "The super-heat beam must consume exactly one wall block");
                            helper.assertTrue(
                                    dropsIn(helper, room(helper, WALL_X), Items.SAND).isEmpty(),
                                    "The smelted block must not drop");
                            helper.assertTrue(
                                    ElectricItemEnergy.charge(laser) == 1500.0,
                                    "The super-heat shot bills 2500 EU");
                            assertBeamSpent(helper, room(helper, WALL_X));
                        })
                .thenSucceed();
    }

    static void sneakUseCyclesTheModeFree(GameTestHelper helper) {
        ItemStack laser = charged(10000.0, 0);
        Player shooter = helper.makeMockPlayer(GameType.SURVIVAL);
        shooter.setItemInHand(InteractionHand.MAIN_HAND, laser);
        shooter.setShiftKeyDown(true);
        var item = ModTools.MINING_LASER.get();
        for (int expected = 1; expected <= MiningLaserItem.MODE_COUNT; expected++) {
            item.use(helper.getLevel(), shooter, InteractionHand.MAIN_HAND);
            helper.assertTrue(
                    MiningLaserItem.modeOf(laser) == expected % MiningLaserItem.MODE_COUNT,
                    "Sneak + use must cycle the firing mode");
        }
        helper.assertTrue(
                ElectricItemEnergy.charge(laser) == 10000.0,
                "Switching modes must stay free of charge");
        helper.succeed();
    }

    static void explosiveShotDetonates(GameTestHelper helper) {
        // Deep placement mirrors the proven itnt isolation so the crater stays inside the
        // runner's test plot.
        BlockPos shooterPos = new BlockPos(12, 2, 13);
        ItemStack laser = charged(10000.0, 6);
        Player shooter = aimDownRange(helper, laser, shooterPos);
        // Dirt absorbs 1.85 ray power per block against stone's 3.5, so the fixed five-power
        // blast carves a real crater instead of stopping at the first block. Two layers deep
        // keep the crater well above the assert floor wherever the scattered beam lands.
        List<BlockPos> wall =
                new ArrayList<>(buildWall(helper, 16, 3, 13, 3, Blocks.DIRT));
        wall.addAll(buildWall(helper, 17, 3, 13, 3, Blocks.DIRT));
        fire(helper, shooter);
        helper.startSequence()
                .thenWaitUntil(
                        () -> {
                            helper.assertTrue(
                                    brokenCount(helper, wall) >= 4,
                                    "The explosive beam must detonate on impact");
                            helper.assertTrue(
                                    ElectricItemEnergy.charge(laser) == 5000.0,
                                    "The explosive shot bills 5000 EU");
                            assertBeamSpent(helper, room(helper, 16));
                        })
                .thenSucceed();
    }

    private static ItemStack charged(double amount, int mode) {
        ItemStack laser = new ItemStack(ModTools.MINING_LASER.get());
        ElectricItemEnergy.charge(laser, amount, 3, true, false);
        if (mode != 0) {
            laser.set(ModDataComponents.LASER_MODE, mode);
        }
        return laser;
    }

    /** Places a mock shooter at {@code feet} looking down +X with its eye mid block row. */
    private static Player aimDownRange(
            GameTestHelper helper, ItemStack laser, BlockPos feet) {
        Player shooter = helper.makeMockPlayer(GameType.SURVIVAL);
        shooter.setItemInHand(InteractionHand.MAIN_HAND, laser);
        BlockPos absolute = helper.absolutePos(feet);
        shooter.setPos(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 0.5);
        shooter.setYRot(-90.0F);
        shooter.setXRot(0.0F);
        return shooter;
    }

    private static List<BlockPos> buildWall(
            GameTestHelper helper, int x, int yBase, int zBase, int height, Block block) {
        List<BlockPos> wall = new ArrayList<>();
        for (int y = yBase; y < yBase + height; y++) {
            for (int z = zBase; z <= zBase + 2; z++) {
                BlockPos pos = new BlockPos(x, y, z);
                helper.setBlock(pos, block);
                wall.add(pos);
            }
        }
        return wall;
    }

    private static void fire(GameTestHelper helper, Player shooter) {
        ModTools.MINING_LASER
                .get()
                .use(helper.getLevel(), shooter, InteractionHand.MAIN_HAND);
    }

    private static long brokenCount(GameTestHelper helper, List<BlockPos> wall) {
        return wall.stream().filter(pos -> helper.getBlockState(pos).isAir()).count();
    }

    private static AABB room(GameTestHelper helper, int maxX) {
        return new AABB(
                helper.absolutePos(new BlockPos(-3, 0, -3)).getCenter(),
                helper.absolutePos(new BlockPos(maxX + 3, 7, 20)).getCenter());
    }

    private static void assertBeamSpent(GameTestHelper helper, AABB area) {
        helper.assertTrue(
                helper.getLevel()
                        .getEntitiesOfClass(LaserBulletEntity.class, area)
                        .isEmpty(),
                "The beam must be spent after its impact");
    }

    private static List<ItemEntity> dropsIn(GameTestHelper helper, AABB area, Item item) {
        return helper.getLevel()
                .getEntitiesOfClass(ItemEntity.class, area)
                .stream()
                .filter(drop -> drop.getItem().is(item))
                .toList();
    }

    private static void assertSingleDrop(GameTestHelper helper, AABB area, Item item) {
        helper.assertTrue(
                dropsIn(helper, area, item).size() == 1,
                "The beam must leave exactly one drop of the mined block");
    }
}
