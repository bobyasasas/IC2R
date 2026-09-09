package ic2.neoforge.test;

import ic2.neoforge.item.CrystalMemoryItem;
import ic2.neoforge.registration.ModReactorItems;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

final class CrystalMemoryTests {
    static void recordsAndReadsPattern(GameTestHelper helper) {
        var memory = ModReactorItems.CRYSTAL_MEMORY.get().getDefaultInstance();
        var item = (CrystalMemoryItem) ModReactorItems.CRYSTAL_MEMORY.get();
        helper.assertTrue(item.readPattern(memory).isEmpty(), "A fresh memory is blank");
        item.writePattern(memory, new ItemStack(Items.IRON_INGOT));
        var read = item.readPattern(memory);
        helper.assertTrue(read.is(Items.IRON_INGOT), "The recorded pattern reads back");
        item.writePattern(memory, ItemStack.EMPTY);
        helper.assertTrue(
                item.readPattern(memory).isEmpty(), "Writing an empty stack blanks the memory");
        helper.succeed();
    }

    private CrystalMemoryTests() {}
}
