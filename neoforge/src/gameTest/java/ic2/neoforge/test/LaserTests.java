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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

/**
 * Legacy mining laser: a mining shot bills 1250 EU and leaves the block's normal drop, the
 * super-heat shot bills 2500 EU and smelts the mined block in place (sand to glass) without a
 * drop, sneak + use cycles the eight modes free of charge, and the explosive shot bills 5000 EU
 * and detonates on impact.
 *
 * <p>The runner spaces plots only six blocks apart and runs every test in parallel, so nothing
 * here may travel sideways: every shot is fired straight down from a mock shooter floating high
 * in this plot's own column, the mined pads sit low like every other suite's structures (a
 * distant high explosion cannot descend far enough to still break a low block), and the
 * explosive test detonates inside a stone box under a glass lid with an open centre hole so
 * the twelve-and-a-half block blast never leaves the plot.
 */
final class LaserTests {
    /** Shooter feet position; the mock eye sits 1.62 above and looks straight down. */
    private static final BlockPos SHOOTER = new BlockPos(3, 16, 2);

    static void miningShotBillsAndBreaks(GameTestHelper helper) {
        ItemStack laser = charged(2000.0, 0);
        Player shooter = aimDown(helper, laser);
        buildFloor(helper);
        List<BlockPos> pad = buildPad(helper, 2, Blocks.STONE);
        fire(helper, shooter);
        helper.startSequence()
                .thenWaitUntil(
                        () -> {
                            helper.assertTrue(
                                    brokenCount(helper, pad) >= 1,
                                    "The mining beam must break a pad block");
                            assertSingleDrop(helper, room(helper), Items.COBBLESTONE);
                            helper.assertTrue(
                                    ElectricItemEnergy.charge(laser) == 750.0,
                                    "The mining shot bills 1250 EU");
                            assertBeamSpent(helper, room(helper));
                        })
                .thenSucceed();
    }

    static void superheatSmeltsSandToGlass(GameTestHelper helper) {
        ItemStack laser = charged(4000.0, 4);
        Player shooter = aimDown(helper, laser);
        buildFloor(helper);
        List<BlockPos> sandPad = buildPad(helper, 2, Blocks.SAND);
        fire(helper, shooter);
        helper.startSequence()
                .thenWaitUntil(
                        () -> {
                            long glass =
                                    sandPad.stream()
                                            .filter(pos -> helper.getBlockState(pos).is(Blocks.GLASS))
                                            .count();
                            helper.assertTrue(
                                    glass == 1,
                                    "The super-heat beam must smelt exactly one sand block in "
                                            + "place");
                            long sand =
                                    sandPad.stream()
                                            .filter(pos -> helper.getBlockState(pos).is(Blocks.SAND))
                                            .count();
                            helper.assertTrue(
                                    sand == sandPad.size() - 1,
                                    "The super-heat beam must consume exactly one sand block");
                            helper.assertTrue(
                                    dropsIn(helper, room(helper), Items.SAND).isEmpty(),
                                    "The smelted block must not drop");
                            helper.assertTrue(
                                    ElectricItemEnergy.charge(laser) == 1500.0,
                                    "The super-heat shot bills 2500 EU");
                            assertBeamSpent(helper, room(helper));
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
        ItemStack laser = charged(10000.0, 6);
        Player shooter = aimDown(helper, laser);
        List<BlockPos> dirt = buildBlastBox(helper);
        fire(helper, shooter);
        helper.startSequence()
                .thenWaitUntil(
                        () -> {
                            helper.assertTrue(
                                    brokenCount(helper, dirt) >= 3,
                                    "The explosive beam must detonate on impact");
                            helper.assertTrue(
                                    ElectricItemEnergy.charge(laser) == 5000.0,
                                    "The explosive shot bills 5000 EU");
                            assertBeamSpent(helper, room(helper));
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

    /** Places a mock shooter high over the pad looking straight down. */
    private static Player aimDown(GameTestHelper helper, ItemStack laser) {
        Player shooter = helper.makeMockPlayer(GameType.SURVIVAL);
        shooter.setItemInHand(InteractionHand.MAIN_HAND, laser);
        BlockPos absolute = helper.absolutePos(SHOOTER);
        shooter.setPos(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 0.5);
        shooter.setXRot(90.0F);
        shooter.setYRot(0.0F);
        return shooter;
    }

    /** Catches the mined drop: 5x5 stone under every pad so the drop rests inside the room. */
    private static void buildFloor(GameTestHelper helper) {
        for (int x = 1; x <= 5; x++) {
            for (int z = 0; z <= 4; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
            }
        }
    }

    /** Builds a 3x3 pad centred under the shooter on layer {@code y}. */
    private static List<BlockPos> buildPad(GameTestHelper helper, int y, net.minecraft.world.level.block.Block block) {
        List<BlockPos> pad = new ArrayList<>();
        for (int x = 2; x <= 4; x++) {
            for (int z = 1; z <= 3; z++) {
                BlockPos pos = new BlockPos(x, y, z);
                helper.setBlock(pos, block);
                pad.add(pos);
            }
        }
        return pad;
    }

    /**
     * Builds the detonation box: a 5x5 stone floor at y=1, stone ring walls up to y=5, a
     * 3x3x3 dirt fill inside, and a glass lid at y=5 with the centre block left open over the
     * shooter column. The open hole keeps the detonation point deterministic — the beam always
     * reaches the dirt surface before exploding (punching through glass depends on where the
     * per-tick movement lands relative to the lid plane, which moved the crater between runs) —
     * and every explosion ray still dies in the stone shell long before it could reach a
     * neighbouring plot.
     */
    private static List<BlockPos> buildBlastBox(GameTestHelper helper) {
        buildFloor(helper);
        List<BlockPos> dirt = new ArrayList<>();
        for (int y = 2; y <= 5; y++) {
            for (int x = 1; x <= 5; x++) {
                for (int z = 0; z <= 4; z++) {
                    boolean inside = x >= 2 && x <= 4 && z >= 1 && z <= 3;
                    BlockPos pos = new BlockPos(x, y, z);
                    if (inside && y <= 4) {
                        helper.setBlock(pos, Blocks.DIRT);
                        dirt.add(pos);
                    } else if (inside) {
                        if (!pos.equals(new BlockPos(3, 5, 2))) {
                            helper.setBlock(pos, Blocks.GLASS);
                        }
                    } else {
                        helper.setBlock(pos, Blocks.STONE);
                    }
                }
            }
        }
        return dirt;
    }

    private static void fire(GameTestHelper helper, Player shooter) {
        ModTools.MINING_LASER.get().use(helper.getLevel(), shooter, InteractionHand.MAIN_HAND);
    }

    private static long brokenCount(GameTestHelper helper, List<BlockPos> pad) {
        return pad.stream().filter(pos -> helper.getBlockState(pos).isAir()).count();
    }

    private static AABB room(GameTestHelper helper) {
        return new AABB(
                helper.absolutePos(new BlockPos(0, 0, 0)).getCenter(),
                helper.absolutePos(new BlockPos(9, 19, 6)).getCenter());
    }

    private static void assertBeamSpent(GameTestHelper helper, AABB area) {
        helper.assertTrue(
                helper.getLevel().getEntitiesOfClass(LaserBulletEntity.class, area).isEmpty(),
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
