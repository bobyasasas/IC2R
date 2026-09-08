package ic2.neoforge.test;

import ic2.neoforge.explosion.HeatExplosion;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.ExplosionEvent;

import java.util.function.Consumer;

final class HeatExplosionTests {
    private static final BlockPos ORIGIN = new BlockPos(17, 17, 17);

    static void unloadedTerrain(GameTestHelper helper) {
        var pos = new BlockPos(16000000, 64, 16000000);
        var chunks = helper.getLevel().getChunkSource();
        helper.assertTrue(
                !chunks.hasChunk(pos.getX() >> 4, pos.getZ() >> 4),
                "Test begins beyond loaded terrain");
        HeatExplosion.trigger(helper.getLevel(), pos, 10, 0, true);
        helper.assertTrue(
                !chunks.hasChunk(pos.getX() >> 4, pos.getZ() >> 4),
                "Thermal blast never loads terrain to trace or destroy blocks");
        helper.succeed();
    }

    static void cancellation(GameTestHelper helper) {
        helper.setBlock(ORIGIN, Blocks.STONE);
        var origin = helper.absolutePos(ORIGIN);
        var pig = helper.spawn(EntityType.PIG, ORIGIN.above());
        Consumer<ExplosionEvent.Start> guard =
                event -> {
                    if (event.getExplosion() instanceof HeatExplosion
                            && event.getExplosion().center().equals(origin.getCenter()))
                        event.setCanceled(true);
                };
        NeoForge.EVENT_BUS.addListener(guard);
        try {
            helper.assertTrue(
                    !HeatExplosion.trigger(helper.getLevel(), origin, 10, .01f, true),
                    "Protection cancellation must be reported to the boiler");
            helper.assertTrue(
                    helper.getBlockState(ORIGIN).is(Blocks.STONE)
                            && pig.getHealth() == pig.getMaxHealth(),
                    "Cancellation includes origin removal, entities and drops");
        } finally {
            NeoForge.EVENT_BUS.unregister(guard);
        }
        helper.succeed();
    }

    static void detonateFiltering(GameTestHelper helper) {
        helper.setBlock(ORIGIN, Blocks.STONE);
        helper.setBlock(ORIGIN.east(), Blocks.WHITE_WOOL);
        helper.setBlock(ORIGIN.west(), Blocks.WHITE_WOOL);
        var origin = helper.absolutePos(ORIGIN);
        var pig = helper.spawn(EntityType.PIG, ORIGIN.above());
        Consumer<ExplosionEvent.Detonate> guard =
                event -> {
                    if (event.getExplosion() instanceof HeatExplosion
                            && event.getExplosion().center().equals(origin.getCenter())) {
                        event.getAffectedBlocks().remove(origin.east());
                        event.getAffectedEntities().clear();
                    }
                };
        NeoForge.EVENT_BUS.addListener(guard);
        try {
            HeatExplosion.trigger(helper.getLevel(), origin, 10, 0, true);
            helper.assertTrue(
                    helper.getBlockState(ORIGIN.east()).is(Blocks.WHITE_WOOL)
                            && helper.getBlockState(ORIGIN.west()).isAir()
                            && helper.getBlockState(ORIGIN).isAir(),
                    "Detonate listener can preserve selected blocks while the remaining native"
                        + " hooks execute");
            helper.assertTrue(
                    pig.getHealth() == pig.getMaxHealth()
                            && pig.getDeltaMovement().lengthSqr() == 0,
                    "Filtered entities receive neither damage nor knockback");
        } finally {
            NeoForge.EVENT_BUS.unregister(guard);
        }
        helper.succeed();
    }

    static void thermalDamageAndBarriers(GameTestHelper helper) {
        helper.setBlock(ORIGIN, Blocks.STONE);
        helper.setBlock(ORIGIN.east(), Blocks.STONE);
        helper.setBlock(ORIGIN.west(), Blocks.BEDROCK);
        var origin = helper.absolutePos(ORIGIN);
        var pig = helper.spawn(EntityType.PIG, ORIGIN);
        pig.setPos(origin.getCenter());
        HeatExplosion.trigger(helper.getLevel(), origin, 10, 0, true);
        helper.assertTrue(
                pig.getHealth() < pig.getMaxHealth(),
                "Open directions must deliver native explosion damage");
        helper.assertTrue(
                Double.isFinite(pig.getDeltaMovement().lengthSqr()),
                "An entity at the blast center must never receive NaN motion");
        helper.assertTrue(
                helper.getBlockState(ORIGIN.east()).is(Blocks.STONE)
                        && helper.getBlockState(ORIGIN.west()).is(Blocks.BEDROCK),
                "Thermal resistance and unbreakable barriers stop the heat rays");
        helper.succeed();
    }

    private HeatExplosionTests() {}
}
