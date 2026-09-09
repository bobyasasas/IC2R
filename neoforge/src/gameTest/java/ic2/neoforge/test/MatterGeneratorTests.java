package ic2.neoforge.test;

import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.machine.MatterGeneratorBlockEntity;
import ic2.neoforge.registration.MaterialDefinition;
import ic2.neoforge.registration.ModCells;
import ic2.neoforge.registration.ModItems;
import ic2.neoforge.registration.ModMachines;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.transfer.item.ItemResource;

final class MatterGeneratorTests {
    private static final BlockPos POSITION = new BlockPos(8, 8, 8);

    private static MatterGeneratorBlockEntity generator(GameTestHelper helper) {
        helper.setBlock(
                POSITION, ModMachines.block(MachineKind.MATTER_GENERATOR).defaultBlockState());
        return helper.getBlockEntity(POSITION, MatterGeneratorBlockEntity.class);
    }

    private static void fillTankAndGenerate(
            MatterGeneratorBlockEntity generator, ServerLevel level) {
        generator.energy().insert(1000000);
        generator.serverTick(level);
    }

    static void scrapAmplifiesAndGenerates(GameTestHelper helper) {
        var level = helper.getLevel();
        var generator = generator(helper);
        var scrap = ModItems.MATERIALS.get(MaterialDefinition.SCRAP).get();
        generator.inventory().set(0, ItemResource.of(scrap.getDefaultInstance()), 1);
        generator.energy().insert(900000);
        generator.serverTick(level);
        helper.assertTrue(
                generator.scrap() == 5000 && generator.energy().stored() == 900000,
                "The scrap item pays 5000 amplification into the counter");
        generator.energy().insert(50000);
        generator.serverTick(level);
        helper.assertTrue(
                generator.scrap() == 0 && generator.energy().stored() == 975000,
                "The next 5000 EU gained refund 25000 EU through the scrap counter");
        generator.serverTick(level);
        helper.assertTrue(generator.tankAmount() == 1, "The stalled buffer generates 1 mB");
        helper.assertTrue(generator.energy().stored() == 0, "Generation drains the whole buffer");
        helper.succeed();
    }

    static void fillsUuMatterCells(GameTestHelper helper) {
        var level = helper.getLevel();
        var generator = generator(helper);
        generator.inventory().set(2, ItemResource.of(ModCells.EMPTY.get().getDefaultInstance()), 1);
        helper.assertTrue(
                generator.inventory().stack(2).getCount() == 1,
                "The empty cell sits in the fill slot");
        for (int batch = 0; batch < 1000; batch++) fillTankAndGenerate(generator, level);
        generator.serverTick(level);
        var filled = generator.inventory().stack(1);
        helper.assertTrue(
                filled.is(ModCells.CELLS.get("uu_matter_cell").get()) && filled.getCount() == 1,
                "The empty cell leaves as a dedicated UU-matter cell in the output slot");
        helper.assertTrue(generator.tankAmount() == 0, "Filling the cell drained the tank");
        helper.succeed();
    }

    static void redstoneGateStopsGeneration(GameTestHelper helper) {
        var level = helper.getLevel();
        var generator = generator(helper);
        generator.energy().insert(1000000);
        helper.setBlock(POSITION.east(), Blocks.REDSTONE_BLOCK);
        for (int tick = 0; tick < 20; tick++) generator.serverTick(level);
        helper.assertTrue(
                generator.tankAmount() == 0 && generator.energy().stored() == 1000000,
                "A redstone gate freezes generation without losing the buffer");
        helper.succeed();
    }

    private MatterGeneratorTests() {}
}
