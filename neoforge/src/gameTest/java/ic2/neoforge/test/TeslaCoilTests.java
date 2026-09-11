package ic2.neoforge.test;


import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.machine.TeslaCoilBlockEntity;
import ic2.neoforge.registration.ModArmor;
import ic2.neoforge.registration.ModMachines;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;


/**
 * Legacy TileEntityTesla: redstone-gated 1 EU/t idle drain, a shock every 32 ticks paying
 * stored/400 damage ×400 EU split across everything alive in four blocks, and complete-hazmat
 * immunity. From a full 10k tank the first shock always pays 24 ×400 (9600) EU, so after 64
 * ticks the ledger reads exactly 336 EU regardless of the phase offset.
 */
final class TeslaCoilTests {
    private static final BlockPos COIL = new BlockPos(2, 1, 2);
    private static final BlockPos POWER = new BlockPos(2, 2, 2);

    private static TeslaCoilBlockEntity coil(GameTestHelper helper) {
        helper.setBlock(COIL, ModMachines.block(MachineKind.TESLA_COIL).defaultBlockState());
        return helper.getBlockEntity(COIL, TeslaCoilBlockEntity.class);
    }

    static void shockAndLedger(GameTestHelper helper) {
        var coil = coil(helper);
        coil.energy().insert(TeslaCoilBlockEntity.CAPACITY);
        helper.setBlock(POWER, Blocks.REDSTONE_BLOCK);
        helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(4, 1, 4));
        helper.startSequence().thenIdle(64).thenExecute(() -> {
            helper.assertEntityNotPresent(EntityType.PIG);
            double stored = coil.energy().stored();
            helper.assertTrue(
                    Math.abs(stored - 336) < 1e-6,
                    "64 ticks must cost the 1 EU idle plus one 24x400 shock (336), got " + stored);
        }).thenSucceed();
    }

    static void redstoneGateAndEmptyTank(GameTestHelper helper) {
        var coil = coil(helper);
        coil.energy().insert(TeslaCoilBlockEntity.CAPACITY);
        var pig = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(4, 1, 4));
        helper.startSequence().thenIdle(64).thenExecute(() -> {
            helper.assertTrue(pig.isAlive(), "Without a signal the coil must never shock");
            double stored = coil.energy().stored();
            helper.assertTrue(
                    Math.abs(stored - TeslaCoilBlockEntity.CAPACITY) < 1e-6,
                    "Without a signal not even the idle drain may run (got " + stored + ")");
            coil.energy().extract(TeslaCoilBlockEntity.CAPACITY - 200);
            helper.setBlock(POWER, Blocks.REDSTONE_BLOCK);
        }).thenIdle(64).thenExecute(() -> {
            double stored = coil.energy().stored();
            helper.assertTrue(
                    Math.abs(stored - 136) < 1e-6,
                    "A sub-400 tank must only pay the idle drain (200-64=136), got " + stored);
            helper.assertTrue(pig.isAlive(), "Zero damage must never shock");
        }).thenSucceed();
    }

    static void hazmatImmunityAndSplit(GameTestHelper helper) {
        var coil = coil(helper);
        coil.energy().insert(TeslaCoilBlockEntity.CAPACITY);
        helper.setBlock(POWER, Blocks.REDSTONE_BLOCK);

        // Three beings split 24 damage into 8 each: the suited one takes nothing, each pig
        // (armor 0, 10 HP) survives at exactly 2. A missed denominator or a shocked suit
        // would leave a different ledger.
        var pig1 = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(4, 1, 2));
        var pig2 = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(4, 1, 4));
        var suited = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(2, 1, 4));
        suited.setItemSlot(EquipmentSlot.HEAD, ModArmor.HAZMAT_HELMET.toStack());
        suited.setItemSlot(EquipmentSlot.CHEST, ModArmor.HAZMAT_CHESTPLATE.toStack());
        suited.setItemSlot(EquipmentSlot.LEGS, ModArmor.HAZMAT_LEGGINGS.toStack());
        suited.setItemSlot(EquipmentSlot.FEET, ModArmor.RUBBER_BOOTS.toStack());

        helper.startSequence().thenIdle(64).thenExecute(() -> {
            helper.assertTrue(
                    suited.isAlive() && suited.getHealth() == suited.getMaxHealth(),
                    "A complete hazmat suit must ignore the shock (health " + suited.getHealth()
                            + ")");
            for (var pig : java.util.List.of(pig1, pig2)) {
                helper.assertTrue(
                        pig.isAlive() && pig.getHealth() == 2.0F,
                        "Three beings split 24 damage into 8 per pig (health " + pig.getHealth()
                                + ")");
            }
        }).thenSucceed();
    }

    private TeslaCoilTests() {}
}
