package ic2.neoforge.test;

import ic2.neoforge.fluid.FluidDefinition;
import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.machine.NuclearReactorBlockEntity;
import ic2.neoforge.machine.ReactorFluidPortBlockEntity;
import ic2.neoforge.registration.ModFluids;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.registration.ModMaterialBlocks;
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

    private static void placeChamber(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, ModMachines.block(MachineKind.REACTOR_CHAMBER).defaultBlockState());
    }

    private static void placePort(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, ModMachines.block(MachineKind.REACTOR_FLUID_PORT).defaultBlockState());
    }

    /** The legacy full-size fluid build: six chamber faces plus a complete vessel shell. */
    private static void buildVesselShell(GameTestHelper helper, BlockPos center) {
        var vessel = ModMaterialBlocks.MATERIALS.get("reactor_vessel").get();
        for (int dx = -2; dx <= 2; dx++)
            for (int dy = -2; dy <= 2; dy++)
                for (int dz = -2; dz <= 2; dz++) {
                    if (Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz))) != 2) continue;
                    helper.setBlock(center.offset(dx, dy, dz), vessel.defaultBlockState());
                }
    }

    /** Full 6x9 form with the shell; ports replace two shell blocks, redstone feeds the port. */
    private static void buildFullFluidStructure(GameTestHelper helper, BlockPos center) {
        for (Direction face : Direction.values()) placeChamber(helper, center.relative(face));
        buildVesselShell(helper, center);
        placePort(helper, center.east().east());
        helper.setBlock(
                center.west().west(),
                ModMachines.block(MachineKind.REACTOR_REDSTONE_PORT).defaultBlockState());
        helper.setBlock(center.west().west().west(), Blocks.REDSTONE_BLOCK);
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

    private static void cycles(NuclearReactorBlockEntity reactor, GameTestHelper helper, int n) {
        for (int i = 0; i < n * NuclearReactorBlockEntity.CYCLE_TICKS; i++)
            reactor.serverTick(helper.getLevel());
    }

    static void fluidModeRequiresFullStructure(GameTestHelper helper) {
        var reactor = reactor(helper);
        placePort(helper, POSITION.north());
        helper.setBlock(POSITION.west(), Blocks.REDSTONE_BLOCK);
        reactor.inventory()
                .set(
                        9,
                        ItemResource.of(
                                ModReactorItems.URANIUM_FUEL_ROD.get().getDefaultInstance()),
                        1);
        cycles(reactor, helper, 2);
        helper.assertTrue(
                !reactor.fluidCooled(), "An adjacent port alone never engages fluid mode");
        helper.assertTrue(
                reactor.getReactorEnergyOutput() > 0,
                "The unvalidated core keeps feeding the EU path");
        helper.assertTrue(
                reactor.hotCoolantAmount() == 0, "No hot coolant is produced outside fluid mode");
        helper.succeed();
    }

    static void fluidModeConvertsHeatToHotCoolant(GameTestHelper helper) {
        var reactor = reactor(helper);
        buildFullFluidStructure(helper, POSITION);
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
        cycles(reactor, helper, 6);
        helper.assertTrue(
                reactor.fluidCooled(),
                "The full-size vessel structure engages fluid mode, cols="
                        + reactor.columns()
                        + " ring="
                        + reactor.hasVesselRing(helper.getLevel()));
        helper.assertTrue(
                reactor.hotCoolantAmount() > 0,
                "Fluid mode converts pass heat into hot coolant, emit="
                        + reactor.emitBuffer()
                        + " heat="
                        + reactor.getHeat());
        helper.assertTrue(
                reactor.coolantAmount() < coolantBefore, "Coolant is consumed by the conversion");
        helper.assertTrue(
                reactor.hotCoolantAmount() <= coolantBefore - reactor.coolantAmount(),
                "Hot coolant is minted only from drained coolant");
        helper.assertTrue(reactor.getHeat() < 5000, "Fluid cooling keeps the core cool");
        helper.succeed();
    }

    static void hotCoolantExtractsThroughPort(GameTestHelper helper) {
        var reactor = reactor(helper);
        buildFullFluidStructure(helper, POSITION);
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
        cycles(reactor, helper, 6);
        var port = helper.getBlockEntity(POSITION.east().east(), ReactorFluidPortBlockEntity.class);
        helper.assertTrue(
                port.findReactor() == reactor,
                "The shell port attaches to the fluid-cooled core inside the vessel");
        var tanks = port.fluidAutomation(Direction.NORTH);
        int extracted;
        try (var transaction = Transaction.openRoot()) {
            extracted = tanks.extract(1, hotCoolant(), 20, transaction);
            transaction.commit();
        }
        helper.assertTrue(extracted == 20, "Hot coolant extracts through the fluid port");
        helper.succeed();
    }

    static void conflictingFluidReactorBlocksMode(GameTestHelper helper) {
        var reactor = reactor(helper);
        for (Direction face : Direction.values()) placeChamber(helper, POSITION.relative(face));
        buildVesselShell(helper, POSITION);
        var otherPos = POSITION.east(4);
        helper.setBlock(
                otherPos, ModMachines.block(MachineKind.NUCLEAR_REACTOR).defaultBlockState());
        for (Direction face : Direction.values()) placeChamber(helper, otherPos.relative(face));
        buildVesselShell(helper, otherPos);
        cycles(reactor, helper, 1);
        helper.assertTrue(
                !reactor.fluidCooled(), "A second full-size fluid core in range blocks fluid mode");
        // Breaching the rival shell frees this core to run fluid mode again.
        helper.setBlock(otherPos.east(2), Blocks.AIR);
        cycles(reactor, helper, 1);
        helper.assertTrue(
                reactor.fluidCooled(), "The core engages fluid mode once the conflict is gone");
        helper.succeed();
    }

    private ReactorFluidModeTests() {}
}
