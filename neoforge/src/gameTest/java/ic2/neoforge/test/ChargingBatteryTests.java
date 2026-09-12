package ic2.neoforge.test;

import ic2.neoforge.item.ChargingBatteryItem;
import ic2.neoforge.item.ElectricItemEnergy;
import ic2.neoforge.registration.ModItems;
import ic2.neoforge.registration.ModTools;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

/**
 * Legacy single-use battery: one right click dumps the 1200 EU cell into the other hotbar stacks
 * and is consumed when any energy found a home. Legacy charging battery: while carried it feeds
 * the other hotbar stacks a transfer limit worth of EU every ten ticks per tier, gated by a
 * right-click mode that never drains sibling charging batteries.
 */
final class ChargingBatteryTests {
    static void singleUseChargesHotbarAndConsumes(GameTestHelper helper) {
        ItemStack cell = new ItemStack(ModItems.SINGLE_USE_BATTERY.get());
        cell.setCount(5);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, cell);
        ItemStack drill = new ItemStack(ModTools.DRILL.get());
        player.getInventory().setItem(1, drill);

        var item = ModItems.SINGLE_USE_BATTERY.get();
        var result = item.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);

        helper.assertTrue(result.consumesAction(), "A paying cell must report success");
        helper.assertTrue(
                ElectricItemEnergy.charge(drill) == 1200.0,
                "The cell must dump its full 1200 EU into the drained drill");
        helper.assertTrue(cell.getCount() == 4, "The used cell must leave the stack");

        // Top the drill off so the next cell has nowhere to discharge into.
        ElectricItemEnergy.charge(drill, 28800.0, 1, true, false);
        result = item.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(
                !result.consumesAction(),
                "With every hotbar device full the cell must pass and survive");
        helper.assertTrue(cell.getCount() == 4, "A passed cell must not be consumed");
        helper.succeed();
    }

    static void chargingBatteryTickChargesHotbar(GameTestHelper helper) {
        ItemStack battery = chargedBattery();
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, battery);
        ItemStack drill = new ItemStack(ModTools.DRILL.get());
        player.getInventory().setItem(1, drill);
        ItemStack sibling = new ItemStack(ModItems.CHARGING_ENERGY_CRYSTAL.get());
        player.getInventory().setItem(2, sibling);

        onQualifyingTick(helper, () -> {
            ModItems.CHARGING_RE_BATTERY
                    .get()
                    .inventoryTick(battery, helper.getLevel(), player, null);
            helper.assertTrue(
                    ElectricItemEnergy.charge(drill) == 100.0,
                    "One feeding tick must top the drill by its own 100 EU transfer limit");
            helper.assertTrue(
                    ElectricItemEnergy.charge(battery) == 39900.0,
                    "The battery must pay exactly what the drill accepted");
            helper.assertTrue(
                    ElectricItemEnergy.charge(sibling) == 0.0,
                    "Sibling charging batteries must never be drained into");
            helper.succeed();
        });
    }

    static void modeCycleGatesFeeding(GameTestHelper helper) {
        ItemStack battery = chargedBattery();
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, battery);
        ItemStack drill = new ItemStack(ModTools.DRILL.get());
        player.getInventory().setItem(1, drill);
        var item = ModItems.CHARGING_RE_BATTERY.get();

        item.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(
                ChargingBatteryItem.mode(battery) == ChargingBatteryItem.Mode.DISABLED,
                "The first right click must disable feeding");

        onQualifyingTick(helper, () -> {
            item.inventoryTick(battery, helper.getLevel(), player, null);
            helper.assertTrue(
                    ElectricItemEnergy.charge(drill) == 0.0,
                    "A disabled battery must not feed the hotbar");

            item.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(
                    ChargingBatteryItem.mode(battery) == ChargingBatteryItem.Mode.NOT_IN_HAND,
                    "Cycling from disabled must reach the not-in-hand mode");

            // Not-in-hand skips the held stack (hotbar slot 0); move the drill there.
            player.getInventory().setItem(0, drill);
            player.getInventory().setItem(1, battery);
            player.getInventory().setSelectedSlot(0);
            item.inventoryTick(battery, helper.getLevel(), player, null);
            helper.assertTrue(
                    ElectricItemEnergy.charge(drill) == 0.0,
                    "The not-in-hand mode must skip the held stack");

            player.getInventory().setItem(0, ItemStack.EMPTY);
            player.getInventory().setItem(2, drill);
            item.inventoryTick(battery, helper.getLevel(), player, null);
            helper.assertTrue(
                    ElectricItemEnergy.charge(drill) == 100.0,
                    "The not-in-hand mode must feed every non-held hotbar stack");
            helper.assertTrue(
                    ElectricItemEnergy.charge(battery) == 39900.0,
                    "The battery must pay exactly what the drill accepted");
            helper.succeed();
        });
    }

    private static ItemStack chargedBattery() {
        ItemStack battery = new ItemStack(ModItems.CHARGING_RE_BATTERY.get());
        ElectricItemEnergy.charge(battery, 40000.0, 1, true, false);
        return battery;
    }

    /**
     * The legacy feeding gate is world time modulo ten under the battery tier; schedule the
     * single feeding tick onto the next qualifying world tick so the assertion is deterministic.
     */
    private static void onQualifyingTick(GameTestHelper helper, Runnable assertion) {
        long phase = helper.getLevel().getGameTime() % 10L;
        if (phase < 1) {
            assertion.run();
        } else {
            helper.runAfterDelay(10L - phase, assertion);
        }
    }
}
