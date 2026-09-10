package ic2.neoforge.test;

import ic2.neoforge.registration.ModExplosives;
import ic2.neoforge.world.Ic2Explosion;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

final class Ic2ExplosionTests {
    private static final BlockPos ORIGIN = new BlockPos(17, 17, 17);

    static void rayCraterStopsAtAbsorption(GameTestHelper helper) {
        helper.setBlock(ORIGIN.below(), Blocks.STONE);
        helper.setBlock(ORIGIN.below(2), Blocks.STONE);
        explode(helper, 4.5F, 1.0F, Ic2Explosion.Type.Normal);
        helper.assertTrue(
                helper.getBlockState(ORIGIN.below()).isAir(),
                "A four-and-a-half-power ray destroys the first stone layer");
        helper.assertTrue(
                helper.getBlockState(ORIGIN.below(2)).is(Blocks.STONE),
                "The exhausted ray cannot cut a second stone layer");
        helper.succeed();
    }

    static void ultraResistantBedrockIsPassedThrough(GameTestHelper helper) {
        helper.setBlock(ORIGIN.below(), Blocks.BEDROCK);
        helper.setBlock(ORIGIN.below(2), Blocks.STONE);
        explode(helper, 5.0F, 1.0F, Ic2Explosion.Type.Normal);
        helper.assertTrue(
                helper.getBlockState(ORIGIN.below()).is(Blocks.BEDROCK),
                "Ultra resistant bedrock survives the blast untouched");
        helper.assertTrue(
                helper.getBlockState(ORIGIN.below(2)).isAir(),
                "Rays punch straight through the bedrock and destroy what is behind it");
        helper.succeed();
    }

    static void stoneShieldStopsRays(GameTestHelper helper) {
        helper.setBlock(ORIGIN.below(), Blocks.STONE);
        helper.setBlock(ORIGIN.below(2), Blocks.STONE);
        helper.setBlock(ORIGIN.below(3), Blocks.STONE);
        explode(helper, 5.0F, 1.0F, Ic2Explosion.Type.Normal);
        helper.assertTrue(
                helper.getBlockState(ORIGIN.below()).isAir(),
                "The first shield layer absorbs the blow and is destroyed");
        helper.assertTrue(
                helper.getBlockState(ORIGIN.below(2)).is(Blocks.STONE),
                "A thick enough shield spends the remaining ray power");
        helper.assertTrue(
                helper.getBlockState(ORIGIN.below(3)).is(Blocks.STONE),
                "The stopped ray cannot reach the block behind the shield");
        helper.succeed();
    }

    static void zeroDropRateSuppressesDrops(GameTestHelper helper) {
        placeStonePad(helper, 1);
        explode(helper, 4.5F, 0.0F, Ic2Explosion.Type.Normal);
        helper.assertTrue(
                helper.getBlockState(ORIGIN.below()).isAir(),
                "The pad is still destroyed with a zero drop rate");
        helper.assertTrue(itemsInRoom(helper).isEmpty(), "A zero drop rate spawns no debris");
        helper.succeed();
    }

    static void fullDropRateKeepsDrops(GameTestHelper helper) {
        placeStonePad(helper, 1);
        explode(helper, 4.5F, 1.0F, Ic2Explosion.Type.Normal);
        helper.assertTrue(
                !itemsInRoom(helper).isEmpty(),
                "A full drop rate turns every destroyed block into debris");
        helper.succeed();
    }

    static void accumulatedRayDamageKillsMob(GameTestHelper helper) {
        var chicken = helper.spawn(EntityType.CHICKEN, ORIGIN.east(2));
        explode(helper, 4.5F, 1.0F, Ic2Explosion.Type.Normal);
        helper.assertTrue(!chicken.isAlive(), "Accumulated ray damage kills a nearby animal");
        helper.succeed();
    }

    static void nuclearTypeResolvesNukeDamageSource(GameTestHelper helper) {
        var chicken = helper.spawn(EntityType.CHICKEN, ORIGIN.east(2));
        explode(helper, 4.5F, 1.0F, Ic2Explosion.Type.Nuclear);
        helper.assertTrue(
                !chicken.isAlive(), "The nuclear type resolves the ic2:nuke damage source");
        helper.succeed();
    }

    static void itntBlastDropsDebris(GameTestHelper helper) {
        placeStonePad(helper, 2);
        helper.setBlock(ORIGIN, ModExplosives.ITNT.get());
        helper.setBlock(ORIGIN.east(), Blocks.REDSTONE_BLOCK);
        helper.startSequence()
                .thenExecuteAfter(
                        62,
                        () ->
                                helper.assertTrue(
                                        itemsInRoom(helper).size() >= 2,
                                        "The industrial blast drops most destroyed blocks "
                                                + "(legacy drop rate 0.9)"))
                .thenSucceed();
    }

    private static void explode(
            GameTestHelper helper, float power, float dropRate, Ic2Explosion.Type type) {
        ServerLevel level = helper.getLevel();
        Vec3 center = Vec3.atCenterOf(helper.absolutePos(ORIGIN));
        new Ic2Explosion(level, null, null, center.x, center.y, center.z, power, dropRate, type, 0)
                .doExplosion();
    }

    private static void placeStonePad(GameTestHelper helper, int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                helper.setBlock(ORIGIN.offset(dx, -1, dz), Blocks.STONE);
            }
        }
    }

    private static List<ItemEntity> itemsInRoom(GameTestHelper helper) {
        BlockPos low = helper.absolutePos(new BlockPos(0, 0, 0));
        BlockPos high = helper.absolutePos(new BlockPos(34, 34, 34));
        return helper.getLevel()
                .getEntities(
                        EntityType.ITEM,
                        new AABB(
                                low.getX(),
                                low.getY(),
                                low.getZ(),
                                high.getX(),
                                high.getY(),
                                high.getZ()),
                        ItemEntity::isAlive);
    }

    private Ic2ExplosionTests() {}
}
