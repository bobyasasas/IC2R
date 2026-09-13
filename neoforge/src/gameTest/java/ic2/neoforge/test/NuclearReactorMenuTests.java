package ic2.neoforge.test;

import ic2.neoforge.fluid.FluidDefinition;
import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.machine.NuclearReactorBlockEntity;
import ic2.neoforge.menu.NuclearReactorMenu;
import ic2.neoforge.registration.ModCells;
import ic2.neoforge.registration.ModFluids;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.registration.ModMaterialBlocks;
import ic2.neoforge.registration.ModReactorItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/** Legacy ContainerNuclearReactor: dedicated menu layout and the fluid container slot semantics. */
final class NuclearReactorMenuTests {
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
        helper.setBlock(
                pos, ModMachines.block(MachineKind.REACTOR_FLUID_PORT).defaultBlockState());
    }

    /** The legacy full-size fluid build: six chamber faces, a vessel shell, ports and redstone. */
    private static void buildFullFluidStructure(GameTestHelper helper, BlockPos center) {
        for (Direction face : Direction.values()) placeChamber(helper, center.relative(face));
        var vessel = ModMaterialBlocks.MATERIALS.get("reactor_vessel").get();
        for (int dx = -2; dx <= 2; dx++)
            for (int dy = -2; dy <= 2; dy++)
                for (int dz = -2; dz <= 2; dz++) {
                    if (Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz))) != 2) continue;
                    helper.setBlock(center.offset(dx, dy, dz), vessel.defaultBlockState());
                }
        placePort(helper, center.east().east());
        helper.setBlock(
                center.west().west(),
                ModMachines.block(MachineKind.REACTOR_REDSTONE_PORT).defaultBlockState());
        helper.setBlock(center.west().west().west(), Blocks.REDSTONE_BLOCK);
    }

    private static void cycles(NuclearReactorBlockEntity reactor, GameTestHelper helper, int n) {
        for (int i = 0; i < n * NuclearReactorBlockEntity.CYCLE_TICKS; i++)
            reactor.serverTick(helper.getLevel());
    }

    private static ItemStack coolantCell() {
        return new ItemStack(ModCells.CELLS.get("coolant_cell").get());
    }

    private static ItemStack emptyCell() {
        return new ItemStack(ModCells.EMPTY.get());
    }

    static void dedicatedMenuMirrorsLegacyLayout(GameTestHelper helper) {
        var reactor = reactor(helper);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(
                reactor.getBlockPos().getX() + 0.5,
                reactor.getBlockPos().getY() + 0.5,
                reactor.getBlockPos().getZ() + 0.5);
        var menu = new NuclearReactorMenu(1, player.getInventory(), reactor);
        helper.assertTrue(
                menu.slots.size() == NuclearReactorMenu.MACHINE_SLOTS + 36,
                "The reactor menu exposes the 9x6 grid, four fluid slots and the player"
                        + " inventory: got "
                        + menu.slots.size());
        helper.assertTrue(
                menu.slots.get(0).x == 26 && menu.slots.get(0).y == 25,
                "The component grid opens at the legacy (26,25) origin: "
                        + menu.slots.get(0).x
                        + ","
                        + menu.slots.get(0).y);
        helper.assertTrue(
                menu.slots.get(8).x == 26 + 8 * 18 && menu.slots.get(8).y == 25,
                "The grid runs nine columns wide");
        // Menu slot order: grid 0-53, coolant input 54, hot input 55, coolant output 56,
        // hot output 57, player inventory 58+. The hot pair swaps relative to inventory order.
        var coolantInput = menu.slots.get(54);
        var hotInput = menu.slots.get(55);
        var coolantOutput = menu.slots.get(56);
        var hotOutput = menu.slots.get(57);
        helper.assertTrue(
                coolantInput.x == 8 && coolantInput.y == 25,
                "The coolant input sits left of the grid at (8,25)");
        helper.assertTrue(
                hotInput.x == 188 && hotInput.y == 25,
                "The hot coolant input sits right of the grid at (188,25)");
        helper.assertTrue(
                coolantOutput.x == 8 && coolantOutput.y == 115,
                "The coolant output sits at (8,115)");
        helper.assertTrue(
                hotOutput.x == 188 && hotOutput.y == 115,
                "The hot coolant output sits at (188,115)");
        helper.assertTrue(
                menu.slots.get(58).x == 26 && menu.slots.get(58).y == 161,
                "The player inventory follows the legacy width-214 centering");
        helper.assertTrue(
                menu.slots.get(85).y == 219, "The hotbar row sits at legacy y=219");
        helper.assertTrue(
                !coolantInput.mayPlace(new ItemStack(Items.STONE)),
                "The coolant input rejects anything that is not a fluid container");
        helper.assertTrue(
                coolantInput.mayPlace(coolantCell()),
                "The coolant input accepts a coolant cell");
        helper.assertTrue(
                !hotInput.mayPlace(coolantCell()),
                "The hot coolant input rejects a cell that cannot take hot coolant");
        helper.assertTrue(
                hotInput.mayPlace(emptyCell()),
                "The hot coolant input accepts an empty cell");
        helper.assertTrue(
                !coolantOutput.mayPlace(coolantCell()) && !hotOutput.mayPlace(emptyCell()),
                "The container outputs are extraction-only");
        helper.assertTrue(
                menu.stillValid(player),
                "The dedicated menu validates against the reactor block");
        helper.succeed();
    }

    static void menuDrainsCoolantContainersIntoTheTank(GameTestHelper helper) {
        var reactor = reactor(helper);
        buildFullFluidStructure(helper, POSITION);
        var menu = new NuclearReactorMenu(1, helper.makeMockPlayer(GameType.SURVIVAL).getInventory(), reactor);
        reactor.inventory()
                .set(NuclearReactorBlockEntity.COOLANT_INPUT, ItemResource.of(coolantCell()), 1);
        cycles(reactor, helper, 1);
        helper.assertTrue(
                reactor.fluidCooled(), "The full-size vessel structure engages fluid mode");
        helper.assertTrue(
                reactor.coolantAmount() == 1000,
                "The coolant cell drains into the coolant tank: "
                        + reactor.coolantAmount());
        helper.assertTrue(
                reactor.inventory().stack(NuclearReactorBlockEntity.COOLANT_INPUT).isEmpty(),
                "The drained slot empties");
        var emptied = reactor.inventory().stack(NuclearReactorBlockEntity.COOLANT_OUTPUT);
        helper.assertTrue(
                !emptied.isEmpty() && emptied.getItem() == ModCells.EMPTY.get(),
                "The emptied container parks in the coolant output slot");
        helper.assertTrue(
                menu.coolantAmount() == 1000 && menu.fluidCooled(),
                "The menu data slots stream the tank and mode to the client side");
        helper.assertTrue(
                menu.slots.get(56).hasItem(),
                "The menu slot view follows the moved container");
        helper.succeed();
    }

    static void menuFillsHotCoolantContainersFromTheTank(GameTestHelper helper) {
        var reactor = reactor(helper);
        buildFullFluidStructure(helper, POSITION);
        for (int slot = 9; slot <= 11; slot++)
            reactor.inventory()
                    .set(
                            slot,
                            ItemResource.of(
                                    ModReactorItems.QUAD_URANIUM_FUEL_ROD
                                            .get()
                                            .getDefaultInstance()),
                            1);
        // Vents carry the rod heat into the emit buffer that fluid mode mints into hot coolant.
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
        helper.assertTrue(
                reactor.hotCoolantAmount() > 0,
                "The fluid cycles mint hot coolant for the fill test (fluid="
                        + reactor.fluidCooled()
                        + " emit="
                        + reactor.emitBuffer()
                        + " heat="
                        + reactor.getHeat()
                        + " coolant="
                        + reactor.coolantAmount()
                        + " cols="
                        + reactor.columns()
                        + " ring="
                        + reactor.hasVesselRing(helper.getLevel())
                        + ")");
        // CellFluidHandler fills whole cells only: top the hot tank up past one cell after the
        // minting proof, mirroring the legacy tank that keeps at least 1000 mB on hand.
        try (var transaction = Transaction.openRoot()) {
            reactor.hotCoolantTankHandler().insert(0, hotCoolant(), 2000, transaction);
            transaction.commit();
        }
        int hotBefore = reactor.hotCoolantAmount();
        reactor.inventory()
                .set(
                        NuclearReactorBlockEntity.HOT_COOLANT_INPUT,
                        ItemResource.of(emptyCell()),
                        1);
        cycles(reactor, helper, 1);
        helper.assertTrue(
                reactor.inventory().stack(NuclearReactorBlockEntity.HOT_COOLANT_INPUT).isEmpty(),
                "The filled-from slot empties (input="
                        + reactor.inventory().stack(NuclearReactorBlockEntity.HOT_COOLANT_INPUT)
                        + " output="
                        + reactor.inventory().stack(NuclearReactorBlockEntity.HOT_COOLANT_OUTPUT)
                        + " hot="
                        + reactor.hotCoolantAmount()
                        + " of "
                        + hotBefore
                        + ")");
        var filled = reactor.inventory().stack(NuclearReactorBlockEntity.HOT_COOLANT_OUTPUT);
        helper.assertTrue(
                !filled.isEmpty()
                        && ic2.neoforge.menu.NuclearReactorMenu.holdsFluid(
                                ItemResource.of(filled),
                                ModFluids.FAMILIES.get(FluidDefinition.HOT_COOLANT)
                                        .source()
                                        .get()),
                "The filled hot coolant container parks in the hot output slot");
        helper.assertTrue(
                reactor.hotCoolantAmount() == hotBefore - 1000,
                "The fill draws exactly one cell worth from the hot tank: "
                        + reactor.hotCoolantAmount());
        helper.succeed();
    }

    private static FluidResource coolant() {
        return FluidResource.of(
                ModFluids.FAMILIES.get(FluidDefinition.COOLANT).source().get());
    }

    private static FluidResource hotCoolant() {
        return FluidResource.of(
                ModFluids.FAMILIES.get(FluidDefinition.HOT_COOLANT).source().get());
    }

    private NuclearReactorMenuTests() {}
}
