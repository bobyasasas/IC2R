package ic2.neoforge.test;

import ic2.neoforge.component.ModDataComponents;
import ic2.neoforge.item.UpgradeItem;
import ic2.neoforge.machine.CannerBlockEntity;
import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.machine.UpgradeableBlockEntity;
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
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

final class UpgradeTests {
    static void ratesAndPersistence(GameTestHelper helper) {
        var machine = machine(helper, MachineKind.ELECTRIC_FURNACE, new BlockPos(5, 1, 5));
        machine.inventory().set(0, ItemResource.of(Items.RAW_IRON), 8);
        machine.energy().insert(600);
        for (int tick = 0; tick < 50; tick++) machine.serverTick(helper.getLevel());
        upgrade(machine, 0, UpgradeItem.Kind.OVERCLOCKER, 1, null);
        upgrade(machine, 1, UpgradeItem.Kind.ENERGY_STORAGE, 1, null);
        helper.assertTrue(
                machine.upgradeProfile().ticks() == 70 && machine.progress() == 35,
                "Installing an upgrade must preserve fractional progress");
        machine.energy().insert(10000);
        var restored =
                (UpgradeableBlockEntity)
                        BlockEntity.loadStatic(
                                machine.getBlockPos(),
                                machine.getBlockState(),
                                machine.saveWithFullMetadata(helper.getLevel().registryAccess()),
                                helper.getLevel().registryAccess());
        helper.assertTrue(
                restored != null
                        && restored.energy().stored() == machine.energy().stored()
                        && restored.energy().stored() > 600,
                "Upgrade capacity must be restored before loading stored EU");
        helper.assertTrue(
                restored.progress() == 35 && restored.progressMaximum() == 70,
                "Reload must preserve upgraded progress");
        for (int tick = 0; tick < 35; tick++) machine.serverTick(helper.getLevel());
        helper.assertTrue(
                machine.inventory().stack(1).is(Items.IRON_INGOT)
                        && machine.inventory().stack(1).getCount() == 1,
                "Upgraded furnace must complete at the new duration");
        machine.inventory().set(machine.kind().upgradeStart() + 1, ItemResource.EMPTY, 0);
        helper.assertTrue(
                machine.upgradeProfile().capacity() == 650 && machine.energy().stored() == 650,
                "Removing storage must clamp surplus energy");
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var menu = new MachineMenu(11, player.getInventory(), machine);
        helper.assertTrue(
                menu.slots.size() == 43 && menu.capacity() == 650,
                "Menu must include four upgrade slots and dynamic capacity");
        helper.assertTrue(
                !menu.slots.get(3).mayPlace(new ItemStack(Items.DIAMOND)),
                "Upgrade slots must reject ordinary items");
        try (var tx = Transaction.openRoot()) {
            helper.assertTrue(
                    machine.automation(Direction.UP)
                                    .insert(
                                            3,
                                            ItemResource.of(
                                                    ModUpgrades.ALL
                                                            .get(UpgradeItem.Kind.OVERCLOCKER)
                                                            .get()),
                                            1,
                                            tx)
                            == 0,
                    "Automation must never modify upgrade slots");
        }
        helper.succeed();
    }

    static void batch(GameTestHelper helper) {
        var machine = machine(helper, MachineKind.ELECTRIC_FURNACE, new BlockPos(5, 1, 5));
        upgrade(machine, 0, UpgradeItem.Kind.OVERCLOCKER, 16, null);
        var profile = machine.upgradeProfile();
        machine.inventory().set(0, ItemResource.of(Items.RAW_IRON), 64);
        machine.energy().insert(profile.capacity());
        double before = machine.energy().stored();
        machine.serverTick(helper.getLevel());
        helper.assertTrue(
                profile.ticks() == 1
                        && profile.operations() == 4
                        && machine.inventory().stack(1).getCount() == 4
                        && machine.inventory().stack(0).getCount() == 60,
                "Heavy overclocking must produce the legacy four-item batch");
        helper.assertTrue(
                before - machine.energy().stored() == profile.euPerTick(),
                "A batch consumes the configured EU once per tick");
        machine.inventory().set(1, ItemResource.of(Items.IRON_INGOT), 64);
        before = machine.energy().stored();
        machine.serverTick(helper.getLevel());
        helper.assertTrue(
                machine.energy().stored() == before
                        && machine.inventory().stack(0).getCount() == 60,
                "Full output must pause the batch without consuming input or EU");
        helper.succeed();
    }

