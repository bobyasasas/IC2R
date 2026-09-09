package ic2.neoforge.test;

import ic2.neoforge.item.ElectricItemEnergy;
import ic2.neoforge.machine.AdvMinerBlockEntity;
import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.registration.ModTools;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

final class AdvMinerTests {
    private static final BlockPos POSITION = new BlockPos(8, 8, 8);

    private static AdvMinerBlockEntity miner(GameTestHelper helper) {
        helper.setBlock(POSITION, ModMachines.block(MachineKind.ADV_MINER).defaultBlockState());
        return helper.getBlockEntity(POSITION, AdvMinerBlockEntity.class);
    }

    private static void run(AdvMinerBlockEntity miner, int ticks) {
        var level = (ServerLevel) miner.getLevel();
        for (int tick = 0; tick < ticks; tick++) miner.serverTick(level);
    }

    static void sweepsAndMines(GameTestHelper helper) {
        var level = helper.getLevel();
        level.setBlockAndUpdate(
                helper.absolutePos(new BlockPos(9, 7, 9)), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(
                helper.absolutePos(new BlockPos(10, 7, 9)), Blocks.COAL_ORE.defaultBlockState());
        level.setBlockAndUpdate(
                helper.absolutePos(new BlockPos(11, 7, 9)), Blocks.IRON_ORE.defaultBlockState());
        var miner = miner(helper);
        miner.energy().restore(100000);
        var scanner = ModTools.SCANNER.toStack();
        ElectricItemEnergy.charge(scanner, 100000, 1, true, false);
        try (var transaction = Transaction.openRoot()) {
            int inserted =
                    miner.inventory()
                            .insert(
                                    AdvMinerBlockEntity.SCANNER_SLOT,
                                    ItemResource.of(scanner),
                                    1,
                                    transaction);
            helper.assertTrue(inserted == 1, "The scanner sits in its slot");
            transaction.commit();
        }
        for (int tick = 0; tick < 25; tick++) miner.serverTick((ServerLevel) miner.getLevel());
        helper.assertTrue(
                ElectricItemEnergy.charge(miner.inventory().stack(AdvMinerBlockEntity.SCANNER_SLOT))
                        >= 64,
                "The scanner holds charge for the sweep");
        helper.assertTrue(
                miner.mineTarget() != null, "The sweep cursor is established within a batch");
        run(miner, 9000);
        helper.assertTrue(
                level.getBlockState(helper.absolutePos(new BlockPos(9, 7, 9))).isAir()
                        && level.getBlockState(helper.absolutePos(new BlockPos(10, 7, 9))).isAir()
                        && level.getBlockState(helper.absolutePos(new BlockPos(11, 7, 9))).isAir(),
                "The sweep mines every in-list block it passes");
        boolean coalDropped =
                helper.getEntities(EntityType.ITEM).stream()
                        .anyMatch(entity -> entity.getItem().is(Items.COAL));
        helper.assertTrue(coalDropped, "Mined drops appear above the machine");
        helper.assertTrue(miner.mineTarget() != null, "The sweep cursor survived the batch");
        helper.succeed();
    }

    static void whitelistGates(GameTestHelper helper) {
        var level = helper.getLevel();
        level.setBlockAndUpdate(
                helper.absolutePos(new BlockPos(9, 7, 9)), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(
                helper.absolutePos(new BlockPos(10, 7, 9)), Blocks.COAL_ORE.defaultBlockState());
        var miner = miner(helper);
        miner.energy().restore(100000);
        helper.assertTrue(miner.menuAction(1), "The menu switches the filter mode");
        helper.assertTrue(!miner.blacklist(), "The machine runs in whitelist mode");
        var chargedScanner = ModTools.SCANNER.toStack();
        ElectricItemEnergy.charge(chargedScanner, 100000, 1, true, false);
        try (var transaction = Transaction.openRoot()) {
            miner.inventory()
                    .insert(
                            AdvMinerBlockEntity.FILTER_START,
                            ItemResource.of(new ItemStack(Items.COAL)),
                            1,
                            transaction);
            miner.inventory()
                    .insert(
                            AdvMinerBlockEntity.SCANNER_SLOT,
                            ItemResource.of(chargedScanner),
                            1,
                            transaction);
            transaction.commit();
        }
        run(miner, 9000);
        helper.assertTrue(
                level.getBlockState(helper.absolutePos(new BlockPos(10, 7, 9))).isAir(),
                "The listed coal ore is mined");
        helper.assertTrue(
                !level.getBlockState(helper.absolutePos(new BlockPos(9, 7, 9))).isAir(),
                "Unlisted stone survives the whitelist");
        helper.succeed();
    }

    static void silkAndReset(GameTestHelper helper) {
        var level = helper.getLevel();
        level.setBlockAndUpdate(
                helper.absolutePos(new BlockPos(9, 7, 9)), Blocks.DIAMOND_ORE.defaultBlockState());
        var miner = miner(helper);
        miner.energy().restore(100000);
        helper.assertTrue(miner.menuAction(2), "The menu toggles silk touch");
        helper.assertTrue(miner.silkTouch(), "Silk touch is on");
        var chargedScanner = ModTools.SCANNER.toStack();
        ElectricItemEnergy.charge(chargedScanner, 100000, 1, true, false);
        try (var transaction = Transaction.openRoot()) {
            miner.inventory()
                    .insert(
                            AdvMinerBlockEntity.SCANNER_SLOT,
                            ItemResource.of(chargedScanner),
                            1,
                            transaction);
            transaction.commit();
        }
        run(miner, 9000);
        helper.assertTrue(
                level.getBlockState(helper.absolutePos(new BlockPos(9, 7, 9))).isAir(),
                "The silk touch sweep mined the ore");
        boolean selfDropped =
                helper.getEntities(EntityType.ITEM).stream()
                        .anyMatch(entity -> entity.getItem().is(Blocks.DIAMOND_ORE.asItem()));
        helper.assertTrue(selfDropped, "Silk touch drops the ore block itself");
        helper.assertTrue(
                miner.menuAction(0) && miner.mineTarget() == null,
                "The reset button clears the sweep cursor");
        helper.succeed();
    }

    private AdvMinerTests() {}
}
