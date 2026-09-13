package ic2.neoforge.test;

import ic2.neoforge.item.UpgradeItem;
import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.machine.MagnetizerBlockEntity;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.registration.ModMaterialBlocks;
import ic2.neoforge.registration.ModUpgrades;
import ic2.neoforge.world.IronFenceBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.transfer.item.ItemResource;

final class MagnetizerTests {
    private static final BlockPos POSITION = new BlockPos(2, 2, 2);
    private static final BlockPos FENCE = new BlockPos(2, 2, 2);

    static void poweredLift(GameTestHelper helper) {
        var magnetizer = magnetizer(helper);
        helper.setBlock(POSITION, ModMaterialBlocks.IRON_FENCE.get());
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        magnetizer.energy().insert(100);
        var state = helper.getLevel().getBlockState(helper.absolutePos(POSITION));
        helper.assertTrue(
                state.getBlock() instanceof IronFenceBlock, "The test places an iron fence");
        boolean lifted =
                IronFenceBlock.lift(helper.getLevel(), helper.absolutePos(POSITION), player);
        helper.assertTrue(lifted, "A powered magnetizer lifts the climber");
        helper.assertTrue(
                player.getDeltaMovement().y > 0, "The lift adds upward velocity against gravity");
        helper.assertTrue(
                magnetizer.storedEnergy() == 98.0, "One climber share costs two EU per boost");
        helper.succeed();
    }

    static void unpoweredStays(GameTestHelper helper) {
        var magnetizer = magnetizer(helper);
        helper.setBlock(POSITION, ModMaterialBlocks.IRON_FENCE.get());
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        boolean lifted =
                IronFenceBlock.lift(helper.getLevel(), helper.absolutePos(POSITION), player);
        helper.assertTrue(
                !lifted && player.getDeltaMovement().y == 0.0,
                "Without stored EU the fence never lifts anyone");
        magnetizer.energy().insert(1);
        var poor = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.assertTrue(
                !IronFenceBlock.lift(helper.getLevel(), helper.absolutePos(POSITION), poor),
                "One EU cannot cover the two-EU boost share");
        magnetizer.energy().insert(1);
        var climber = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.assertTrue(
                IronFenceBlock.lift(helper.getLevel(), helper.absolutePos(POSITION), climber),
                "Two EU are exactly one boost share");
        helper.succeed();
    }

    static void upgradeStorageResizes(GameTestHelper helper) {
        var magnetizer = magnetizer(helper);
        helper.assertTrue(magnetizer.energyCapacity() == 100.0, "The bare magnetizer stores 100 EU");
        upgrade(magnetizer, 0, UpgradeItem.Kind.ENERGY_STORAGE, 2);
        magnetizer.serverTick(helper.getLevel());
        helper.assertTrue(
                magnetizer.energyCapacity() == 20100.0,
                "Each storage upgrade adds the legacy 10,000 EU to the buffer");
        magnetizer.energy().insert(50000);
        helper.assertTrue(
                magnetizer.storedEnergy() == 20100.0,
                "The widened buffer fills to its new physical capacity");
        magnetizer.inventory().set(1, ItemResource.EMPTY, 0);
        magnetizer.serverTick(helper.getLevel());
        helper.assertTrue(
                magnetizer.energyCapacity() == 100.0 && magnetizer.storedEnergy() == 100.0,
                "Removing the upgrades drops only the energy above the physical capacity");
        helper.succeed();
    }

    static void upgradeTransformersRaiseAmps(GameTestHelper helper) {
        var magnetizer = magnetizer(helper);
        helper.assertTrue(
                magnetizer.energyNode().input().orElseThrow().maxAmps() == 1,
                "The bare magnetizer accepts a single LV packet per tick");
        upgrade(magnetizer, 0, UpgradeItem.Kind.TRANSFORMER, 2);
        upgrade(magnetizer, 1, UpgradeItem.Kind.OVERCLOCKER, 3);
        magnetizer.serverTick(helper.getLevel());
        helper.assertTrue(
                magnetizer.energyNode().input().orElseThrow().maxAmps() == 3,
                "Two transformer upgrades accept two extra LV packets");
        helper.assertTrue(
                magnetizer.energyCapacity() == 100.0,
                "Overclockers stay accepted but inert: legacy distance() has no caller");
        helper.succeed();
    }

    private static MagnetizerBlockEntity magnetizer(GameTestHelper helper) {
        var pos = new BlockPos(1, 2, 2);
        helper.setBlock(pos, ModMachines.block(MachineKind.MAGNETIZER));
        return helper.getBlockEntity(pos, MagnetizerBlockEntity.class);
    }

    private static void upgrade(MagnetizerBlockEntity machine, int slot, UpgradeItem.Kind kind, int count) {
        var stack = ModUpgrades.ALL.get(kind).toStack(count);
        machine.inventory()
                .set(machine.kind().upgradeStart() + slot, ItemResource.of(stack), count);
    }

    private MagnetizerTests() {}
}
