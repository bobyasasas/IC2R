package ic2.neoforge.test;

import ic2.neoforge.machine.CentrifugeBlockEntity;
import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.registration.ModMachines;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.transfer.item.ItemResource;

/** Nuclear refinement chains (P09): heat-gated centrifuge recipes over the new materials. */
final class NuclearTests {
    private static final BlockPos POSITION = new BlockPos(2, 1, 2);

    static void uraniumCentrifuge(GameTestHelper helper) {
        var machine = centrifuge(helper);
        machine.inventory().set(0, pelletless("uranium"), 20);
        machine.energy().insert(60000);
        for (int tick = 0; tick < 4700; tick++) machine.serverTick(helper.getLevel());
        int uranium238 =
                machine.inventory().getAmountAsInt(1) + machine.inventory().getAmountAsInt(3);
        helper.assertTrue(
                machine.inventory().stack(0).isEmpty()
                        && machine.inventory().getResource(1).equals(item("uranium_238"))
                        && machine.inventory().getResource(3).equals(item("uranium_238"))
                        && uranium238 == 112
                        && machine.inventory().getResource(4).equals(item("uranium_235"))
                        && machine.inventory().getAmountAsInt(4) == 7,
                "Twenty uranium split into 112 uranium 238 across slots plus seven uranium 235;"
                        + " slot stacks cap at 64 so the bulk spans two slots");

        helper.succeed();
    }

    static void rtgPelletCentrifuge(GameTestHelper helper) {
        var machine = centrifuge(helper);
        machine.inventory().set(0, pelletless("rtg_pellet"), 1);
        machine.energy().insert(60000);
        for (int tick = 0; tick < 5600; tick++) machine.serverTick(helper.getLevel());
        helper.assertTrue(
                machine.inventory().stack(0).isEmpty(), "The spent RTG pellet is consumed");
        helper.assertTrue(
                machine.inventory().getResource(1).equals(item("plutonium"))
                        && machine.inventory().getAmountAsInt(1) == 3,
                "A spent pellet yields three plutonium");
        helper.assertTrue(
                machine.inventory().getResource(3).equals(item("iron_dust"))
                        && machine.inventory().getAmountAsInt(3) == 54,
                "The remainder is fifty-four iron dust");
        helper.succeed();
    }

    private static CentrifugeBlockEntity centrifuge(GameTestHelper helper) {
        helper.setBlock(POSITION, ModMachines.block(MachineKind.CENTRIFUGE));
        return helper.getBlockEntity(POSITION, CentrifugeBlockEntity.class);
    }

    private static ItemResource pelletless(String id) {
        return ItemResource.of(
                net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(
                        net.minecraft.resources.Identifier.parse("ic2:" + id)));
    }

    private static ItemResource item(String id) {
        return ItemResource.of(
                net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(
                        net.minecraft.resources.Identifier.parse("ic2:" + id)));
    }

    private NuclearTests() {}
}
