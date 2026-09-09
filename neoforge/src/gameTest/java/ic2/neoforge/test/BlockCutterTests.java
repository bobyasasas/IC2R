package ic2.neoforge.test;

import ic2.neoforge.machine.BlockCutterBlockEntity;
import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.registration.MaterialDefinition;
import ic2.neoforge.registration.ModItems;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.registration.ModTools;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.transfer.item.ItemResource;

final class BlockCutterTests {
    private static final BlockPos POSITION = new BlockPos(1, 1, 1);

    private static BlockCutterBlockEntity cutter(GameTestHelper helper) {
        return new BlockCutterBlockEntity(
                helper.absolutePos(POSITION),
                ModMachines.block(MachineKind.BLOCK_CUTTER).defaultBlockState());
    }

    private static void insert(BlockCutterBlockEntity cutter, int slot, ItemStack stack) {
        cutter.inventory().set(slot, ItemResource.of(stack), stack.getCount());
    }

    private static void run(BlockCutterBlockEntity cutter, ServerLevel level, int ticks) {
        for (int tick = 0; tick < ticks; tick++) cutter.serverTick(level);
    }

    static void cutsBlockIntoPlates(GameTestHelper helper) {
        var cutter = cutter(helper);
        cutter.energy().insert(1800);
        insert(cutter, BlockCutterBlockEntity.BLADE, ModTools.IRON_CUTTING_BLADE.toStack());
        insert(cutter, 0, new ItemStack(Items.COPPER_BLOCK));
        var level = helper.getLevel();
        run(cutter, level, 450);
        var plate = ModItems.MATERIALS.get(MaterialDefinition.COPPER_PLATE).get();
        helper.assertTrue(
                cutter.inventory().stack(0).isEmpty(), "The iron blade cuts the copper block");
        helper.assertTrue(
                cutter.inventory().stack(1).is(plate)
                        && cutter.inventory().stack(1).getCount() == 9,
                "Cutting a storage block yields nine plates");
        helper.assertTrue(cutter.energy().stored() == 0, "One cut consumes 4 EU/t over 450 ticks");
        helper.assertTrue(!cutter.bladeTooWeak(), "A sufficient blade never stalls the machine");
        helper.succeed();
    }

    static void weakBladeStallsAndDiamondResumes(GameTestHelper helper) {
        var cutter = cutter(helper);
        cutter.energy().insert(1800);
        insert(cutter, BlockCutterBlockEntity.BLADE, ModTools.IRON_CUTTING_BLADE.toStack());
        insert(cutter, 0, new ItemStack(Items.OBSIDIAN));
        var level = helper.getLevel();
        run(cutter, level, 900);
        helper.assertTrue(
                !cutter.inventory().stack(0).isEmpty() && cutter.inventory().stack(1).isEmpty(),
                "Obsidian resists the iron blade completely");
        helper.assertTrue(
                cutter.bladeTooWeak() && cutter.menuValue(0) == 1,
                "The machine reports the blade as too weak");
        helper.assertTrue(
                cutter.energy().stored() == 1800, "A stalled cutter must not drain energy");
        insert(cutter, BlockCutterBlockEntity.BLADE, ModTools.DIAMOND_CUTTING_BLADE.toStack());
        run(cutter, level, 450);
        helper.assertTrue(
                cutter.inventory().stack(0).isEmpty() && !cutter.inventory().stack(1).isEmpty(),
                "The diamond blade resumes the cut on obsidian");
        helper.assertTrue(cutter.energy().stored() == 0, "The resumed cut costs the full 1800 EU");
        helper.succeed();
    }

    static void missingBladeStalls(GameTestHelper helper) {
        var cutter = cutter(helper);
        cutter.energy().insert(1000);
        insert(cutter, 0, new ItemStack(Items.OAK_PLANKS, 4));
        cutter.serverTick(helper.getLevel());
        helper.assertTrue(
                cutter.bladeTooWeak(),
                "A missing blade stalls the machine before any energy is spent");
        helper.assertTrue(cutter.energy().stored() == 1000, "No EU is used without a blade");
        helper.succeed();
    }

    private BlockCutterTests() {}
}
