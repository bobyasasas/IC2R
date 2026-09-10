package ic2.neoforge.test;

import ic2.neoforge.component.ModDataComponents;
import ic2.neoforge.crop.CropBlockEntity;
import ic2.neoforge.item.UpgradeItem;
import ic2.neoforge.machine.CropHarvesterBlockEntity;
import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.registration.ModCrops;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.registration.ModUpgrades;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.neoforge.transfer.item.ItemResource;

final class CropHarvesterTests {
    /**
     * The cursor starts before (-4,-1,-4), so the first scan steps land on the row one step
     * up-left-forward of the machine: a ripe crop there is visited on scan call one.
     */
    private static final BlockPos MACHINE = new BlockPos(4, 3, 4);

    private static CropHarvesterBlockEntity machine(GameTestHelper helper) {
        helper.setBlock(MACHINE, ModMachines.block(MachineKind.CROP_HARVESTER).defaultBlockState());
        return helper.getBlockEntity(MACHINE, CropHarvesterBlockEntity.class);
    }

    /** Plants a ripe wheat crop at the given scan-stop position; returns the fresh tile. */
    private static CropBlockEntity ripeWheat(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos.below(), Blocks.FARMLAND);
        helper.setBlock(pos, ModCrops.CROP_STICK.get().defaultBlockState());
        CropBlockEntity stick = helper.getBlockEntity(pos, CropBlockEntity.class);
        helper.assertTrue(
                stick.tryPlantIn(ModCrops.WHEAT_CARD, 0, 2, 2, 2, 4),
                "Wheat plants into the crop stick");
        CropBlockEntity crop = helper.getBlockEntity(pos, CropBlockEntity.class);
        crop.setCurrentAge(crop.card().getMaxAge());
        return crop;
    }

    static void harvestsRipeCrop(GameTestHelper helper) {
        var machine = machine(helper);
        // Six consecutive cursor stops each carry a ripe crop; a gaussian roll of zero drops
        // resets the tile without produce, so six visits make an empty harvest vanishingly
        // unlikely while every visited tile is deterministically harvested and reset.
        var tiles = new CropBlockEntity[6];
        for (int i = 0; i < tiles.length; i++) {
            tiles[i] = ripeWheat(helper, MACHINE.offset(-3 + i, -1, -4));
        }
        machine.energy().insert(200);
        for (int i = 0; i < tiles.length; i++) machine.scan();
        for (int i = 0; i < tiles.length; i++) {
            helper.assertTrue(
                    tiles[i].getCurrentAge() < tiles[i].card().getMaxAge(),
                    "Every visited ripe crop must be harvested and reset");
        }
        int wheat = 0;
        for (int slot = 0; slot < CropHarvesterBlockEntity.CONTENT_END; slot++) {
            wheat += machine.inventory().getResource(slot).is(Items.WHEAT)
                    ? machine.inventory().getAmountAsInt(slot)
                    : 0;
        }
        helper.assertTrue(
                wheat >= 1 && machine.energy().stored() <= 200 - 6 - 20,
                "At least one harvest must feed wheat into the buffer and cost 20 EU"
                    + " (saw " + wheat + " wheat, " + machine.energy().stored() + " EU)");
        helper.succeed();
    }

    static void fullBufferGuards(GameTestHelper helper) {
        var machine = machine(helper);
        var crop = ripeWheat(helper, MACHINE.offset(-3, -1, -4));
        machine.energy().insert(100);
        var dirt = ItemResource.of(Items.DIRT);
        for (int slot = 0; slot < CropHarvesterBlockEntity.CONTENT_END; slot++) {
            machine.inventory().set(slot, dirt, 64);
        }
        machine.scan();
        helper.assertTrue(
                machine.energy().stored() == 99
                        && crop.getCurrentAge() == crop.card().getMaxAge(),
                "A full buffer must skip the harvest entirely and only pay the 1 EU step");
        helper.succeed();
    }

    static void ejectorAndUpgrades(GameTestHelper helper) {
        var machine = machine(helper);
        var chestPos = MACHINE.east();
        helper.setBlock(chestPos, Blocks.CHEST.defaultBlockState());
        var chest = helper.getBlockEntity(chestPos, ChestBlockEntity.class);
        machine.inventory().set(0, ItemResource.of(Items.WHEAT), 3);
        var ejector = ModUpgrades.ALL.get(UpgradeItem.Kind.EJECTOR).toStack();
        ejector.set(ModDataComponents.UPGRADE_DIRECTION, Direction.EAST);
        machine.inventory()
                .set(machine.kind().upgradeStart(), ItemResource.of(ejector), 1);
        machine.serverTick(helper.getLevel());
        helper.assertTrue(
                chest.getItem(0).is(Items.WHEAT) && chest.getItem(0).getCount() == 1
                        && machine.inventory().getAmountAsInt(0) == 2,
                "An east-configured ejector must drain the buffer into the chest");
        machine.inventory()
                .set(
                        machine.kind().upgradeStart() + 1,
                        ItemResource.of(
                                ModUpgrades.ALL.get(UpgradeItem.Kind.ENERGY_STORAGE).toStack()),
                        2);
        machine.serverTick(helper.getLevel());
        helper.assertTrue(
                machine.energyCapacity() == 30000,
                "Two storage upgrades must raise the buffer to 30000 EU");
        helper.assertTrue(
                UpgradeItem.Kind.TRANSFORMER.suitable(MachineKind.CROP_HARVESTER)
                        && UpgradeItem.Kind.ENERGY_STORAGE.suitable(MachineKind.CROP_HARVESTER)
                        && UpgradeItem.Kind.EJECTOR.suitable(MachineKind.CROP_HARVESTER)
                        && !UpgradeItem.Kind.OVERCLOCKER.suitable(MachineKind.CROP_HARVESTER)
                        && !UpgradeItem.Kind.PULLING.suitable(MachineKind.CROP_HARVESTER),
                "Legacy suitability: transformer, storage and ejector only");
        helper.succeed();
    }

    private CropHarvesterTests() {}
}
