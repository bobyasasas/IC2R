package ic2.neoforge.test;

import ic2.neoforge.item.ElectricItemEnergy;
import ic2.neoforge.registration.MaterialDefinition;
import ic2.neoforge.registration.ModArmor;
import ic2.neoforge.registration.ModEntities;
import ic2.neoforge.registration.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Legacy boat family: every boat drops its legacy item (the rubber boat drops the broken
 * rubber boat), lava kills the rubber and carbon boats while the fire-immune electric boat
 * survives and sets its rider on fire, and a driven electric boat burns 4 EU per tick drawn
 * from the rider's worn electric armour.
 */
final class BoatTests {
    static void dropsMatchLegacySuppliers(GameTestHelper helper) {
        var level = helper.getLevel();
        for (int x : new int[] {2, 5, 8}) {
            helper.setBlock(new BlockPos(x, 1, 2), Blocks.STONE);
        }
        var rubber = helper.spawn(ModEntities.RUBBER_BOAT.get(), new BlockPos(2, 2, 2));
        var carbon = helper.spawn(ModEntities.CARBON_BOAT.get(), new BlockPos(5, 2, 2));
        var electric = helper.spawn(ModEntities.ELECTRIC_BOAT.get(), new BlockPos(8, 2, 2));
        rubber.hurtServer(level, level.damageSources().lava(), Float.MAX_VALUE);
        carbon.hurtServer(level, level.damageSources().lava(), Float.MAX_VALUE);
        electric.hurtServer(level, level.damageSources().generic(), 10.0F);
        helper.startSequence()
                .thenWaitUntil(
                        () -> {
                            helper.assertTrue(
                                    !rubber.isAlive() && !carbon.isAlive() && !electric.isAlive(),
                                    "The three hurt boats must be destroyed");
                            assertDrops(
                                    helper,
                                    "Each boat must drop its legacy item",
                                    ModItems.MATERIALS.get(MaterialDefinition.BROKEN_RUBBER_BOAT).get(),
                                    ModItems.CARBON_BOAT.get(),
                                    ModItems.ELECTRIC_BOAT.get());
                        })
                .thenSucceed();
    }

    static void lavaJudgesTheFamily(GameTestHelper helper) {
        // Two layers: boats float, so a single source block can leave the hull clear of the
        // fluid while it bobs; a deep pool keeps the hull in lava like the legacy lava bath.
        var lava = Blocks.LAVA.defaultBlockState();
        for (int x : new int[] {2, 5, 8}) {
            helper.setBlock(new BlockPos(x, 1, 2), Blocks.STONE);
            helper.setBlock(new BlockPos(x, 2, 2), lava);
            helper.setBlock(new BlockPos(x, 3, 2), lava);
        }
        var rubber = helper.spawn(ModEntities.RUBBER_BOAT.get(), new BlockPos(2, 3, 2));
        var carbon = helper.spawn(ModEntities.CARBON_BOAT.get(), new BlockPos(5, 3, 2));
        var electric = helper.spawn(ModEntities.ELECTRIC_BOAT.get(), new BlockPos(8, 3, 2));
        Player rider = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.assertTrue(rider.startRiding(electric), "The rider must mount the boat");
        helper.startSequence()
                .thenIdle(5)
                .thenExecute(
                        () -> {
                            helper.assertTrue(
                                    !rubber.isAlive(), "The rubber boat must die in lava");
                            helper.assertTrue(
                                    !carbon.isAlive(), "The carbon boat must die in lava");
                            helper.assertTrue(
                                    electric.isAlive(),
                                    "The electric boat must be fire immune");
                            helper.assertTrue(
                                    electric.getControllingPassenger() == rider,
                                    "The rider must stay the controlling passenger");
                            helper.assertTrue(
                                    rider.isOnFire(),
                                    "The electric boat must set its rider on fire in lava");
                        })
                .thenSucceed();
    }

    static void drivenBoatDrawsFromWornPack(GameTestHelper helper) {
        var batpack = ModArmor.BATPACK.toStack();
        ElectricItemEnergy.charge(batpack, 5000, 1, true, false);
        Player rider = helper.makeMockPlayer(GameType.SURVIVAL);
        rider.setItemSlot(EquipmentSlot.CHEST, batpack);
        helper.setBlock(new BlockPos(2, 1, 2), Blocks.STONE);
        var boat = helper.spawn(ModEntities.ELECTRIC_BOAT.get(), new BlockPos(2, 2, 2));
        helper.assertTrue(rider.startRiding(boat), "The rider must mount the boat");
        helper.startSequence()
                .thenWaitUntil(
                        () -> {
                            helper.assertTrue(
                                    boat.getControllingPassenger() == rider,
                                    "The rider must stay the controlling passenger");
                            double drawn =
                                    5000
                                            - ElectricItemEnergy.charge(
                                                    rider.getItemBySlot(EquipmentSlot.CHEST));
                            helper.assertTrue(
                                    drawn > 0 && drawn % 4 == 0,
                                    "The driven electric boat must draw 4 EU per tick from the "
                                            + "worn pack");
                        })
                .thenExecute(
                        () ->
                                helper.assertTrue(
                                        boat.isAlive(),
                                        "The boat must survive its own drive current"))
                .thenSucceed();
    }

    /**
     * Entity section visibility on a loaded runner can lag a tick, so the drop assertions poll
     * through the sequence; this helper asserts the room holds exactly one of each boat drop.
     */
    private static void assertDrops(GameTestHelper helper, String message, Item... items) {
        List<ItemEntity> drops =
                helper.getLevel()
                        .getEntitiesOfClass(
                                ItemEntity.class,
                                new AABB(
                                        helper.absolutePos(new BlockPos(1, 1, 1))
                                                .offset(-2, -2, -2)
                                                .getCenter(),
                                        helper.absolutePos(new BlockPos(9, 3, 3))
                                                .offset(2, 4, 4)
                                                .getCenter()));
        helper.assertTrue(drops.size() == items.length, message);
        for (Item item : items) {
            helper.assertTrue(
                    drops.stream().filter(drop -> drop.getItem().is(item)).count() == 1,
                    message);
        }
    }
}
