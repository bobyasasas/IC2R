package ic2.neoforge.test;

import ic2.neoforge.item.ElectricItemEnergy;
import ic2.neoforge.machine.MachineBlock;
import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.machine.MinerBlockEntity;
import ic2.neoforge.machine.PumpBlockEntity;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.registration.ModMaterialBlocks;
import ic2.neoforge.registration.ModTools;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

final class MinerTests {
    private static final BlockPos POSITION = new BlockPos(8, 7, 8);

    private static MinerBlockEntity miner(GameTestHelper helper) {
        helper.setBlock(POSITION, ModMachines.block(MachineKind.MINER).defaultBlockState());
        return helper.getBlockEntity(POSITION, MinerBlockEntity.class);
    }

    private static void feed(MinerBlockEntity miner, int pipes, ItemStack drill) {
        try (var transaction = Transaction.openRoot()) {
            if (drill != null && !drill.isEmpty()) {
                miner.inventory()
                        .insert(
                                MinerBlockEntity.DRILL_SLOT,
                                ItemResource.of(drill),
                                drill.getCount(),
                                transaction);
            }
            miner.inventory()
                    .insert(
                            MinerBlockEntity.PIPE_SLOT,
                            ItemResource.of(
                                    new ItemStack(ModMaterialBlocks.MINING_PIPE.get().asItem())),
                            pipes,
                            transaction);
            transaction.commit();
        }
    }

    private static void insert(MinerBlockEntity miner, int slot, ItemStack stack) {
        try (var transaction = Transaction.openRoot()) {
            miner.inventory().insert(slot, ItemResource.of(stack), stack.getCount(), transaction);
            transaction.commit();
        }
    }

    private static void run(MinerBlockEntity miner, int ticks) {
        var level = (ServerLevel) miner.getLevel();
        for (int tick = 0; tick < ticks; tick++) {
            miner.energy().insert(1000);
            miner.serverTick(level);
        }
    }

    private static ItemStack inDrill(MinerBlockEntity miner) {
        return miner.inventory().stack(MinerBlockEntity.DRILL_SLOT);
    }

    static void digsDownAndHarvests(GameTestHelper helper) {
        var level = helper.getLevel();
        for (int y = 4; y <= 6; y++) {
            level.setBlockAndUpdate(
                    helper.absolutePos(new BlockPos(8, y, 8)),
                    (y == 4 ? Blocks.IRON_ORE : Blocks.STONE).defaultBlockState());
        }
        var miner = miner(helper);
        feed(miner, 3, ModTools.DIAMOND_DRILL.toStack());
        ElectricItemEnergy.charge(inDrill(miner), 30000, 1, true, false);
        run(miner, 1500);
        helper.assertTrue(
                helper.getBlockState(new BlockPos(8, 6, 8)).getBlock()
                        == ModMaterialBlocks.MINING_PIPE.get(),
                "The first dug level keeps a pipe");
        helper.assertTrue(
                helper.getBlockState(new BlockPos(8, 5, 8)).getBlock()
                        == ModMaterialBlocks.MINING_PIPE.get(),
                "Passed levels convert their tip into a pipe");
        helper.assertTrue(
                helper.getBlockState(new BlockPos(8, 4, 8)).getBlock()
                        == ModMaterialBlocks.MINING_PIPE_TIP.get(),
                "The deepest level ends in a fresh tip");
        int ores = 0;
        for (int slot = MinerBlockEntity.BUFFER_START;
                slot < MinerBlockEntity.BUFFER_START + MinerBlockEntity.BUFFER_SIZE;
                slot++) {
            if (miner.inventory().stack(slot).is(Items.RAW_IRON)) ores++;
        }
        helper.assertTrue(ores == 1, "Harvested ore lands in the buffer with the drill's drops");
        helper.assertTrue(
                ElectricItemEnergy.charge(inDrill(miner)) == 30000,
                "The miner recharges its drill from the buffer like the legacy chargeTools");
        helper.succeed();
    }

