package ic2.neoforge.test;

import ic2.neoforge.item.ElectricItemEnergy;
import ic2.neoforge.machine.BlastFurnaceBlockEntity;
import ic2.neoforge.machine.ElectricWorkBlockEntity;
import ic2.neoforge.machine.MachineBlock;
import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.registration.MaterialDefinition;
import ic2.neoforge.registration.ModCells;
import ic2.neoforge.registration.ModItems;
import ic2.neoforge.registration.ModMachines;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.transfer.item.ItemResource;

final class BlastFurnaceTests {
    private static final BlockPos POSITION = new BlockPos(8, 8, 8);

    private static BlastFurnaceBlockEntity furnace(GameTestHelper helper) {
        helper.setBlock(
                POSITION,
                ModMachines.block(MachineKind.BLAST_FURNACE)
                        .defaultBlockState()
                        .setValue(MachineBlock.FACING, Direction.EAST));
        return helper.getBlockEntity(POSITION, BlastFurnaceBlockEntity.class);
    }

    private static ElectricWorkBlockEntity heater(
            GameTestHelper helper, BlockPos pos, Direction facing) {
        helper.setBlock(
                pos,
                ModMachines.block(MachineKind.ELECTRIC_HEAT_GENERATOR)
                        .defaultBlockState()
                        .setValue(MachineBlock.FACING, facing));
        var source = helper.getBlockEntity(pos, ElectricWorkBlockEntity.class);
        for (int slot = 0; slot < 10; slot++)
            source.inventory()
                    .set(
                            slot,
                            ItemResource.of(ModItems.MATERIALS.get(MaterialDefinition.COIL).get()),
                            1);
        source.energy().insert(10000);
        return source;
    }

    private static void loadAirCells(BlastFurnaceBlockEntity furnace, int count) {
        furnace.inventory().set(3, ItemResource.of(ModCells.CELLS.get("air_cell").get()), count);
    }

    static void smeltsIronIntoSteel(GameTestHelper helper) {
        var furnace = furnace(helper);
        var heater = heater(helper, POSITION.east(), Direction.WEST);
        var crystal = ModItems.ENERGY_CRYSTAL.get().getDefaultInstance();
        ElectricItemEnergy.charge(crystal, 1000000, 3, true, false);
        heater.inventory().set(10, ItemResource.of(crystal), 1);
        loadAirCells(furnace, 8);
        furnace.inventory().set(0, ItemResource.of(Items.GOLD_INGOT), 1);
        var steel = ModItems.MATERIALS.get(MaterialDefinition.STEEL_INGOT).get();
        var slag = ModItems.MATERIALS.get(MaterialDefinition.SLAG).get();
        StringBuilder log = new StringBuilder();
        var check =
                net.minecraft.world.item.crafting.RecipeManager.createCheck(
                        ic2.neoforge.registration.ModProcessingRecipes.BLAST_FURNACE_TYPE.get());
        helper.runAfterDelay(
                300,
                () ->
                        log.append(" t300 heat ")
                                .append(furnace.heat())
                                .append(" found ")
                                .append(
                                        check.getRecipeFor(
                                                        new net.minecraft.world.item.crafting
                                                                .SingleRecipeInput(
                                                                new net.minecraft.world.item
                                                                        .ItemStack(
                                                                        Items.GOLD_INGOT)),
                                                        helper.getLevel())
                                                .isPresent()));
        helper.runAfterDelay(
                700,
                () ->
                        log.append(" t700 heat ")
                                .append(furnace.heat())
                                .append(" prog ")
                                .append(furnace.progress()));
        helper.runAfterDelay(
                1100,
                () -> {
                    if (!furnace.inventory().stack(1).is(steel))
                        helper.assertTrue(false, "no steel." + log);
                    helper.assertTrue(
                            furnace.inventory().stack(1).is(steel)
                                    && furnace.inventory().stack(2).is(slag),
                            "The hot furnace smelts the fast probe recipe into steel and"
                                    + " slag");
                    helper.assertTrue(
                            furnace.inventory().stack(0).isEmpty(), "The gold ingot is consumed");
                    helper.assertTrue(
                            furnace.tankAmount() == 7800,
                            "The 200-tick recipe draws 200 mB of air");
                    helper.succeed();
                });
    }

    static void staysColdWithoutHeat(GameTestHelper helper) {
        var level = helper.getLevel();
        var furnace = furnace(helper);
        furnace.inventory().set(0, ItemResource.of(Items.IRON_INGOT), 1);
        for (int tick = 0; tick < 2000; tick++) furnace.serverTick(level);
        helper.assertTrue(furnace.heat() == 0, "Without a heat source the furnace stays cold");
        helper.assertTrue(furnace.progress() == 0, "No progress happens while cold");
        helper.assertTrue(
                furnace.inventory().stack(0).getCount() == 1,
                "The iron ingot survives a cold furnace");
        helper.succeed();
    }

    static void airCellsFillTank(GameTestHelper helper) {
        var level = helper.getLevel();
        var furnace = furnace(helper);
        loadAirCells(furnace, 8);
        for (int tick = 0; tick < 10; tick++) furnace.serverTick(level);
        helper.assertTrue(furnace.tankAmount() == 8000, "Eight air cells fill the air tank");
        helper.assertTrue(
                furnace.inventory().stack(4).getCount() == 8,
                "The spent cells collect in the return slot");
        helper.succeed();
    }

    private BlastFurnaceTests() {}
}
