package ic2.neoforge.test;

import ic2.neoforge.component.AdvancedFilterSettings;
import ic2.neoforge.component.ModDataComponents;
import ic2.neoforge.item.ElectricItemEnergy;
import ic2.neoforge.item.UpgradeItem;
import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.machine.UpgradeableBlockEntity;
import ic2.neoforge.menu.AdvancedMenus;
import ic2.neoforge.menu.AdvancedUpgradeMenu;
import ic2.neoforge.menu.AdvancedValueConfigMenu;
import ic2.neoforge.registration.ModItems;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.registration.ModUpgrades;
import ic2.neoforge.transfer.AdvancedUpgradeFilter;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.transfer.item.ItemResource;

import java.util.List;

final class AdvancedUpgradeTests {
    static void registration(GameTestHelper helper) {
        helper.assertTrue(
                ModUpgrades.ALL.get(UpgradeItem.Kind.ADVANCED_EJECTOR).get() instanceof UpgradeItem
                        && ModUpgrades.ALL.get(UpgradeItem.Kind.ADVANCED_PULLING).get()
                                instanceof UpgradeItem,
                "Both advanced upgrades must be registered");
        // Legacy couples each advanced kind with its plain kind on every machine.
        for (var machine : MachineKind.values()) {
            helper.assertTrue(
                    UpgradeItem.Kind.ADVANCED_EJECTOR.suitable(machine)
                            == UpgradeItem.Kind.EJECTOR.suitable(machine)
                            && UpgradeItem.Kind.ADVANCED_PULLING.suitable(machine)
                                    == UpgradeItem.Kind.PULLING.suitable(machine),
                    "Advanced kinds must be suitable exactly where their plain kinds are");
        }
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var menu =
                new ic2.neoforge.menu.MachineMenu(
                        11, player.getInventory(), machine(helper, new BlockPos(5, 1, 5)));
        var ejector = advanced(UpgradeItem.Kind.ADVANCED_EJECTOR, 1);
        var pulling = advanced(UpgradeItem.Kind.ADVANCED_PULLING, 1);
        helper.assertTrue(
                menu.slots.get(3).mayPlace(ejector) && menu.slots.get(3).mayPlace(pulling),
                "Upgrade slots must accept both advanced kinds");
        helper.assertTrue(
                !menu.slots.get(3).mayPlace(new ItemStack(Items.DIAMOND)),
                "Upgrade slots must still reject ordinary items");
        // Right-clicking a plain upgrade does nothing; the advanced one opens its editor
        // (the mock player is not a ServerPlayer, so the open itself stays server-only).
        helper.assertTrue(
                ModUpgrades.ALL.get(UpgradeItem.Kind.EJECTOR)
                                .get()
                                .use(helper.getLevel(), player, InteractionHand.MAIN_HAND)
                        == InteractionResult.PASS,
                "Plain upgrades must not react to a right click in air");
        helper.assertTrue(
                ejector.getItem()
                                .use(helper.getLevel(), player, InteractionHand.MAIN_HAND)
                        == InteractionResult.SUCCESS,
                "Advanced upgrades must react to a right click in air");
        // Dropping the edited upgrade closes its editor, as in legacy onDroppedByPlayer.
        player.getInventory().setItem(0, ejector);
        var editor = new AdvancedUpgradeMenu(7, player.getInventory(), 0);
        player.containerMenu = editor;
        ejector.getItem().onDroppedByPlayer(ejector, player);
        helper.assertTrue(
                player.containerMenu == player.inventoryMenu,
                "Dropping the edited upgrade must close its editor");
        helper.succeed();
    }

