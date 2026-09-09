package ic2.neoforge.test;

import ic2.neoforge.component.ModDataComponents;
import ic2.neoforge.item.ReactorComponent;
import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.machine.NuclearReactorBlockEntity;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.registration.ModReactorItems;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.transfer.item.ItemResource;

final class ReactorComponentTests {
    private static final BlockPos POSITION = new BlockPos(8, 8, 8);

    private static NuclearReactorBlockEntity reactor(GameTestHelper helper) {
        helper.setBlock(
                POSITION, ModMachines.block(MachineKind.NUCLEAR_REACTOR).defaultBlockState());
        return helper.getBlockEntity(POSITION, NuclearReactorBlockEntity.class);
    }

    private static void cycles(NuclearReactorBlockEntity reactor, GameTestHelper helper, int n) {
        for (int i = 0; i < n * NuclearReactorBlockEntity.CYCLE_TICKS; i++) {
            helper.setBlock(POSITION.west(), Blocks.REDSTONE_BLOCK);
            reactor.serverTick(helper.getLevel());
        }
    }

    static void reflectorBouncesPulse(GameTestHelper helper) {
        var reactor = reactor(helper);
        reactor.inventory()
                .set(
                        9,
                        ItemResource.of(
                                ModReactorItems.URANIUM_FUEL_ROD.get().getDefaultInstance()),
                        1);
        reactor.inventory()
                .set(
                        10,
                        ItemResource.of(
                                ModReactorItems.NEUTRON_REFLECTOR.get().getDefaultInstance()),
                        1);
        cycles(reactor, helper, 1);
        helper.assertTrue(
                reactor.getReactorEnergyOutput() == 2,
                "The reflector bounces the pulse back for a second output unit");
        helper.assertTrue(reactor.getHeat() >= 8, "Two pulses heat the core by at least 8, saw " + reactor.getHeat());
        var reflector = reactor.inventory().stack(10);
        helper.assertTrue(
                reflector.getOrDefault(ModDataComponents.REACTOR_USE, -1) == 1,
                "The thin reflector depletes per reflected pulse");
        helper.succeed();
    }

    static void platingRaisesCoreLimits(GameTestHelper helper) {
        var reactor = reactor(helper);
        reactor.inventory()
                .set(
                        9,
                        ItemResource.of(ModReactorItems.REACTOR_PLATING.get().getDefaultInstance()),
                        1);
        cycles(reactor, helper, 1);
        helper.assertTrue(
                reactor.getMaxHeat() == 11000, "The plating raises the core limit to 11000");
        helper.assertTrue(
                Math.abs(reactor.getHeatEffectModifier() - 0.95F) < 1e-6,
                "The plating softens the heat effect rolls");
        helper.succeed();
    }

    static void heatSwitchBalancesCoreHeat(GameTestHelper helper) {
        var reactor = reactor(helper);
        reactor.setHeat(5000);
        reactor.inventory()
                .set(
                        9,
                        ItemResource.of(
                                ModReactorItems.REACTOR_HEAT_EXCHANGER.get().getDefaultInstance()),
                        1);
        cycles(reactor, helper, 1);
        var exchanger =
                ModReactorItems.REACTOR_HEAT_EXCHANGER
                        .get()
                        .currentHeat(reactor.inventory().stack(9));
        helper.assertTrue(exchanger > 0, "The exchanger pulled heat out of the core");
        helper.assertTrue(reactor.getHeat() < 5000, "The core cools once the exchanger engages");
        helper.succeed();
    }

    static void ventSpreadCoolsNeighbours(GameTestHelper helper) {
        var reactor = reactor(helper);
        var coolant = ModReactorItems.REACTOR_COOLANT_CELL.get().getDefaultInstance();
        coolant.set(ModDataComponents.REACTOR_HEAT, 100);
        reactor.inventory().set(9, ItemResource.of(coolant), 1);
        reactor.inventory()
                .set(
                        10,
                        ItemResource.of(
                                ModReactorItems.COMPONENT_HEAT_VENT.get().getDefaultInstance()),
                        1);
        cycles(reactor, helper, 1);
        var component = (ReactorComponent) reactor.inventory().stack(9).getItem();
        int left = component.getCurrentHeat(reactor.inventory().stack(9), reactor, 9, 0);
        helper.assertTrue(left == 96, "The spread vent cools its neighbour by four, saw " + left);
        helper.succeed();
    }

    private ReactorComponentTests() {}
}
