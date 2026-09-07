package ic2.neoforge.test;

import ic2.core.machine.MetalFormerMode;
import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.machine.MetalFormerBlockEntity;
import ic2.neoforge.registration.ModMachines;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.transfer.item.ItemResource;

final class MetalFormerTests {
    static void modes(GameTestHelper helper) {
        for (var mode : MetalFormerMode.values()) {
            var machine = machine(helper);
            machine.setMode(mode);
            machine.inventory()
                    .set(
                            0,
                            mode == MetalFormerMode.CUTTING
                                    ? item("copper_plate")
                                    : ItemResource.of(Items.COPPER_INGOT),
                            1);
            machine.energy().insert(2000);
            for (int tick = 0; tick < 200; tick++) machine.serverTick(helper.getLevel());
            String result = mode == MetalFormerMode.ROLLING ? "copper_plate" : "copper_cable";
            int count =
                    switch (mode) {
                        case EXTRUDING -> 3;
                        case ROLLING -> 1;
                        case CUTTING -> 2;
                    };
            helper.assertTrue(
                    machine.inventory().getResource(1).equals(item(result))
                            && machine.inventory().getAmountAsInt(1) == count,
                    "Mode must execute the correct recipe: " + mode);
            helper.assertTrue(
                    machine.energy().stored() == 0 && machine.inventory().getAmountAsInt(0) == 0,
                    "Each operation must cost exactly 2000 EU and one input");
        }
        helper.succeed();
    }

    static void persistence(GameTestHelper helper) {
        var machine = machine(helper);
        machine.inventory().set(0, ItemResource.of(Items.COPPER_INGOT), 1);
        machine.energy().insert(3000);
        for (int i = 0; i < 20; i++) machine.serverTick(helper.getLevel());
        machine.setMode(MetalFormerMode.ROLLING);
        helper.assertTrue(
                machine.progress() == 0 && machine.energy().stored() == 2800,
                "Changing mode resets work without refunding spent EU");
        for (int i = 0; i < 30; i++) machine.serverTick(helper.getLevel());
        var restored =
                (MetalFormerBlockEntity)
                        BlockEntity.loadStatic(
                                machine.getBlockPos(),
                                machine.getBlockState(),
                                machine.saveWithFullMetadata(helper.getLevel().registryAccess()),
                                helper.getLevel().registryAccess());
        helper.assertTrue(
                restored.mode() == MetalFormerMode.ROLLING && restored.progress() == 30,
                "Reload must preserve mode and in-flight progress");
        for (int i = 0; i < 170; i++) restored.serverTick(helper.getLevel());
        helper.assertTrue(
                restored.inventory().getResource(1).equals(item("copper_plate"))
                        && restored.energy().stored() == 800,
                "Reloaded work must finish once without changing recipe family");
        helper.succeed();
    }

    private static MetalFormerBlockEntity machine(GameTestHelper helper) {
        return new MetalFormerBlockEntity(
                helper.absolutePos(new BlockPos(1, 1, 1)),
                ModMachines.block(MachineKind.METAL_FORMER).defaultBlockState());
    }

    private static ItemResource item(String id) {
        return ItemResource.of(BuiltInRegistries.ITEM.getValue(Identifier.parse("ic2:" + id)));
    }

    private MetalFormerTests() {}
}