    static void menu(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var upgrade = advanced(UpgradeItem.Kind.ADVANCED_EJECTOR, 1);
        player.getInventory().setItem(0, upgrade);
        var menu = new AdvancedUpgradeMenu(7, player.getInventory(), 0);
        player.containerMenu = menu;
        helper.assertTrue(
                !menu.meta() && !menu.energy() && !menu.nbtIndicator(),
                "A fresh advanced upgrade must show every flag off");
        helper.assertTrue(
                menu.clickMenuButton(player, AdvancedUpgradeMenu.META_TOGGLE)
                        && menu.meta()
                        && upgrade.get(ModDataComponents.ADVANCED_META).active(),
                "The meta toggle must flip the settings component");
        helper.assertTrue(
                menu.clickMenuButton(player, AdvancedUpgradeMenu.ENERGY_TOGGLE)
                        && menu.energy()
                        && upgrade.get(ModDataComponents.ADVANCED_ENERGY).active(),
                "The energy toggle must flip the settings component");
        helper.assertTrue(
                !menu.clickMenuButton(player, 42),
                "Unknown button ids must be rejected");
        // Ghost slot editing writes straight into the upgrade's filter component.
        menu.setCarried(new ItemStack(Items.DIAMOND, 5));
        menu.clicked(0, 0, ContainerInput.PICKUP, player);
        var stored =
                upgrade.getOrDefault(
                        ModDataComponents.ADVANCED_FILTER_ITEMS, ItemContainerContents.EMPTY);
        helper.assertTrue(
                stored.getSlots() > 0 && stored.getStackInSlot(0).is(Items.DIAMOND),
                "Placing a ghost entry must write the filter component");
        menu.setCarried(ItemStack.EMPTY);
        menu.clicked(0, 0, ContainerInput.PICKUP, player);
        var cleared =
                upgrade.getOrDefault(
                        ModDataComponents.ADVANCED_FILTER_ITEMS, ItemContainerContents.EMPTY);
        helper.assertTrue(
                cleared.getSlots() == 0 || cleared.getStackInSlot(0).isEmpty(),
                "Picking the ghost entry back up must clear the filter component");
        // Number-key swaps must not pull the open upgrade out of its hand.
        menu.clicked(-1, 0, ContainerInput.SWAP, player);
        helper.assertTrue(
                player.getInventory().getItem(0) == upgrade,
                "Hotbar swaps must not move the open upgrade");
        // Any interaction needs the same stack to remain in the bound hand slot.
        player.getInventory().setItem(0, new ItemStack(Items.STONE));
        helper.assertTrue(
                !menu.clickMenuButton(player, AdvancedUpgradeMenu.META_TOGGLE),
                "A foreign stack in the bound slot must invalidate the editor");
        player.getInventory().setItem(0, upgrade);
        // The dev-only config buttons open sub screens on real servers and are declined in
        // production, mirroring legacy Util.inDev() gating.
        helper.assertTrue(
                menu.clickMenuButton(player, AdvancedUpgradeMenu.META_CONFIG)
                        == !FMLLoader.getCurrent().isProduction(),
                "Config buttons must follow the dev gating");
        helper.succeed();
    }

