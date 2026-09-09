package ic2.neoforge.test;

import ic2.neoforge.item.ElectricItemEnergy;
import ic2.neoforge.registration.ModTools;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;

final class DrillItemTests {
    private static final BlockPos POSITION = new BlockPos(2, 2, 2);

    static void speedAndDrops(GameTestHelper helper) {
        helper.setBlock(POSITION, Blocks.STONE.defaultBlockState());
        var stone = helper.getBlockState(POSITION);
        var drill = ModTools.DRILL.toStack();
        var item = ModTools.DRILL.get();
        helper.assertTrue(
                item.getDestroySpeed(drill, stone) == 1.0F,
                "An uncharged drill keeps the hand-mining speed on stone");
        ElectricItemEnergy.charge(drill, 1000, 1, true, false);
        helper.assertTrue(
                item.getDestroySpeed(drill, stone) == 8.0F,
                "A charged drill mines stone at the legacy 8.0 speed");
        var diamondDrill = ModTools.DIAMOND_DRILL.toStack();
        ElectricItemEnergy.charge(diamondDrill, 1000, 1, true, false);
        helper.assertTrue(
                ModTools.DIAMOND_DRILL.get().getDestroySpeed(diamondDrill, stone) == 16.0F,
                "The diamond drill mines stone at the legacy 16.0 speed");
        helper.setBlock(POSITION, Blocks.DIRT.defaultBlockState());
        helper.assertTrue(
                item.getDestroySpeed(drill, helper.getBlockState(POSITION)) == 8.0F,
                "The drill covers shovel blocks like the legacy effective list");
        helper.assertTrue(
                item.isCorrectToolForDrops(drill, stone),
                "Drops stay correct while uncharged; only the speed collapses");
        helper.setBlock(POSITION, Blocks.OBSIDIAN.defaultBlockState());
        var obsidian = helper.getBlockState(POSITION);
        helper.assertTrue(
                !item.isCorrectToolForDrops(drill, obsidian),
                "The iron-tier drill cannot harvest obsidian");
        helper.assertTrue(
                ModTools.DIAMOND_DRILL.get().isCorrectToolForDrops(diamondDrill, obsidian),
                "The diamond-tier drill harvests obsidian");
        helper.setBlock(POSITION, Blocks.OAK_LOG.defaultBlockState());
        helper.assertTrue(
                !item.isCorrectToolForDrops(drill, helper.getBlockState(POSITION)),
                "Blocks outside the pickaxe and shovel lists stay ineffective");
        helper.succeed();
    }

    static void dischargePerBlock(GameTestHelper helper) {
        var level = helper.getLevel();
        helper.setBlock(POSITION, Blocks.STONE.defaultBlockState());
        var state = helper.getBlockState(POSITION);
        var pos = helper.absolutePos(POSITION);
        var drill = ModTools.DRILL.toStack();
        ElectricItemEnergy.charge(drill, 1000, 1, true, false);
        ModTools.DRILL.get().mineBlock(drill, level, state, pos, null);
        helper.assertTrue(
                ElectricItemEnergy.charge(drill) == 950,
                "One mined block costs the legacy 50 EU operation charge");
        var empty = ModTools.DRILL.toStack();
        ElectricItemEnergy.charge(empty, 10, 1, true, false);
        ModTools.DRILL.get().mineBlock(empty, level, state, pos, null);
        helper.assertTrue(
                ElectricItemEnergy.charge(empty) == 10,
                "An insufficient charge is not partially drained");
        var iridium = ModTools.IRIDIUM_DRILL.toStack();
        ElectricItemEnergy.charge(iridium, 1000, 3, true, false);
        ModTools.IRIDIUM_DRILL.get().mineBlock(iridium, level, state, pos, null);
        helper.assertTrue(
                ElectricItemEnergy.charge(iridium) == 200,
                "The iridium drill pays the legacy 800 EU operation charge");
        helper.setBlock(POSITION, Blocks.DANDELION.defaultBlockState());
        ModTools.DRILL.get().mineBlock(drill, level, helper.getBlockState(POSITION), pos, null);
        helper.assertTrue(
                ElectricItemEnergy.charge(drill) == 950, "Zero-hardness blocks consume no energy");
        helper.succeed();
    }

    static void minerConstants(GameTestHelper helper) {
        var drill = ModTools.DRILL.get().specification();
        helper.assertTrue(
                drill.capacity() == 30000 && drill.transferLimit() == 100 && drill.tier() == 1,
                "The drill keeps the legacy 30000 EU tier-1 buffer");
        helper.assertTrue(
                ModTools.DRILL.get().minerEnergyPerTick() == 6
                        && ModTools.DRILL.get().minerDuration() == 200
                        && ModTools.DRILL.get().harvestEnergyCost() == 50
                        && ModTools.DRILL.get().fortuneLevel() == 0,
                "The miner paces basic drill blocks over 200 ticks at 6 EU per tick");
        var diamond = ModTools.DIAMOND_DRILL.get();
        helper.assertTrue(
                diamond.minerEnergyPerTick() == 20
                        && diamond.minerDuration() == 50
                        && diamond.harvestEnergyCost() == 80
                        && diamond.fortuneLevel() == 0,
                "The diamond drill mines miner blocks over 50 ticks at 20 EU per tick");
        var iridium = ModTools.IRIDIUM_DRILL.get().specification();
        helper.assertTrue(
                iridium.capacity() == 300000
                        && iridium.transferLimit() == 1000
                        && iridium.tier() == 3,
                "The iridium drill keeps the legacy 300000 EU tier-3 buffer");
        helper.assertTrue(
                ModTools.IRIDIUM_DRILL.get().minerEnergyPerTick() == 200
                        && ModTools.IRIDIUM_DRILL.get().minerDuration() == 20
                        && ModTools.IRIDIUM_DRILL.get().harvestEnergyCost() == 800
                        && ModTools.IRIDIUM_DRILL.get().fortuneLevel() == 3,
                "The iridium drill mines miner blocks over 20 ticks with fortune III");
        helper.succeed();
    }

    private DrillItemTests() {}
}
