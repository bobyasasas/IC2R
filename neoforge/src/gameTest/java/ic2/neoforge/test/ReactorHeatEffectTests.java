package ic2.neoforge.test;

import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.machine.NuclearReactorBlockEntity;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.registration.ModReactorItems;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.transfer.item.ItemResource;

final class ReactorHeatEffectTests {
    private static final BlockPos POSITION = new BlockPos(8, 8, 8);

    private static NuclearReactorBlockEntity reactor(GameTestHelper helper) {
        helper.setBlock(
                POSITION, ModMachines.block(MachineKind.NUCLEAR_REACTOR).defaultBlockState());
        return helper.getBlockEntity(POSITION, NuclearReactorBlockEntity.class);
    }

    private static void cyclesWithRedstone(
            NuclearReactorBlockEntity reactor, GameTestHelper helper, int cycles) {
        for (int i = 0; i < cycles * NuclearReactorBlockEntity.CYCLE_TICKS; i++) {
            helper.setBlock(POSITION.west(), Blocks.REDSTONE_BLOCK);
            reactor.serverTick(helper.getLevel());
        }
    }

    /** Three quad rods around one heat vent exceed the 50% threshold within a few cycles. */
    private static void overheatToHalf(
            NuclearReactorBlockEntity reactor, GameTestHelper helper, ServerLevel level) {
        for (int slot = 9; slot <= 11; slot++)
            reactor.inventory()
                    .set(
                            slot,
                            ItemResource.of(
                                    ModReactorItems.QUAD_URANIUM_FUEL_ROD
                                            .get()
                                            .getDefaultInstance()),
                            1);
        cyclesWithRedstone(reactor, helper, 10);
        helper.assertTrue(
                reactor.getHeat() >= 5000,
                "The core passed the 50% heat threshold, heat=" + reactor.getHeat());
    }

    static void heatpackWarmsVentStorage(GameTestHelper helper) {
        var reactor = reactor(helper);
        reactor.inventory()
                .set(9, ItemResource.of(ModReactorItems.HEATPACK.get().getDefaultInstance()), 60);
        reactor.inventory()
                .set(
                        10,
                        ItemResource.of(
                                ModReactorItems.REACTOR_COOLANT_CELL.get().getDefaultInstance()),
                        1);
        helper.setBlock(POSITION.west(), Blocks.REDSTONE_BLOCK);
        reactor.setHeat(500);
        for (int tick = 0; tick < NuclearReactorBlockEntity.CYCLE_TICKS; tick++)
            reactor.serverTick(helper.getLevel());
        var cell = ModReactorItems.REACTOR_COOLANT_CELL.get();
        int stored = cell.currentHeat(reactor.inventory().stack(10));
        helper.assertTrue(stored == 60, "The heatpack warmed the cell by 60, saw " + stored);
        helper.assertTrue(reactor.getHeat() <= 500, "The warmth came from the core heat budget");
        helper.succeed();
    }

    private ReactorHeatEffectTests() {}
}