    static void filter(GameTestHelper helper) {
        var level = helper.getLevel();
        var machine = machine(helper, new BlockPos(5, 1, 5));
        var chest = chest(helper, new BlockPos(4, 1, 5));
        // Furnace input slots only admit smeltable items, so the selectivity pair is drawn
        // from two smeltables: the filter must pick raw iron over raw copper.
        chest.setItem(0, new ItemStack(Items.RAW_IRON, 8));
        chest.setItem(1, new ItemStack(Items.RAW_COPPER, 8));
        var upgrade = advanced(UpgradeItem.Kind.ADVANCED_PULLING, 1);
        upgrade.set(
                ModDataComponents.ADVANCED_FILTER_ITEMS,
                ItemContainerContents.fromItems(List.of(new ItemStack(Items.RAW_IRON))));
        upgrade.set(ModDataComponents.UPGRADE_DIRECTION, Direction.WEST);
        machine.inventory().set(machine.kind().upgradeStart(), ItemResource.of(upgrade), 1);
        machine.serverTick(level);
        helper.assertTrue(
                machine.inventory().stack(0).is(Items.RAW_IRON),
                "The pull must bring the filtered item into the machine");
        helper.assertTrue(
                machine.inventory().stack(0).getCount() == 1,
                "A single upgrade must pull at the rate of one per tick");
        helper.assertTrue(
                chest.getItem(0).getCount() == 7,
                "The source chest must lose exactly the pulled item");
        helper.assertTrue(
                chest.getItem(1).getCount() == 8,
                "The unfiltered raw copper must stay in the chest");
        // Legacy: with no filters and no active energy flag the upgrade moves nothing at all.
        var inertMachine = machine(helper, new BlockPos(2, 1, 5));
        var inertChest = chest(helper, new BlockPos(1, 1, 5));
        inertChest.setItem(0, new ItemStack(Items.RAW_IRON, 8));
        var inert = advanced(UpgradeItem.Kind.ADVANCED_PULLING, 1);
        inert.set(ModDataComponents.UPGRADE_DIRECTION, Direction.WEST);
        inertMachine.inventory()
                .set(inertMachine.kind().upgradeStart(), ItemResource.of(inert), 1);
        inertMachine.serverTick(level);
        helper.assertTrue(
                inertMachine.inventory().stack(0).isEmpty()
                        && inertChest.getItem(0).getCount() == 8,
                "An unconfigured advanced upgrade must stay inert");
        helper.succeed();
    }

