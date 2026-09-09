package ic2.neoforge.test;

import ic2.neoforge.item.FuelRodItem;
import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.machine.NuclearReactorBlockEntity;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.registration.ModReactorItems;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.transfer.item.ItemResource;

final class NuclearReactorTests {
    private static final BlockPos POSITION = new BlockPos(8, 8, 8);

    private static NuclearReactorBlockEntity reactor(GameTestHelper helper) {
        helper.setBlock(
                POSITION, ModMachines.block(MachineKind.NUCLEAR_REACTOR).defaultBlockState());
        return helper.getBlockEntity(POSITION, NuclearReactorBlockEntity.class);
    }

    private static void redstoneOn(GameTestHelper helper) {
        helper.setBlock(POSITION.west(), Blocks.REDSTONE_BLOCK);
    }

    private static void cycles(NuclearReactorBlockEntity reactor, GameTestHelper helper, int n) {
        for (int i = 0; i < n * NuclearReactorBlockEntity.CYCLE_TICKS; i++) {
            redstoneOn(helper);
            reactor.serverTick(helper.getLevel());
        }
    }

    static void uraniumRodPulsesAndDepletes(GameTestHelper helper) {
        var reactor = reactor(helper);
        var rod = ModReactorItems.URANIUM_FUEL_ROD.get().getDefaultInstance();
        reactor.inventory().set(4, ItemResource.of(rod), 1);
        cycles(reactor, helper, 3);
        var slot = reactor.inventory().stack(4);
        helper.assertTrue(
                slot.getItem() instanceof FuelRodItem,
                "A fresh rod keeps running for 20000 cycles before depleting");
        var use = slot.getOrDefault(ic2.neoforge.component.ModDataComponents.REACTOR_USE, -1);
        helper.assertTrue(use == 3, "Three cycles deplete the rod by three, saw " + use);
        int pulses = 1 + 1 / 2;
        helper.assertTrue(
                reactor.getReactorEnergyOutput() == pulses,
                "A single uranium rod pulses once per cycle");
        helper.assertTrue(
                reactor.getHeat() == 12, "Three uncooled cycles accumulate twelve heat");
        helper.succeed();
    }

    static void adjacentRodMultipliesHeat(GameTestHelper helper) {
        var reactor = reactor(helper);
        reactor.inventory()
                .set(
                        4,
                        ItemResource.of(
                                ModReactorItems.URANIUM_FUEL_ROD.get().getDefaultInstance()),
                        1);
        reactor.inventory()
                .set(
                        5,
                        ItemResource.of(
                                ModReactorItems.URANIUM_FUEL_ROD.get().getDefaultInstance()),
                        1);
        cycles(reactor, helper, 2);
        helper.assertTrue(
                reactor.getReactorEnergyOutput() > 2,
                "Adjacent rods pulse each other for extra output");
        helper.assertTrue(
                reactor.getHeat() > 8, "Neighbouring pulses multiply the triangular heat formula");
        helper.succeed();
    }

    static void ventAbsorbsRodHeat(GameTestHelper helper) {
        var reactor = reactor(helper);
        reactor.inventory()
                .set(
                        4,
                        ItemResource.of(
                                ModReactorItems.URANIUM_FUEL_ROD.get().getDefaultInstance()),
                        1);
        reactor.inventory()
                .set(5, ItemResource.of(ModReactorItems.HEAT_VENT.get().getDefaultInstance()), 1);
        cycles(reactor, helper, 3);
        var vent = ModReactorItems.HEAT_VENT.get().heat(reactor.inventory().stack(5));
        helper.assertTrue(vent.stored() > 0, "The vent absorbed the rod's heat");
        helper.assertTrue(vent.stored() <= vent.capacity(), "The vent never exceeds capacity");
        helper.succeed();
    }

    static void meltDownExplodesCore(GameTestHelper helper) {
        var reactor = reactor(helper);
        for (int slot = 4; slot <= 8; slot++)
            reactor.inventory()
                    .set(
                            slot,
                            ItemResource.of(
                                    ModReactorItems.QUAD_URANIUM_FUEL_ROD
                                            .get()
                                            .getDefaultInstance()),
                            1);
        cycles(reactor, helper, 12);
        helper.assertTrue(
                !helper.getBlockState(POSITION)
                        .is(ModMachines.block(MachineKind.NUCLEAR_REACTOR)),
                "A full melt-down removes the reactor core");
        helper.assertTrue(reactor.getHeat() == 0, "The melt-down resets the heat accounting");
        boolean debris = helper.getEntities(net.minecraft.world.entity.EntityType.ITEM).isEmpty();
        helper.assertTrue(debris, "A melt-down vaporises the charge without drops");
        helper.succeed();
    }

    private NuclearReactorTests() {}
}
