package ic2.neoforge.test;

import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.machine.StorageBoxBlockEntity;
import ic2.neoforge.machine.TradeOMatBlockEntity;
import ic2.neoforge.menu.MachineMenu;
import ic2.neoforge.registration.ModMachines;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

final class TradeOMatTests {
    private static final BlockPos POSITION = new BlockPos(2, 1, 2);
    private static final ItemResource STICK = ItemResource.of(Items.STICK);
    private static final ItemResource DIAMOND = ItemResource.of(Items.DIAMOND);

    static void infiniteTrade(GameTestHelper helper) {
        var machine = machine(helper);
        set(machine, TradeOMatBlockEntity.DEMAND, STICK, 1);
        set(machine, TradeOMatBlockEntity.OFFER, DIAMOND, 1);
        machine.toggleInfinite();
        insertInput(machine);
        machine.serverTick(helper.getLevel());
        helper.assertTrue(
                machine.inventory().stack(TradeOMatBlockEntity.INPUT).isEmpty(),
                "The demanded stack is consumed");
        helper.assertValueEqual(
                machine.inventory().getAmountAsInt(TradeOMatBlockEntity.OUTPUT),
                1,
                "The infinite offer is conjured into the output");
        helper.assertValueEqual(machine.totalTradeCount(), 1, "The trade is counted");
        helper.succeed();
    }

    static void suppliedTrade(GameTestHelper helper) {
        var machine = machine(helper);
        var supply = box(helper, POSITION.east());
        try (var transaction = Transaction.openRoot()) {
            supply.inventory().insert(0, DIAMOND, 3, transaction);
            transaction.commit();
        }
        set(machine, TradeOMatBlockEntity.DEMAND, STICK, 1);
        set(machine, TradeOMatBlockEntity.OFFER, DIAMOND, 1);
        insertInput(machine);
        machine.serverTick(helper.getLevel());
        helper.assertValueEqual(
                machine.inventory().getAmountAsInt(TradeOMatBlockEntity.OUTPUT),
                1,
                "The offer comes out of the adjacent supply");
        helper.assertValueEqual(
                supply.inventory().getAmountAsInt(0), 2, "The supply lost exactly one offer");
        helper.assertValueEqual(
                supply.inventory().getAmountAsInt(1),
                1,
                "The traded-in stick returns to the supply");
        helper.succeed();
    }

    /** Legacy checkAccess: first opener claims, later non-operator openers get the Closed menu. */
    static void ownerAndVisitorMenus(GameTestHelper helper) {
        var machine = machine(helper);
        var owner = helper.makeMockPlayer(GameType.SURVIVAL);
        var ownerMenu = (MachineMenu) machine.createMenu(1, owner.getInventory(), owner);
        helper.assertTrue(
                machine.owned() && ownerMenu.tradeEditable(),
                "The first opener claims the terminal and gets the editable legacy Open menu");
        ownerMenu.setCarried(new ItemStack(Items.STICK));
        ownerMenu.clicked(0, TradeOMatBlockEntity.DEMAND, ContainerInput.PICKUP, owner);
        helper.assertTrue(
                machine.inventory().stack(TradeOMatBlockEntity.DEMAND).is(Items.STICK),
                "The owner slots a demand template through the GUI");
        var visitor = helper.makeMockPlayer(GameType.SURVIVAL);
        var visitorMenu = (MachineMenu) machine.createMenu(2, visitor.getInventory(), visitor);
        helper.assertTrue(
                !visitorMenu.tradeEditable(),
                "A non-operator opener gets the read-only legacy Closed menu");
        var demand = visitorMenu.slots.get(TradeOMatBlockEntity.DEMAND);
        var offer = visitorMenu.slots.get(TradeOMatBlockEntity.OFFER);
        helper.assertTrue(
                !demand.mayPlace(new ItemStack(Items.STICK)) && !demand.mayPickup(visitor),
                "Visitors can neither fill nor drain the demand template");
        helper.assertTrue(
                !offer.mayPlace(new ItemStack(Items.DIAMOND)) && !offer.mayPickup(visitor),
                "Visitors can neither fill nor drain the offer template");
        helper.assertTrue(
                visitorMenu.slots.get(TradeOMatBlockEntity.INPUT)
                        .mayPlace(new ItemStack(Items.STICK)),
                "Visitors still trade by inserting the demanded stack");
        helper.succeed();
    }

    /** Legacy canToggleInfinite: the ∞ flip is operator-only, even for the claimant. */
    static void toggleNeedsOperator(GameTestHelper helper) {
        var machine = machine(helper);
        var owner = helper.makeMockPlayer(GameType.SURVIVAL);
        var menu = (MachineMenu) machine.createMenu(1, owner.getInventory(), owner);
        owner.setPos(helper.absolutePos(POSITION).getCenter());
        owner.containerMenu = menu;
        helper.assertTrue(
                menu.stillValid(owner),
                "The claimant stands at the terminal, so the click path is reachable");
        helper.assertTrue(!machine.infinite(), "A fresh terminal starts finite");
        helper.assertTrue(
                !menu.clickMenuButton(owner, 0),
                "The non-operator claimant cannot flip the infinite flag from the GUI");
        helper.assertTrue(
                !machine.infinite(), "The rejected click leaves the infinite flag off");
        machine.toggleInfinite();
        helper.assertTrue(
                machine.infinite() && machine.stock() == -1,
                "The direct path still flips the flag for the operator-only surface");
        helper.succeed();
    }

    private static TradeOMatBlockEntity machine(GameTestHelper helper) {
        helper.setBlock(POSITION, ModMachines.block(MachineKind.TRADE_O_MAT));
        return helper.getBlockEntity(POSITION, TradeOMatBlockEntity.class);
    }

    private static StorageBoxBlockEntity box(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, ModMachines.block(MachineKind.WOODEN_STORAGE_BOX));
        return helper.getBlockEntity(pos, StorageBoxBlockEntity.class);
    }

    private static void set(
            TradeOMatBlockEntity machine, int slot, ItemResource resource, int count) {
        try (var transaction = Transaction.openRoot()) {
            machine.inventory().insert(slot, resource, count, transaction);
            transaction.commit();
        }
    }

    private static void insertInput(TradeOMatBlockEntity machine) {
        try (var transaction = Transaction.openRoot()) {
            machine.inventory().insert(TradeOMatBlockEntity.INPUT, STICK, 1, transaction);
            transaction.commit();
        }
    }

    private TradeOMatTests() {}
}