    static void energy(GameTestHelper helper) {
        var charged = charge(new ItemStack(ModItems.RE_BATTERY.get()), 1000);
        var empty = charge(new ItemStack(ModItems.RE_BATTERY.get()), 0);
        var filters = List.of(charge(new ItemStack(ModItems.RE_BATTERY.get()), 1000));
        // Legacy DIRECT + active with no filter entries is still inert: the final fallback
        // requires !checkEnergy.
        helper.assertTrue(
                !AdvancedUpgradeFilter.matches(
                        charged,
                        List.of(),
                        0,
                        AdvancedFilterSettings.DEFAULT.withActive(true)),
                "An active energy flag without filters must stay inert");
        helper.assertTrue(
                AdvancedUpgradeFilter.matches(
                        charged, filters, 0, AdvancedFilterSettings.DEFAULT.withActive(true))
                        && !AdvancedUpgradeFilter.matches(
                                empty, filters, 0, AdvancedFilterSettings.DEFAULT.withActive(true))
                        && !AdvancedUpgradeFilter.matches(
                                new ItemStack(Items.DIAMOND),
                                filters,
                                0,
                                AdvancedFilterSettings.DEFAULT.withActive(true)),
                "An active energy flag must only admit items at the filter's charge");
        var comparison =
                AdvancedFilterSettings.DEFAULT.withActive(true)
                        .withType(AdvancedFilterSettings.ComparisonType.COMPARISON)
                        .withNormalBound(500);
        helper.assertTrue(
                AdvancedUpgradeFilter.matches(charged, List.of(), 0, comparison)
                        && !AdvancedUpgradeFilter.matches(empty, List.of(), 0, comparison)
                        && !AdvancedUpgradeFilter.matches(
                                new ItemStack(Items.DIAMOND), List.of(), 0, comparison),
                "A charge threshold must gate every move on the stored charge");
        // Legacy default operators (LESS on both): normalBound < charge < extraBound.
        var range =
                AdvancedFilterSettings.DEFAULT.withActive(true)
                        .withType(AdvancedFilterSettings.ComparisonType.RANGE)
                        .withNormalBound(500)
                        .withExtraBound(1500);
        helper.assertTrue(
                AdvancedUpgradeFilter.matches(charged, List.of(), 0, range)
                        && !AdvancedUpgradeFilter.matches(
                                charge(new ItemStack(ModItems.RE_BATTERY.get()), 500),
                                List.of(),
                                0,
                                range)
                        && !AdvancedUpgradeFilter.matches(
                                charge(new ItemStack(ModItems.RE_BATTERY.get()), 1500),
                                List.of(),
                                0,
                                range)
                        && !AdvancedUpgradeFilter.matches(
                                charge(new ItemStack(ModItems.RE_BATTERY.get()), 2000),
                                List.of(),
                                0,
                                range)
                        && !AdvancedUpgradeFilter.matches(empty, List.of(), 0, range),
                "A charge range must admit only the window between the bounds");
        helper.assertTrue(
                !AdvancedUpgradeFilter.matches(
                        charged,
                        List.of(),
                        0,
                        comparison.withActive(false)),
                "A configured but inactive comparison must stay inert");
        // End to end: two furnaces eject through identical advanced upgrades whose filter
        // battery carries 1000 EU. The output slot has no recipe gate, so the batteries can be
        // placed there directly; only the same-charge battery may leave.
        var level = helper.getLevel();
        var chargedMachine = machine(helper, new BlockPos(5, 1, 5));
        var chargedChest = chest(helper, new BlockPos(6, 1, 5));
        var emptyMachine = machine(helper, new BlockPos(2, 1, 5));
        var emptyChest = chest(helper, new BlockPos(3, 1, 5));
        var upgrade = advanced(UpgradeItem.Kind.ADVANCED_EJECTOR, 1);
        upgrade.set(
                ModDataComponents.ADVANCED_FILTER_ITEMS,
                ItemContainerContents.fromItems(
                        List.of(charge(new ItemStack(ModItems.RE_BATTERY.get()), 1000))));
        upgrade.set(ModDataComponents.ADVANCED_ENERGY, AdvancedFilterSettings.DEFAULT.withActive(true));
        upgrade.set(ModDataComponents.UPGRADE_DIRECTION, Direction.EAST);
        chargedMachine.inventory()
                .set(chargedMachine.kind().upgradeStart(), ItemResource.of(upgrade), 1);
        chargedMachine.inventory().set(1, ItemResource.of(charged.copy()), 1);
        var emptyFilter = advanced(UpgradeItem.Kind.ADVANCED_EJECTOR, 1);
        emptyFilter.set(
                ModDataComponents.ADVANCED_FILTER_ITEMS,
                ItemContainerContents.fromItems(
                        List.of(charge(new ItemStack(ModItems.RE_BATTERY.get()), 1000))));
        emptyFilter.set(
                ModDataComponents.ADVANCED_ENERGY, AdvancedFilterSettings.DEFAULT.withActive(true));
        emptyFilter.set(ModDataComponents.UPGRADE_DIRECTION, Direction.EAST);
        emptyMachine.inventory()
                .set(emptyMachine.kind().upgradeStart(), ItemResource.of(emptyFilter), 1);
        emptyMachine.inventory().set(1, ItemResource.of(empty.copy()), 1);
        chargedMachine.serverTick(level);
        emptyMachine.serverTick(level);
        helper.assertTrue(
                chargedChest.getItem(0).getItem() == ModItems.RE_BATTERY.get()
                        && ElectricItemEnergy.charge(chargedChest.getItem(0)) == 1000.0,
                "The energy gate must eject the matching charged battery");
        helper.assertTrue(
                emptyChest.isEmpty() && ElectricItemEnergy.charge(emptyMachine.inventory().stack(1)) == 0.0,
                "The charge-mismatched battery must stay in the machine");
        helper.succeed();
    }

