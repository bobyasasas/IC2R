package ic2.neoforge.test;

import ic2.neoforge.component.ModDataComponents;
import ic2.neoforge.item.UpgradeItem;
import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.machine.TankBlockEntity;
import ic2.neoforge.menu.MachineMenu;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.registration.ModUpgrades;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

final class TankTests {
    private static final BlockPos POSITION = new BlockPos(2, 1, 2);
    private static final FluidResource WATER = FluidResource.of(Fluids.WATER);

    static void storageAndComparator(GameTestHelper helper) {
        var tank = machine(helper, POSITION);
        var state = tank.getBlockState();
        helper.assertTrue(
                state.getAnalogOutputSignal(helper.getLevel(), tank.getBlockPos(), Direction.NORTH)
                        == 0,
                "Empty tanks must have comparator signal zero");
        try (var transaction = Transaction.openRoot()) {
            tank.tank().insert(0, WATER, 12000, transaction);
            helper.assertTrue(
                    tank.comparator() == 8, "Half-full tanks emit comparator signal eight");
        }
        helper.assertTrue(
                tank.tank().getAmountAsInt(0) == 0 && tank.comparator() == 0,
                "Aborted fills must restore contents and comparator value");
        try (var transaction = Transaction.openRoot()) {
            helper.assertTrue(
                    tank.fluidAutomation(Direction.UP).insert(0, WATER, 30000, transaction)
                            == 24000,
                    "The native tank caps fills at 24 buckets");
            helper.assertTrue(
                    tank.tank().insert(0, FluidResource.of(Fluids.LAVA), 1, transaction) == 0,
                    "Different fluids cannot mix");
            transaction.commit();
        }
        var restored =
                (TankBlockEntity)
                        BlockEntity.loadStatic(
                                tank.getBlockPos(),
                                state,
                                tank.saveWithFullMetadata(helper.getLevel().registryAccess()),
                                helper.getLevel().registryAccess());
        restored.setLevel(helper.getLevel());
        helper.assertTrue(
                restored.tank().getAmountAsInt(0) == 24000 && restored.comparator() == 15,
                "Full contents and comparator survive native reload");
        for (var side : Direction.values())
            try (var transaction = Transaction.openRoot()) {
                helper.assertTrue(
                        restored.fluidAutomation(side).extract(0, WATER, 1000, transaction) == 1000,
                        "Every tank face is bidirectional");
            }
        helper.succeed();
    }

    static void cursorAndPermissions(GameTestHelper helper) {
        var tank = machine(helper, POSITION);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(
                tank.getBlockPos().getX() + .5,
                tank.getBlockPos().getY(),
                tank.getBlockPos().getZ() + .5);
        var menu = new MachineMenu(1, player.getInventory(), tank);
        player.containerMenu = menu;
        menu.setCarried(new ItemStack(Items.WATER_BUCKET));
        helper.assertTrue(
                menu.clickMenuButton(player, 0)
                        && tank.tank().getAmountAsInt(0) == 1000
                        && menu.getCarried().is(Items.BUCKET),
                "Clicking the tank with a filled cursor bucket must atomically drain it");
        try (var transaction = Transaction.openRoot()) {
            tank.tank().insert(0, WATER, 2000, transaction);
            transaction.commit();
        }
        menu.setCarried(new ItemStack(Items.BUCKET, 3));
        helper.assertTrue(
                menu.clickMenuButton(player, 1) && tank.tank().getAmountAsInt(0) == 0,
                "Shift-click fills exactly the original cursor stack without draining replacements"
                    + " back");
        int buckets = menu.getCarried().is(Items.WATER_BUCKET) ? menu.getCarried().getCount() : 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            var stack = player.getInventory().getItem(slot);
            if (stack.is(Items.WATER_BUCKET)) buckets += stack.getCount();
        }
        helper.assertTrue(
                buckets == 3, "All three filled buckets must remain on the cursor or in inventory");
        menu.setCarried(new ItemStack(Items.WATER_BUCKET));
        helper.assertTrue(
                !menu.clickMenuButton(player, 2) && tank.tank().getAmountAsInt(0) == 0,
                "Unknown menu action IDs must be rejected");
        player.setPos(
                tank.getBlockPos().getX() + 100,
                tank.getBlockPos().getY(),
                tank.getBlockPos().getZ());
        helper.assertTrue(
                !menu.clickMenuButton(player, 0), "Distant cursor requests must be rejected");
        player.setPos(
                tank.getBlockPos().getX(), tank.getBlockPos().getY(), tank.getBlockPos().getZ());
        player.containerMenu = player.inventoryMenu;
        helper.assertTrue(!menu.clickMenuButton(player, 0), "A closed menu cannot transfer fluids");
        player.containerMenu = menu;
        helper.setBlock(POSITION, Blocks.AIR);
        helper.assertTrue(
                !menu.clickMenuButton(player, 0) && menu.getCarried().is(Items.WATER_BUCKET),
                "A removed tank cannot consume cursor items");
        helper.succeed();
    }

    static void partialBucketAndUpgrade(GameTestHelper helper) {
        var tank = machine(helper, POSITION);
        try (var transaction = Transaction.openRoot()) {
            tank.tank().insert(0, WATER, 23500, transaction);
            transaction.commit();
        }
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(
                tank.getBlockPos().getX(), tank.getBlockPos().getY(), tank.getBlockPos().getZ());
        var menu = new MachineMenu(1, player.getInventory(), tank);
        player.containerMenu = menu;
        menu.setCarried(new ItemStack(Items.WATER_BUCKET));
        helper.assertTrue(
                !menu.clickMenuButton(player, 0)
                        && tank.tank().getAmountAsInt(0) == 23500
                        && menu.getCarried().is(Items.WATER_BUCKET),
                "A bucket must remain intact when less than one bucket of space remains");
        var target = machine(helper, POSITION.east());
        var upgrade =
                ModUpgrades.ALL.get(UpgradeItem.Kind.FLUID_EJECTOR).get().getDefaultInstance();
        upgrade.set(ModDataComponents.UPGRADE_DIRECTION, Direction.EAST);
        tank.inventory().set(0, ItemResource.of(upgrade), 1);
        tank.serverTick(helper.getLevel());
        helper.assertTrue(
                tank.tank().getAmountAsInt(0) == 23450 && target.tank().getAmountAsInt(0) == 50,
                "A single fluid ejector moves fifty mB without changing total stored fluid");
        helper.assertTrue(
                menu.getSlot(0).mayPlace(upgrade)
                        && !menu.getSlot(0)
                                .mayPlace(
                                        ModUpgrades.ALL
                                                .get(UpgradeItem.Kind.EJECTOR)
                                                .get()
                                                .getDefaultInstance()),
                "Tanks accept fluid upgrades and reject item upgrades");
        helper.succeed();
    }

    private static TankBlockEntity machine(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, ModMachines.block(MachineKind.TANK));
        return helper.getBlockEntity(pos, TankBlockEntity.class);
    }

    private TankTests() {}
}
