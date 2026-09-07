package ic2.neoforge.test;

import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.machine.RecyclerBlockEntity;
import ic2.neoforge.registration.MaterialDefinition;
import ic2.neoforge.registration.ModItems;
import ic2.neoforge.registration.ModMachines;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.transfer.item.ItemResource;

final class RecyclerTests {
    static void blacklist(GameTestHelper helper) {
        for (var item :
                new net.minecraft.world.item.Item[] {
                    Items.STICK, Items.GLASS_PANE, Items.SNOWBALL, Items.SNOW
                }) {
            var machine = machine(helper);
            machine.inventory().set(0, ItemResource.of(item), 1);
            machine.inventory().set(1, ItemResource.of(Items.DIRT), 64);
            machine.energy().insert(45);
            for (int tick = 0; tick < 45; tick++) machine.serverTick(helper.getLevel());
            helper.assertTrue(
                    machine.inventory().stack(0).isEmpty()
                            && machine.energy().stored() == 0
                            && machine.inventory().stack(1).is(Items.DIRT),
                    "Blacklisted items must be consumed without scrap even when output is blocked: "
                            + item);
        }
        helper.succeed();
    }

    static void deterministicChance(GameTestHelper helper) {
        var machine = machine(helper);
        machine.inventory().set(0, ItemResource.of(Items.COBBLESTONE), 64);
        long seed = 192837L;
        var expected = RandomSource.create(seed);
        int scrap = 0;
        for (int i = 0; i < 64; i++) if (expected.nextInt(8) == 0) scrap++;
        // This synchronous test completes before another world tick can consume this RNG.
        helper.getLevel().getRandom().setSeed(seed);
        for (int tick = 0; tick < 64 * 45; tick++) {
            machine.energy().insert(1);
            machine.serverTick(helper.getLevel());
        }
        helper.assertTrue(
                scrap > 0
                        && scrap < 64
                        && machine.inventory().getAmountAsInt(1) == scrap
                        && machine.inventory()
                                .stack(1)
                                .is(ModItems.MATERIALS.get(MaterialDefinition.SCRAP).get()),
                "Each completed operation must perform exactly one native nextInt(8) roll");
        helper.assertTrue(
                machine.inventory().stack(0).isEmpty() && machine.energy().stored() == 0,
                "Every input must cost exactly 45 EU regardless of random outcome");
        helper.succeed();
    }

    static void persistenceAndBlocking(GameTestHelper helper) {
        var machine = machine(helper);
        machine.inventory().set(0, ItemResource.of(Items.COBBLESTONE), 1);
        machine.energy().insert(45);
        for (int tick = 0; tick < 44; tick++) machine.serverTick(helper.getLevel());
        var restored =
                (RecyclerBlockEntity)
                        BlockEntity.loadStatic(
                                machine.getBlockPos(),
                                machine.getBlockState(),
                                machine.saveWithFullMetadata(helper.getLevel().registryAccess()),
                                helper.getLevel().registryAccess());
        helper.assertTrue(
                restored.progress() == 44,
                "Reload must preserve the input identity and final-tick progress");
        long seed = 0;
        while (true) {
            var candidate = RandomSource.create(seed);
            if (candidate.nextInt(8) == 0 && candidate.nextInt(8) != 0) break;
            seed++;
        }
        helper.getLevel().getRandom().setSeed(seed);
        restored.inventory().set(1, ItemResource.of(Items.DIRT), 64);
        restored.serverTick(helper.getLevel());
        helper.assertTrue(
                restored.progress() == 44
                        && restored.energy().stored() == 1
                        && restored.inventory().getAmountAsInt(0) == 1,
                "Blocked potential output must pause before consuming or drawing a roll");
        restored.inventory().set(1, ItemResource.EMPTY, 0);
        restored.serverTick(helper.getLevel());
        helper.assertTrue(
                restored.inventory()
                                .stack(1)
                                .is(ModItems.MATERIALS.get(MaterialDefinition.SCRAP).get())
                        && restored.inventory().stack(0).isEmpty()
                        && restored.energy().stored() == 0,
                "Unblocked completion must use the first random roll exactly once");
        helper.succeed();
    }

    static void inputComponents(GameTestHelper helper) {
        var machine = machine(helper);
        machine.inventory().set(0, ItemResource.of(Items.COBBLESTONE), 1);
        machine.energy().insert(90);
        for (int tick = 0; tick < 10; tick++) machine.serverTick(helper.getLevel());
        var renamed = Items.COBBLESTONE.getDefaultInstance();
        renamed.set(DataComponents.CUSTOM_NAME, Component.literal("Different input"));
        machine.inventory().set(0, ItemResource.of(renamed), 1);
        machine.serverTick(helper.getLevel());
        helper.assertTrue(
                machine.progress() == 1 && machine.energy().stored() == 79,
                "Changing input components resets work without refunding previous EU");
        helper.succeed();
    }

    private static RecyclerBlockEntity machine(GameTestHelper helper) {
        return new RecyclerBlockEntity(
                helper.absolutePos(new BlockPos(1, 1, 1)),
                ModMachines.block(MachineKind.RECYCLER).defaultBlockState());
    }

    private RecyclerTests() {}
}
