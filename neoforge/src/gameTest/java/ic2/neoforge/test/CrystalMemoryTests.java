package ic2.neoforge.test;

import ic2.neoforge.component.ModDataComponents;
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

    static void valueSnapshotRoundTrips(GameTestHelper helper) {
        var memory = ModReactorItems.CRYSTAL_MEMORY.get().getDefaultInstance();
        var item = (CrystalMemoryItem) ModReactorItems.CRYSTAL_MEMORY.get();
        helper.assertTrue(
                item.readValue(memory) == null, "A fresh memory carries no UU value snapshot");
        item.writePattern(memory, new ItemStack(Items.IRON_INGOT));
        item.writeValue(memory, 13.429430);
        Double value = item.readValue(memory);
        helper.assertTrue(
                value != null && Math.abs(value - 13.429430) < 1.0E-9,
                "The recorded bucket value reads back, saw " + value);
        helper.assertTrue(
                memory.get(ModDataComponents.CRYSTAL_MEMORY_VALUE) != null,
                "The snapshot lives on the CRYSTAL_MEMORY_VALUE component");
        item.writePattern(memory, ItemStack.EMPTY);
        helper.assertTrue(
                item.readValue(memory) == null, "Blanking the memory drops the value snapshot");
        helper.succeed();
    }

    private CrystalMemoryTests() {}
}
