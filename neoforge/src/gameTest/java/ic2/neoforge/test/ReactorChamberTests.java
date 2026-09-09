package ic2.neoforge.test;

import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.machine.NuclearReactorBlockEntity;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.registration.ModReactorItems;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.transfer.item.ItemResource;

final class ReactorChamberTests {
    private static final BlockPos POSITION = new BlockPos(8, 8, 8);

    private static NuclearReactorBlockEntity reactor(GameTestHelper helper) {
        helper.setBlock(
                POSITION, ModMachines.block(MachineKind.NUCLEAR_REACTOR).defaultBlockState());
        return helper.getBlockEntity(POSITION, NuclearReactorBlockEntity.class);
    }

    private static void placeChamber(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, ModMachines.block(MachineKind.REACTOR_CHAMBER).defaultBlockState());
    }

    private static void redstoneCycles(
            NuclearReactorBlockEntity reactor, GameTestHelper helper, int cycles) {
        for (int i = 0; i < cycles * NuclearReactorBlockEntity.CYCLE_TICKS; i++) {
            helper.setBlock(POSITION.west(), Blocks.REDSTONE_BLOCK);
            reactor.serverTick(helper.getLevel());
        }
    }

    static void chamberWidensGrid(GameTestHelper helper) {
        var reactor = reactor(helper);
        placeChamber(helper, POSITION.east());
        helper.assertTrue(reactor.columns() == 4, "One chamber widens the grid to four columns");
        placeChamber(helper, POSITION.west());
        helper.assertTrue(reactor.columns() == 5, "Two chambers widen the grid to five columns");
        var rod = ModReactorItems.URANIUM_FUEL_ROD.get().getDefaultInstance();
        reactor.inventory().set(3, ItemResource.of(rod), 1);
        redstoneCycles(reactor, helper, 2);
        var use =
                reactor.inventory()
                        .stack(3)
                        .getOrDefault(ic2.neoforge.component.ModDataComponents.REACTOR_USE, -1);
        helper.assertTrue(use == 2, "The extended column runs its rod, saw " + use);
        helper.succeed();
    }

    static void brokenChamberEjectsColumn(GameTestHelper helper) {
        var reactor = reactor(helper);
        placeChamber(helper, POSITION.east());
        helper.assertTrue(reactor.columns() == 4, "The chamber widens the grid");
        var rod = ModReactorItems.DUAL_URANIUM_FUEL_ROD.get().getDefaultInstance();
        reactor.inventory().set(3, ItemResource.of(rod), 1);
        helper.setBlock(POSITION.east(), net.minecraft.world.level.block.Blocks.AIR);
        helper.assertTrue(reactor.columns() == 3, "Breaking the chamber shrinks the grid");
        for (int tick = 0; tick < NuclearReactorBlockEntity.CYCLE_TICKS; tick++)
            reactor.serverTick(helper.getLevel());
        helper.assertTrue(
                reactor.inventory().stack(3).isEmpty(),
                "Items beyond the active columns are ejected");
        boolean dropped =
                helper.getEntities(net.minecraft.world.entity.EntityType.ITEM).stream()
                        .anyMatch(
                                entity ->
                                        entity.getItem()
                                                .is(ModReactorItems.DUAL_URANIUM_FUEL_ROD.get()));
        helper.assertTrue(dropped, "The ejected rod drops beside the reactor");
        helper.succeed();
    }

    static void moxPulseScalesWithHeat(GameTestHelper helper) {
        var reactor = reactor(helper);
        reactor.setHeat(5000);
        reactor.inventory()
                .set(
                        9,
                        ItemResource.of(ModReactorItems.MOX_FUEL_ROD.get().getDefaultInstance()),
                        1);
        redstoneCycles(reactor, helper, 1);
        float expected = 4.0F * (5000.0F / 10000.0F) + 1.0F;
        helper.assertTrue(
                Math.abs(reactor.getReactorEnergyOutput() - expected) < 0.5F,
                "The MOX pulse yields 4 x heatRatio + 1 EU, saw "
                        + reactor.getReactorEnergyOutput());
        int heat = reactor.getHeat();
        helper.assertTrue(heat >= 4, "The MOX heat pass adds the uranium formula heat");
        helper.succeed();
    }

    static void chamberChainWidensToNine(GameTestHelper helper) {
        var reactor = reactor(helper);
        placeChamber(helper, POSITION.east());
        placeChamber(helper, POSITION.east().east());
        placeChamber(helper, POSITION.west());
        placeChamber(helper, POSITION.west().west());
        placeChamber(helper, POSITION.north());
        placeChamber(helper, POSITION.south());
        helper.assertTrue(
                reactor.columns() == 9,
                "Chamber chains widen the grid to nine, saw " + reactor.columns());
        helper.succeed();
    }

    private ReactorChamberTests() {}
}
