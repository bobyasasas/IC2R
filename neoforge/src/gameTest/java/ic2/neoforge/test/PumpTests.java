package ic2.neoforge.test;

import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.machine.PumpBlockEntity;
import ic2.neoforge.registration.ModMachines;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

final class PumpTests {
    private static final BlockPos POSITION = new BlockPos(2, 1, 2);

    static void pumpsFacedWater(GameTestHelper helper) {
        var machine = pump(helper);
        helper.setBlock(POSITION.north(), Blocks.WATER);
        machine.energy().insert(100);
        for (int tick = 0; tick < 21; tick++) machine.serverTick(helper.getLevel());
        helper.assertTrue(
                machine.tank().getAmountAsInt(0) == 1000,
                "One operation pumps one bucket of water");
        helper.assertTrue(machine.progress() == 0, "Progress restarts after an operation");
        helper.succeed();
    }

    static void fillsBuckets(GameTestHelper helper) {
        var machine = pump(helper);
        helper.setBlock(POSITION.north(), Blocks.WATER);
        machine.energy().insert(100);
        try (var transaction = Transaction.openRoot()) {
            machine.inventory().insert(0, ItemResource.of(Items.BUCKET), 1, transaction);
            transaction.commit();
        }
        for (int tick = 0; tick < 41; tick++) machine.serverTick(helper.getLevel());
        var bucket = machine.inventory().stack(1);
        helper.assertTrue(
                !bucket.isEmpty()
                        && bucket.getItem() == Items.WATER_BUCKET
                        && machine.tank().getAmountAsInt(0) == 0,
                "The tank fills a carried bucket through the container port");
        helper.succeed();
    }

    static void survivesReload(GameTestHelper helper) {
        var machine = pump(helper);
        helper.setBlock(POSITION.north(), Blocks.WATER);
        machine.energy().insert(100);
        for (int tick = 0; tick < 10; tick++) machine.serverTick(helper.getLevel());
        var restored =
                (PumpBlockEntity)
                        BlockEntity.loadStatic(
                                machine.getBlockPos(),
                                machine.getBlockState(),
                                machine.saveWithFullMetadata(helper.getLevel().registryAccess()),
                                helper.getLevel().registryAccess());
        helper.getLevel().removeBlockEntity(machine.getBlockPos());
        helper.getLevel().setBlockEntity(restored);
        helper.assertTrue(restored.progress() == 10, "Pump progress is part of the saved state");
        restored.serverTick(helper.getLevel());
        helper.assertTrue(restored.progress() == 11, "The restored pump keeps running");
        helper.succeed();
    }

    private static PumpBlockEntity pump(GameTestHelper helper) {
        var state = ModMachines.block(MachineKind.PUMP).defaultBlockState();
        helper.setBlock(POSITION, state);
        return helper.getBlockEntity(POSITION, PumpBlockEntity.class);
    }
}