    static void transfers(GameTestHelper helper) {
        var level = helper.getLevel();
        var machine = machine(helper, MachineKind.ELECTRIC_FURNACE, new BlockPos(5, 1, 5));
        var east = machine.getBlockPos().east();
        var west = machine.getBlockPos().west();
        level.setBlockAndUpdate(east, Blocks.CHEST.defaultBlockState());
        level.setBlockAndUpdate(west, Blocks.CHEST.defaultBlockState());
        var output = (ChestBlockEntity) level.getBlockEntity(east);
        var input = (ChestBlockEntity) level.getBlockEntity(west);
        input.setItem(0, new ItemStack(Items.DIAMOND, 8));
        input.setItem(1, new ItemStack(Items.RAW_IRON, 8));
        machine.inventory().set(1, ItemResource.of(Items.IRON_INGOT), 3);
        upgrade(machine, 0, UpgradeItem.Kind.EJECTOR, 1, Direction.EAST);
        upgrade(machine, 1, UpgradeItem.Kind.PULLING, 2, Direction.WEST);
        machine.serverTick(level);
        helper.assertTrue(
                output.getItem(0).getCount() == 1
                        && input.getItem(1).getCount() == 4
                        && input.getItem(0).getCount() == 8
                        && machine.inventory().stack(0).getCount() == 4
                        && machine.inventory().stack(1).getCount() == 2,
                "Configured sides and stack-dependent rates must govern transfer");
        for (int slot = 0; slot < output.getContainerSize(); slot++)
            output.setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
        machine.serverTick(level);
        helper.assertTrue(
                machine.inventory().stack(1).getCount() == 2,
                "Full destination must not lose output");
        var saved = machine.saveWithFullMetadata(level.registryAccess());
        var restored =
                (UpgradeableBlockEntity)
                        BlockEntity.loadStatic(
                                machine.getBlockPos(),
                                machine.getBlockState(),
                                saved,
                                level.registryAccess());
        helper.assertTrue(
                UpgradeItem.direction(restored.inventory().stack(3)) == Direction.EAST,
                "Upgrade side component must survive machine persistence");
        helper.succeed();
    }

    static void fluids(GameTestHelper helper) {
        var source = (CannerBlockEntity) machine(helper, MachineKind.CANNER, new BlockPos(5, 1, 5));
        var destination =
                (CannerBlockEntity) machine(helper, MachineKind.CANNER, new BlockPos(6, 1, 5));
        var water = FluidResource.of(Fluids.WATER);
        source.outputTank().set(0, water, 1000);
        destination.inputTank().set(0, water, 7950);
        upgrade(source, 0, UpgradeItem.Kind.FLUID_EJECTOR, 2, Direction.EAST);
        source.serverTick(helper.getLevel());
        helper.assertTrue(
                source.outputTank().getAmountAsInt(0) == 950
                        && destination.inputTank().getAmountAsInt(0) == 8000,
                "Fluid upgrade must move only available capacity atomically");
        source.serverTick(helper.getLevel());
        helper.assertTrue(
                source.outputTank().getAmountAsInt(0) == 950,
                "Full tank must preserve remaining fluid");
        helper.succeed();
    }

    private static UpgradeableBlockEntity machine(
            GameTestHelper helper, MachineKind kind, BlockPos pos) {
        helper.setBlock(pos, ModMachines.block(kind));
        return (UpgradeableBlockEntity) helper.getBlockEntity(pos, UpgradeableBlockEntity.class);
    }

    private static void upgrade(
            UpgradeableBlockEntity machine,
            int slot,
            UpgradeItem.Kind kind,
            int count,
            Direction side) {
        var stack = ModUpgrades.ALL.get(kind).toStack(count);
        if (side != null) stack.set(ModDataComponents.UPGRADE_DIRECTION, side);
        machine.inventory()
                .set(machine.kind().upgradeStart() + slot, ItemResource.of(stack), count);
    }

    private UpgradeTests() {}
}
