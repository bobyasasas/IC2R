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
        reactor.inventory().set(9, ItemResource.of(rod), 1);
        cycles(reactor, helper, 3);
        var slot = reactor.inventory().stack(9);
        helper.assertTrue(
                slot.getItem() instanceof FuelRodItem,
                "A fresh rod keeps running for 20000 cycles before depleting");
        var use = slot.getOrDefault(ic2.neoforge.component.ModDataComponents.REACTOR_USE, -1);
        helper.assertTrue(use == 3, "Three cycles deplete the rod by three, saw " + use);
        int pulses = 1 + 1 / 2;
        helper.assertTrue(
                reactor.getReactorEnergyOutput() == pulses,
                "A single uranium rod pulses once per cycle");
        helper.assertTrue(reactor.getHeat() == 12, "Three uncooled cycles accumulate twelve heat");
        helper.succeed();
    }

    static void adjacentRodMultipliesHeat(GameTestHelper helper) {
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
                                ModReactorItems.URANIUM_FUEL_ROD.get().getDefaultInstance()),
                        1);
        cycles(reactor, helper, 2);
        helper.assertTrue(
                reactor.getReactorEnergyOutput() > 2,
                "Adjacent rods pulse each other for extra output, output="
                        + reactor.getReactorEnergyOutput());
        helper.assertTrue(
                reactor.getHeat() > 8, "Neighbouring pulses multiply the triangular heat formula");
        helper.succeed();
    }

    static void ventAbsorbsRodHeat(GameTestHelper helper) {
        var reactor = reactor(helper);
        reactor.inventory()
                .set(
                        9,
                        ItemResource.of(
                                ModReactorItems.URANIUM_FUEL_ROD.get().getDefaultInstance()),
                        1);
        reactor.inventory()
                .set(10, ItemResource.of(ModReactorItems.REACTOR_COOLANT_CELL.get().getDefaultInstance()), 1);
        cycles(reactor, helper, 3);
        var ventItem = (ic2.neoforge.item.HeatStorageComponent) ModReactorItems.REACTOR_COOLANT_CELL.get();
        int ventStored = ventItem.currentHeat(reactor.inventory().stack(10));
        helper.assertTrue(ventStored == 12, "The coolant cell absorbed the rod's heat, saw " + ventStored);
        helper.assertTrue(ventStored <= ventItem.capacity(), "The vent never exceeds capacity");
        helper.succeed();
    }

    static void coolantCellDestroysOnOverflow(GameTestHelper helper) {
        var reactor = reactor(helper);
        reactor.inventory()
                .set(
                        9,
                        ItemResource.of(
                                ModReactorItems.URANIUM_FUEL_ROD.get().getDefaultInstance()),
                        1);
        var cell = (ic2.neoforge.item.HeatStorageComponent) ModReactorItems.REACTOR_COOLANT_CELL.get();
        var charged = cell.getDefaultInstance();
        charged.set(ic2.neoforge.component.ModDataComponents.REACTOR_HEAT, cell.capacity() - 1);
        reactor.inventory().set(10, ItemResource.of(charged), 1);
        cycles(reactor, helper, 1);
        var cellRemains = reactor.getItemAt(1, 1);
        helper.assertTrue(
                cellRemains == null || cellRemains.isEmpty(),
                "A heat push past capacity consumes the coolant cell");
        helper.assertTrue(
                reactor.getHeat() == 0,
                "The legacy overflow books negatively, so the core gains no heat");
        helper.succeed();
    }

    static void heatVentDestroysOnOverflow(GameTestHelper helper) {
        var reactor = reactor(helper);
        reactor.inventory()
                .set(
                        9,
                        ItemResource.of(
                                ModReactorItems.URANIUM_FUEL_ROD.get().getDefaultInstance()),
                        1);
        var vent = ModReactorItems.HEAT_VENT.get();
        var charged = vent.getDefaultInstance();
        charged.set(ic2.neoforge.component.ModDataComponents.REACTOR_HEAT, vent.capacity() - 1);
        reactor.inventory().set(10, ItemResource.of(charged), 1);
        cycles(reactor, helper, 1);
        var ventRemains = reactor.getItemAt(1, 1);
        helper.assertTrue(
                ventRemains == null || ventRemains.isEmpty(),
                "Vents share the legacy consume-on-overflow rule");
        helper.assertTrue(reactor.getHeat() == 0, "The vent overflow never reaches the core");
        helper.succeed();
    }

    static void condensatorAbsorbsRodHeat(GameTestHelper helper) {
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
                        ItemResource.of(ModReactorItems.RSH_CONDENSATOR.get().getDefaultInstance()),
                        1);
        cycles(reactor, helper, 1);
        var condensator = ModReactorItems.RSH_CONDENSATOR.get();
        helper.assertValueEqual(
                condensator.storedHeat(reactor.inventory().stack(10)),
                4,
                "The condensator soaks the single rod pulse of four heat");
        helper.assertValueEqual(reactor.getHeat(), 0, "The core stays cold behind the condensator");
        helper.assertValueEqual(
                reactor.getReactorEnergyOutput(),
                1.0F,
                "The condensator does not change the pulse count");
        helper.succeed();
    }

    static void moxRodBreederOutput(GameTestHelper helper) {
        var reactor = reactor(helper);
        reactor.setHeat(5000);
        reactor.inventory()
                .set(
                        9,
                        ItemResource.of(ModReactorItems.MOX_FUEL_ROD.get().getDefaultInstance()),
                        1);
        cycles(reactor, helper, 1);
        float output = reactor.getReactorEnergyOutput();
        helper.assertTrue(
                output > 3.0F && output < 3.1F,
                "A half-hot core pays every MOX pulse four times the heat ratio plus one, saw "
                        + output);
        helper.succeed();
    }

    static void meltDownExplodesCore(GameTestHelper helper) {
        var reactor = reactor(helper);
        for (int slot = 9; slot <= 11; slot++)
            reactor.inventory()
                    .set(
                            slot,
                            ItemResource.of(
                                    ModReactorItems.QUAD_URANIUM_FUEL_ROD
                                            .get()
                                            .getDefaultInstance()),
                            1);
        cycles(reactor, helper, 20);
        helper.assertTrue(
                !helper.getBlockState(POSITION).is(ModMachines.block(MachineKind.NUCLEAR_REACTOR)),
                "A full melt-down removes the reactor core");
        helper.assertTrue(reactor.getHeat() == 0, "The melt-down resets the heat accounting");
        boolean debris = helper.getEntities(net.minecraft.world.entity.EntityType.ITEM).isEmpty();
        helper.assertTrue(debris, "A melt-down vaporises the charge without drops");
        helper.succeed();
    }

    private NuclearReactorTests() {}
}
