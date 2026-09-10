package ic2.neoforge.test;

import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.machine.NuclearReactorBlockEntity;
import ic2.neoforge.machine.ReactorAccessHatchBlockEntity;
import ic2.neoforge.machine.ReactorRciBlockEntity;
import ic2.neoforge.machine.ReactorRedstonePortBlockEntity;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.registration.ModReactorItems;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

final class ReactorAccessHatchTests {
    private static final BlockPos POSITION = new BlockPos(8, 8, 8);

    private static NuclearReactorBlockEntity reactor(GameTestHelper helper) {
        helper.setBlock(
                POSITION, ModMachines.block(MachineKind.NUCLEAR_REACTOR).defaultBlockState());
        return helper.getBlockEntity(POSITION, NuclearReactorBlockEntity.class);
    }

    private static ItemStack freshRod() {
        return ModReactorItems.URANIUM_FUEL_ROD.get().getDefaultInstance();
    }

    static void redstonePortPowersCore(GameTestHelper helper) {
        var reactor = reactor(helper);
        helper.setBlock(
                POSITION.east(),
                ModMachines.block(MachineKind.REACTOR_REDSTONE_PORT).defaultBlockState());
        var port = helper.getBlockEntity(POSITION.east(), ReactorRedstonePortBlockEntity.class);
        reactor.inventory().set(9, ItemResource.of(freshRod()), 1);
        helper.setBlock(POSITION.east().east(), Blocks.REDSTONE_BLOCK);
        for (int i = 0; i < NuclearReactorBlockEntity.CYCLE_TICKS; i++) {
            helper.setBlock(POSITION.west(), Blocks.AIR);
            reactor.serverTick(helper.getLevel());
        }
        helper.assertTrue(
                ReactorRedstonePortBlockEntity.poweredPortNear(reactor, helper.getLevel()) != null,
                "The redstone port is detected from the core");
        helper.assertTrue(
                reactor.getHeat() >= 4, "The port's redstone input drives the core's pulse cycle");
        helper.succeed();
    }

    static void hatchExposesGrid(GameTestHelper helper) {
        var reactor = reactor(helper);
        helper.setBlock(
                POSITION.east(),
                ModMachines.block(MachineKind.REACTOR_ACCESS_HATCH).defaultBlockState());
        var hatch = helper.getBlockEntity(POSITION.east(), ReactorAccessHatchBlockEntity.class);
        helper.assertTrue(hatch.findReactor() == reactor, "The hatch finds the adjacent core");
        try (var transaction = Transaction.openRoot()) {
            int inserted =
                    hatch.automation(net.minecraft.core.Direction.UP)
                            .insert(9, ItemResource.of(freshRod()), 1, transaction);
            helper.assertTrue(inserted == 1, "The hatch exposes the grid to item insertion");
            transaction.commit();
        }
        helper.assertTrue(
                reactor.inventory().stack(9).is(ModReactorItems.URANIUM_FUEL_ROD.get()),
                "The inserted rod sits in the reactor grid");
        helper.succeed();
    }

    static void rciRechargesCondensator(GameTestHelper helper) {
        var reactor = reactor(helper);
        helper.setBlock(
                POSITION.east(), ModMachines.block(MachineKind.RCI_RSH).defaultBlockState());
        var rci = helper.getBlockEntity(POSITION.east(), ReactorRciBlockEntity.class);
        var condensator = ModReactorItems.RSH_CONDENSATOR.get().getDefaultInstance();
        condensator.set(ic2.neoforge.component.ModDataComponents.REACTOR_HEAT, 18000);
        reactor.inventory().set(9, ItemResource.of(condensator), 1);
        rci.inventory().set(0, ItemResource.of(new ItemStack(Blocks.REDSTONE_BLOCK)), 1);
        rci.energy().insert(1000);
        for (int tick = 0; tick < 4; tick++) rci.serverTick(helper.getLevel());
        var stored =
                reactor.inventory()
                        .stack(9)
                        .getOrDefault(ic2.neoforge.component.ModDataComponents.REACTOR_HEAT, -1);
        helper.assertTrue(stored == 0, "The RCI fully recharged the condensator, saw " + stored);
        boolean coolantGone = rci.inventory().stack(0).isEmpty();
        helper.assertTrue(coolantGone, "The redstone block coolant was consumed");
        helper.succeed();
    }

    static void rciBonusRaisesConversion(GameTestHelper helper) {
        var reactor = reactor(helper);
        helper.setBlock(
                POSITION.north(), ModMachines.block(MachineKind.RCI_RSH).defaultBlockState());
        helper.assertTrue(
                reactor.rciOutputBonus() == 10,
                "One adjacent RCI contributes a ten-point output bonus");
        helper.assertTrue(!reactor.isFullSize(), "Three columns is not the full 6x9 form");
        helper.succeed();
    }

    static void vesselRingDetection(GameTestHelper helper) {
        var reactor = reactor(helper);
        var vessel =
                ic2.neoforge.registration.ModMaterialBlocks.MATERIALS.get("reactor_vessel").get();
        // Build the Chebyshev-radius-2 shell around the core (98 blocks).
        for (int dx = -2; dx <= 2; dx++)
            for (int dy = -2; dy <= 2; dy++)
                for (int dz = -2; dz <= 2; dz++) {
                    if (Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz))) != 2) continue;
                    helper.setBlock(POSITION.offset(dx, dy, dz), vessel.defaultBlockState());
                }
        helper.assertTrue(
                reactor.hasVesselRing(helper.getLevel()),
                "The vessel ring is detected once the shell is built");
        helper.succeed();
    }

    static void vesselRingAcceptsWallPieces(GameTestHelper helper) {
        var reactor = reactor(helper);
        var vessel =
                ic2.neoforge.registration.ModMaterialBlocks.MATERIALS.get("reactor_vessel").get();
        for (int dx = -2; dx <= 2; dx++)
            for (int dy = -2; dy <= 2; dy++)
                for (int dz = -2; dz <= 2; dz++) {
                    if (Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz))) != 2) continue;
                    helper.setBlock(POSITION.offset(dx, dy, dz), vessel.defaultBlockState());
                }
        // Ports count as wall pieces (legacy isWall), fuel-rod chambers do not.
        helper.setBlock(
                POSITION.east().east(),
                ModMachines.block(MachineKind.REACTOR_FLUID_PORT).defaultBlockState());
        helper.assertTrue(
                reactor.hasVesselRing(helper.getLevel()),
                "A fluid port in the shell keeps the ring complete");
        helper.setBlock(
                POSITION.east().east(),
                ModMachines.block(MachineKind.REACTOR_CHAMBER).defaultBlockState());
        helper.assertTrue(
                !reactor.hasVesselRing(helper.getLevel()),
                "A fuel-rod chamber is not a shell wall piece");
        helper.succeed();
    }

    private ReactorAccessHatchTests() {}
}
