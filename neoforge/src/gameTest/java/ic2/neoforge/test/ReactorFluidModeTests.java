package ic2.neoforge.test;

import ic2.neoforge.fluid.FluidDefinition;
import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.machine.NuclearReactorBlockEntity;
import ic2.neoforge.machine.ReactorFluidPortBlockEntity;
import ic2.neoforge.registration.ModFluids;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.registration.ModReactorItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

final class ReactorFluidModeTests {
    private static final BlockPos POSITION = new BlockPos(8, 8, 8);

    private static NuclearReactorBlockEntity reactor(GameTestHelper helper) {
        helper.setBlock(
                POSITION, ModMachines.block(MachineKind.NUCLEAR_REACTOR).defaultBlockState());
        return helper.getBlockEntity(POSITION, NuclearReactorBlockEntity.class);
    }

    private static void placePort(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, ModMachines.block(MachineKind.REACTOR_FLUID_PORT).defaultBlockState());
        helper.getBlockEntity(pos, ReactorFluidPortBlockEntity.class);
    }

    private static FluidResource coolant() {
        return FluidResource.of(ModFluids.FAMILIES.get(FluidDefinition.COOLANT).source().get());
    }

    private static FluidResource hotCoolant() {
        return FluidResource.of(ModFluids.FAMILIES.get(FluidDefinition.HOT_COOLANT).source().get());
    }

    private static void loadRods(NuclearReactorBlockEntity reactor) {
        for (int slot = 9; slot <= 11; slot++)
            reactor.inventory()
                    .set(
                            slot,
                            ItemResource.of(
                                    ModReactorItems.QUAD_URANIUM_FUEL_ROD
                                            .get()
                                            .getDefaultInstance()),
                            1);
    }

    static void fluidModeConvertsHeatToHotCoolant(GameTestHelper helper) {
        var level = helper.getLevel();
        var reactor = reactor(helper);
        placePort(helper, POSITION.north());
        loadRods(reactor);
        for (int slot = 18; slot <= 20; slot++)
            reactor.inventory()
                    .set(
                            slot,
                            ItemResource.of(
                                    ModReactorItems.REACTOR_HEAT_VENT.get().getDefaultInstance()),
                            1);
        try (var transaction = Transaction.openRoot()) {
            reactor.coolantTanks().insert(0, coolant(), 6000, transaction);
            transaction.commit();
        }
        int coolantBefore = reactor.coolantAmount();
        cyclesWithRedstone(reactor, helper, 6);
        helper.assertTrue(
                reactor.hotCoolantAmount() > 0,
                "Fluid mode converts pass heat into hot coolant, cooled="
                        + reactor.fluidCooled()
                        + " emit="
                        + reactor.emitBuffer()
                        + " heat="
                        + reactor.getHeat());
        helper.assertTrue(
                reactor.coolantAmount() < coolantBefore, "Coolant is consumed by the conversion");
        helper.assertTrue(reactor.getHeat() < 5000, "Fluid cooling keeps the core cool");
        helper.succeed();
    }

    static void hotCoolantExtractsThroughPort(GameTestHelper helper) {
        var level = helper.getLevel();
        var reactor = reactor(helper);
        placePort(helper, POSITION.north());
        loadRods(reactor);
        for (int slot = 18; slot <= 20; slot++)
            reactor.inventory()
                    .set(
                            slot,
                            ItemResource.of(
                                    ModReactorItems.REACTOR_HEAT_VENT.get().getDefaultInstance()),
                            1);
        try (var transaction = Transaction.openRoot()) {
            reactor.coolantTanks().insert(0, coolant(), 6000, transaction);
            transaction.commit();
        }
        cyclesWithRedstone(reactor, helper, 6);
        var port = helper.getBlockEntity(POSITION.north(), ReactorFluidPortBlockEntity.class);
        var tanks = port.fluidAutomation(Direction.NORTH);
        int extracted;
        try (var transaction = Transaction.openRoot()) {
            extracted = tanks.extract(1, hotCoolant(), 1000, transaction);
            transaction.commit();
        }
        helper.assertTrue(extracted > 0, "Hot coolant extracts through the fluid port");
        helper.succeed();
    }

    private static void cyclesWithRedstone(
            NuclearReactorBlockEntity reactor, GameTestHelper helper, int cycles) {
        for (int i = 0; i < cycles * NuclearReactorBlockEntity.CYCLE_TICKS; i++) {
            helper.setBlock(POSITION.west(), Blocks.REDSTONE_BLOCK);
            reactor.serverTick(helper.getLevel());
        }
    }

    private ReactorFluidModeTests() {}
}