    static void scannerDigsTowardsOre(GameTestHelper helper) {
        var level = helper.getLevel();
        for (var pos : BlockPos.betweenClosed(new BlockPos(5, 4, 5), new BlockPos(11, 6, 11))) {
            level.setBlockAndUpdate(helper.absolutePos(pos), Blocks.STONE.defaultBlockState());
        }
        level.setBlockAndUpdate(
                helper.absolutePos(new BlockPos(11, 6, 8)), Blocks.COAL_ORE.defaultBlockState());
        var miner = miner(helper);
        feed(miner, 3, ModTools.DIAMOND_DRILL.toStack());
        var scanner = ModTools.SCANNER.toStack();
        ElectricItemEnergy.charge(scanner, 100000, 1, true, false);
        insert(miner, MinerBlockEntity.SCANNER_SLOT, scanner);
        run(miner, 4000);
        helper.assertTrue(
                helper.getBlockState(new BlockPos(11, 6, 8)).isAir(),
                "The layer scan tunnels sideways to the ore target");
        helper.assertTrue(
                ElectricItemEnergy.charge(miner.inventory().stack(MinerBlockEntity.SCANNER_SLOT))
                        == 100000,
                "The miner recharges its scanner from the buffer like the legacy chargeTools");
        boolean coalFound = false;
        for (int slot = MinerBlockEntity.BUFFER_START;
                slot < MinerBlockEntity.BUFFER_START + MinerBlockEntity.BUFFER_SIZE;
                slot++) {
            if (miner.inventory().stack(slot).is(Items.COAL)) coalFound = true;
        }
        helper.assertTrue(coalFound, "The sideways ore reaches the buffer");
        helper.succeed();
    }

    static void withdrawsColumnWithoutDrill(GameTestHelper helper) {
        var level = helper.getLevel();
        for (int y = 5; y <= 6; y++) {
            level.setBlockAndUpdate(
                    helper.absolutePos(new BlockPos(8, y, 8)), Blocks.STONE.defaultBlockState());
        }
        var miner = miner(helper);
        feed(miner, 2, ModTools.DIAMOND_DRILL.toStack());
        run(miner, 800);
        helper.assertTrue(
                helper.getBlockState(new BlockPos(8, 5, 8)).getBlock()
                        == ModMaterialBlocks.MINING_PIPE_TIP.get(),
                "The miner dug down before the drill is removed");
        try (var transaction = Transaction.openRoot()) {
            miner.inventory()
                    .extract(
                            MinerBlockEntity.DRILL_SLOT,
                            miner.inventory().getResource(MinerBlockEntity.DRILL_SLOT),
                            1,
                            transaction);
            transaction.commit();
        }
        run(miner, 4000);
        helper.assertTrue(
                helper.getBlockState(new BlockPos(8, 6, 8)).isAir()
                        && helper.getBlockState(new BlockPos(8, 5, 8)).isAir(),
                "Without a drill the whole pipe column comes back up");
        int bufferPipes = 0;
        for (int slot = MinerBlockEntity.BUFFER_START;
                slot < MinerBlockEntity.BUFFER_START + MinerBlockEntity.BUFFER_SIZE;
                slot++) {
            var stack = miner.inventory().stack(slot);
            if (stack.is(ModMaterialBlocks.MINING_PIPE.get().asItem()))
                bufferPipes += stack.getCount();
        }
        helper.assertTrue(
                bufferPipes == 2, "Withdrawn pipes return to the buffer, none stay in the world");
        helper.succeed();
    }

    static void pumpModeDrainsMarkedLiquid(GameTestHelper helper) {
        var level = helper.getLevel();
        for (int y = 5; y <= 6; y++) {
            level.setBlockAndUpdate(
                    helper.absolutePos(new BlockPos(8, y, 8)), Blocks.STONE.defaultBlockState());
        }
        level.setBlockAndUpdate(
                helper.absolutePos(new BlockPos(8, 4, 8)), Blocks.WATER.defaultBlockState());
        var miner = miner(helper);
        var pumpPos = new BlockPos(7, 7, 8);
        helper.setBlock(
                pumpPos,
                ModMachines.block(MachineKind.PUMP)
                        .defaultBlockState()
                        .setValue(MachineBlock.FACING, Direction.EAST));
        var pump = helper.getBlockEntity(pumpPos, PumpBlockEntity.class);
        pump.energy().insert(100);
        feed(miner, 4, ModTools.DIAMOND_DRILL.toStack());
        helper.assertTrue(miner.menuAction(0) && miner.pumpMode(), "The menu toggles pump mode");
        var serverLevel = (ServerLevel) level;
        for (int tick = 0; tick < 4000; tick++) {
            miner.energy().insert(1000);
            pump.energy().insert(20);
            miner.serverTick(serverLevel);
            pump.serverTick(serverLevel);
        }
        helper.assertTrue(
                helper.getBlockState(new BlockPos(8, 5, 8)).getBlock()
                        == ModMaterialBlocks.MINING_PIPE.get(),
                "The miner keeps digging through the shaft once the water is gone");
        helper.assertTrue(
                pump.tank().getAmountAsInt(0) >= 1000,
                "The marked shaft water is drained by the adjacent pump");
        helper.succeed();
    }

    private MinerTests() {}
}