    static void valueConfig(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var upgrade = advanced(UpgradeItem.Kind.ADVANCED_EJECTOR, 1);
        player.getInventory().setItem(0, upgrade);
        var menu = new AdvancedValueConfigMenu(8, player.getInventory(), 0, AdvancedMenus.TAG_META);
        player.containerMenu = menu;
        helper.assertTrue(
                menu.type() == AdvancedFilterSettings.ComparisonType.DIRECT.ordinal()
                        && menu.normalBound() == 0
                        && menu.extraBound() == 0,
                "The config editor must open on the legacy defaults");
        menu.clickMenuButton(player, AdvancedValueConfigMenu.TYPE_CYCLE);
        var settings = upgrade.get(ModDataComponents.ADVANCED_META);
        helper.assertTrue(
                settings.comparisonType() == AdvancedFilterSettings.ComparisonType.COMPARISON
                        && settings.active(),
                "Cycling the type must enable the comparison");
        // Legacy linkage: the extra bound stays inside the normal bound's direction class.
        menu.clickMenuButton(player, AdvancedValueConfigMenu.NORMAL_OP_CYCLE);
        settings = upgrade.get(ModDataComponents.ADVANCED_META);
        helper.assertTrue(
                settings.normalSetting() == AdvancedFilterSettings.ComparisonSetting.GREATER
                        && settings.extraSetting()
                                == AdvancedFilterSettings.ComparisonSetting.GREATER,
                "Cycling the operator must drag the extra bound into the same class");
        // Steppers: +1 twice, +10 once; the extra side -1 once and -10 down to clamp.
        menu.clickMenuButton(player, AdvancedValueConfigMenu.NORMAL_UP);
        menu.clickMenuButton(player, AdvancedValueConfigMenu.NORMAL_UP);
        menu.clickMenuButton(player, AdvancedValueConfigMenu.NORMAL_UP_10);
        menu.clickMenuButton(player, AdvancedValueConfigMenu.EXTRA_DOWN_10);
        menu.clickMenuButton(player, AdvancedValueConfigMenu.EXTRA_DOWN);
        menu.clickMenuButton(player, AdvancedValueConfigMenu.EXTRA_DOWN_10);
        menu.clickMenuButton(player, AdvancedValueConfigMenu.EXTRA_OP_TOGGLE);
        settings = upgrade.get(ModDataComponents.ADVANCED_META);
        helper.assertTrue(
                menu.normalBound() == 12
                        && settings.normalBound() == 12
                        && settings.extraBound() == 0
                        && settings.extraSetting()
                                == AdvancedFilterSettings.ComparisonSetting.GREATER_OR_EQUAL,
                "Steppers and toggles must write clamped bounds back to the component");
        // The back button is accepted, and (on real servers) returns to the main editor.
        helper.assertTrue(
                menu.clickMenuButton(player, AdvancedValueConfigMenu.BACK),
                "The back button must be accepted");
        // The energy tag edits the other component.
        var energyMenu =
                new AdvancedValueConfigMenu(9, player.getInventory(), 0, AdvancedMenus.TAG_ENERGY);
        player.containerMenu = energyMenu;
        energyMenu.clickMenuButton(player, AdvancedValueConfigMenu.TYPE_CYCLE);
        helper.assertTrue(
                upgrade.get(ModDataComponents.ADVANCED_ENERGY).comparisonType()
                                == AdvancedFilterSettings.ComparisonType.COMPARISON
                        && upgrade.get(ModDataComponents.ADVANCED_META).comparisonType()
                                == AdvancedFilterSettings.ComparisonType.COMPARISON,
                "The energy editor must target the energy component");
        helper.succeed();
    }

    private static ItemStack advanced(UpgradeItem.Kind kind, int count) {
        return ModUpgrades.ALL.get(kind).toStack(count);
    }

    private static ItemStack charge(ItemStack stack, double amount) {
        stack.set(ModDataComponents.CHARGE, amount);
        return stack;
    }

    private static UpgradeableBlockEntity machine(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, ModMachines.block(MachineKind.ELECTRIC_FURNACE));
        return (UpgradeableBlockEntity) helper.getBlockEntity(pos, UpgradeableBlockEntity.class);
    }

    private static ChestBlockEntity chest(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, Blocks.CHEST.defaultBlockState());
        return (ChestBlockEntity) helper.getBlockEntity(pos, ChestBlockEntity.class);
    }

    private AdvancedUpgradeTests() {}
}
