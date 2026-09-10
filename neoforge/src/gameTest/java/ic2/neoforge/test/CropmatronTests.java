package ic2.neoforge.test;

import ic2.neoforge.crop.CropBlockEntity;
import ic2.neoforge.fluid.FluidDefinition;
import ic2.neoforge.item.UpgradeItem;
import ic2.neoforge.machine.CropmatronBlockEntity;
import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.registration.MaterialDefinition;
import ic2.neoforge.registration.ModCells;
import ic2.neoforge.registration.ModCrops;
import ic2.neoforge.registration.ModItems;
import ic2.neoforge.registration.ModFluids;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.registration.ModUpgrades;

import java.util.Objects;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

final class CropmatronTests {
    private static final FluidResource WATER = FluidResource.of(Fluids.WATER);
    private static final FluidResource WEED_EX =
            FluidResource.of(ModFluids.FAMILIES.get(FluidDefinition.WEED_EX).source().get());

    /**
     * The legacy cursor starts before (-4,-1,-4) and the first scan() step lands exactly on the
     * position one step up-left-forward of the machine, so a tile there is served on call one.
     */
    private static final BlockPos MACHINE = new BlockPos(4, 3, 4);
    private static final BlockPos FIRST_SCAN = MACHINE.offset(-3, -1, -4); // helper (1, 2, 0)

    private static CropmatronBlockEntity machine(GameTestHelper helper) {
        helper.setBlock(MACHINE, ModMachines.block(MachineKind.CROPMATRON).defaultBlockState());
        return helper.getBlockEntity(MACHINE, CropmatronBlockEntity.class);
    }

    /** A bare crop stick is a crop tile; the matron serves sticks like any planted crop. */
    private static CropBlockEntity stick(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos.below(), Blocks.FARMLAND);
        helper.setBlock(pos, ModCrops.CROP_STICK.get().defaultBlockState());
        return helper.getBlockEntity(pos, CropBlockEntity.class);
    }

    static void servesCrop(GameTestHelper helper) {
        var machine = machine(helper);
        var crop = stick(helper, FIRST_SCAN);
        machine.energy().insert(400);
        machine.inventory()
                .set(
                        0,
                        ItemResource.of(ModItems.MATERIALS.get(MaterialDefinition.FERTILIZER).get()),
                        4);
        machine.waterTank().set(0, WATER, 2000);
        machine.exTank().set(0, WEED_EX, 1000);
        machine.scan();
        helper.assertTrue(
                crop.getStorageNutrients() == 90
                        && crop.getStorageWater() == 200
                        && crop.getStorageWeedEx() == 150,
                "One scan must top the tile off: machine nutrients 90, water 200, weed-ex 150");
        helper.assertTrue(
                machine.inventory().getAmountAsInt(0) == 3
                        && machine.waterTank().getAmountAsInt(0) == 1800
                        && machine.exTank().getAmountAsInt(0) == 850,
                "The scan must consume one fertilizer dose and drain the tanks by the applied"
                    + " amounts");
        helper.assertTrue(
                machine.energy().stored() == 400 - 31,
                "The scan pays 1 EU for the step plus 10 EU per served resource");
        // The tile is saturated now, so the next step only costs its own 1 EU.
        machine.scan();
        helper.assertTrue(
                machine.energy().stored() == 400 - 32
                        && machine.inventory().getAmountAsInt(0) == 3,
                "A saturated tile only costs the 1 EU step");
        helper.succeed();
    }

    static void hydratesFarmland(GameTestHelper helper) {
        var machine = machine(helper);
        helper.setBlock(FIRST_SCAN, Blocks.FARMLAND.defaultBlockState());
        machine.waterTank().set(0, WATER, 50);
        machine.energy().insert(100);
        machine.scan();
        helper.assertTrue(
                helper.getBlockState(FIRST_SCAN).getValue(FarmlandBlock.MOISTURE) == 7
                        && machine.waterTank().getAmountAsInt(0) == 43,
                "Dry farmland in range must be watered straight from the tank");
        helper.assertTrue(
                machine.energy().stored() == 100 - 11,
                "Farmland hydration pays the step plus 10 EU");
        machine.scan();
        helper.assertTrue(
                machine.waterTank().getAmountAsInt(0) == 43
                        && machine.energy().stored() == 100 - 12,
                "An already moist farmland only costs the step");
        helper.succeed();
    }

    static void containersAndUpgrades(GameTestHelper helper) {
        var machine = machine(helper);
        machine.energy().insert(500);
        machine.inventory()
                .set(CropmatronBlockEntity.WATER_INPUT, ItemResource.of(Items.WATER_BUCKET), 1);
        machine.inventory()
                .set(
                        CropmatronBlockEntity.EX_INPUT,
                        ItemResource.of(
                                Objects.requireNonNull(ModCells.CELLS.get("weed_ex_cell"))
                                        .toStack()),
                        1);
        machine.serverTick(helper.getLevel());
        helper.assertTrue(
                machine.waterTank().getAmountAsInt(0) == 1000
                        && machine.exTank().getAmountAsInt(0) == 1000,
                "Both containers must drain into their tanks");
        helper.assertTrue(
                machine.inventory().stack(CropmatronBlockEntity.WATER_OUTPUT).is(Items.BUCKET)
                        && machine.inventory()
                                .stack(CropmatronBlockEntity.EX_OUTPUT)
                                .is(ModCells.EMPTY.get()),
                "Emptied containers must land in the output slots");
        try (var transaction = Transaction.openRoot()) {
            helper.assertTrue(
                    machine.fluidAutomation(Direction.NORTH)
                                    .insert(0, WEED_EX, 1000, transaction)
                            == 0,
                    "The water side rejects weed-ex");
            helper.assertTrue(
                    machine.fluidAutomation(Direction.NORTH)
                                    .insert(1, WATER, 1000, transaction)
                            == 0,
                    "The weed-ex side rejects water");
        }
        machine.inventory()
                .set(
                        machine.kind().upgradeStart(),
                        ItemResource.of(
                                ModUpgrades.ALL.get(UpgradeItem.Kind.ENERGY_STORAGE).toStack()),
                        2);
        machine.serverTick(helper.getLevel());
        helper.assertTrue(
                machine.energyCapacity() == 30000,
                "Two storage upgrades must raise the buffer to 30000 EU");
        helper.succeed();
    }

    private CropmatronTests() {}
}
