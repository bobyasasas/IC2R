package ic2.neoforge.test;

import ic2.neoforge.item.ElectricItemEnergy;
import ic2.neoforge.machine.ChargepadBlockEntity;
import ic2.neoforge.machine.EnergyStorageBlockEntity;
import ic2.neoforge.machine.MachineBlock;
import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.menu.MachineMenu;
import ic2.neoforge.registration.ModItems;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.registration.ModTools;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.transfer.item.ItemResource;

final class ChargepadTests {
    private static final BlockPos POSITION = new BlockPos(2, 1, 2);

    static void chargesPlayerInventory(GameTestHelper helper) {
        var pad = pad(helper);
        pad.energy().insert(1000);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var battery = new ItemStack(ModItems.RE_BATTERY.get());
        ElectricItemEnergy.discharge(battery, 100, 1, false, false, false);
        player.getInventory().setItem(0, battery);
        helper.assertTrue(
                ElectricItemEnergy.charge(player.getInventory().getItem(0)) == 0,
                "Test starts from a drained battery");
        helper.assertTrue(
                pad.chargeInventory(player), "A drained battery on the pad reports charging");
        helper.assertValueEqual(
                ElectricItemEnergy.charge(player.getInventory().getItem(0)),
                1000.0,
                "One cycle pushes the stored amount, ignoring the per-operation cap like the"
                        + " recovered pad");
        helper.assertValueEqual(pad.storedEnergy(), 0.0, "Charging drains the pad storage");
        helper.succeed();
    }

    static void chargeOrderAndLimits(GameTestHelper helper) {
        var pad = pad(helper);
        pad.energy().insert(1000);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var first = new ItemStack(ModItems.RE_BATTERY.get());
        var second = new ItemStack(ModItems.RE_BATTERY.get());
        ElectricItemEnergy.discharge(first, 100, 1, false, false, false);
        ElectricItemEnergy.discharge(second, 100, 1, false, false, false);
        player.getInventory().setItem(1, second);
        player.getInventory().setItem(0, first);
        pad.chargeInventory(player);
        double mainHand = ElectricItemEnergy.charge(player.getInventory().getItem(0));
        double hotbar = ElectricItemEnergy.charge(player.getInventory().getItem(1));
        helper.assertTrue(
                mainHand > 0 && hotbar == 0,
                "One cycle charges only the first item, starting with the main hand");
        pad.energy().extract(1000);
        pad.energy().insert(5);
        var hungry = new ItemStack(ModItems.RE_BATTERY.get());
        ElectricItemEnergy.discharge(hungry, 100, 1, false, false, false);
        player.getInventory().setItem(1, new ItemStack(net.minecraft.world.item.Items.STONE));
        player.getInventory().setItem(0, hungry);
        boolean charged = pad.chargeInventory(player);
        helper.assertTrue(
                charged && ElectricItemEnergy.charge(player.getInventory().getItem(0)) == 5.0,
                "A nearly empty pad transfers only what it stores");
        helper.succeed();
    }

    static void networkFeeding(GameTestHelper helper) {
        var pad = pad(helper);
        helper.setBlock(
                POSITION.east(),
                ModMachines.block(MachineKind.MFSU)
                        .defaultBlockState()
                        .setValue(MachineBlock.FACING, Direction.WEST));
        var source = helper.getBlockEntity(POSITION.east(), EnergyStorageBlockEntity.class);
        helper.assertTrue(source != null, "Test neighbour is a storage block");
        source.energy().insert(5000);
        helper.runAfterDelay(
                60,
                () -> {
                    helper.assertTrue(
                            pad.storedEnergy() > 0,
                            "The pad draws network power through its non-output faces");
                    helper.succeed();
                });
    }

    /** Legacy two-state pad output: emit while charging, or invert to emit while idle. */
    static void redstoneModes(GameTestHelper helper) {
        var pad = padOf(helper, MachineKind.BATBOX_CHARGEPAD, Direction.NORTH);
        tickPad(helper, pad);
        helper.assertTrue(pad.signal() == 0, "An idle pad in default mode emits no redstone");
        pad.setRedstoneMode(1);
        helper.assertTrue(pad.signal() == 15, "An idle pad must emit redstone in inverted mode");
        helper.setBlock(
                POSITION, helper.getBlockState(POSITION).setValue(MachineBlock.ACTIVE, true));
        pad.setRedstoneMode(1);
        helper.assertTrue(pad.signal() == 0, "Inverted mode must silence a charging pad");
        pad.setRedstoneMode(0);
        helper.assertTrue(pad.signal() == 15, "A charging pad must emit redstone by default");
        var restored =
                (ChargepadBlockEntity)
                        BlockEntity.loadStatic(
                                pad.getBlockPos(),
                                pad.getBlockState(),
                                pad.saveWithFullMetadata(helper.getLevel().registryAccess()),
                                helper.getLevel().registryAccess());
        helper.assertTrue(
                restored.redstoneMode() == 0,
                "The pad redstone mode must survive a save/load round trip");
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(
                pad.getBlockPos().getX() + .5,
                pad.getBlockPos().getY(),
                pad.getBlockPos().getZ() + .5);
        var menu = new MachineMenu(1, player.getInventory(), pad);
        player.containerMenu = menu;
        helper.assertTrue(
                menu.clickMenuButton(player, 1)
                        && pad.redstoneMode() == 1
                        && menu.clickMenuButton(player, 0)
                        && pad.redstoneMode() == 0
                        && !menu.clickMenuButton(player, 2),
                "The pad menu must cycle exactly two redstone modes");
        helper.succeed();
    }

    /** The marked output face still feeds neighbours like the storage blocks pads derive from. */
    static void feedsMarkedFace(GameTestHelper helper) {
        var pad = padOf(helper, MachineKind.BATBOX_CHARGEPAD, Direction.EAST);
        pad.energy().insert(1000);
        var sinkPos = POSITION.east();
        helper.setBlock(
                sinkPos,
                ModMachines.block(MachineKind.BATBOX)
                        .defaultBlockState()
                        .setValue(MachineBlock.FACING, Direction.EAST));
        helper.runAtTickTime(
                20,
                () -> {
                    var sink = helper.getBlockEntity(sinkPos, EnergyStorageBlockEntity.class);
                    helper.assertTrue(
                            sink.storedEnergy() > 0 && pad.storedEnergy() < 1000,
                            "The pad must feed its marked output face");
                    helper.succeed();
                });
    }

    /** Legacy sink directions skip the top face: EU cannot enter through a pad's top. */
    static void rejectsTopFeed(GameTestHelper helper) {
        var pad = padOf(helper, MachineKind.BATBOX_CHARGEPAD, Direction.NORTH);
        var topPos = POSITION.above();
        var westPos = POSITION.west();
        helper.setBlock(
                topPos,
                ModMachines.block(MachineKind.BATBOX)
                        .defaultBlockState()
                        .setValue(MachineBlock.FACING, Direction.DOWN));
        helper.setBlock(
                westPos,
                ModMachines.block(MachineKind.BATBOX)
                        .defaultBlockState()
                        .setValue(MachineBlock.FACING, Direction.EAST));
        var top = helper.getBlockEntity(topPos, EnergyStorageBlockEntity.class);
        var west = helper.getBlockEntity(westPos, EnergyStorageBlockEntity.class);
        top.energy().insert(1000);
        west.energy().insert(1000);
        helper.runAtTickTime(
                20,
                () -> {
                    helper.assertTrue(
                            top.storedEnergy() == 1000 && pad.storedEnergy() > 0,
                            "The pad must reject top-fed EU while accepting side EU");
                    helper.succeed();
                });
    }

    /** Legacy rule: the debug item is never a charging target for pads. */
    static void skipsDebugItem(GameTestHelper helper) {
        var pad = padOf(helper, MachineKind.BATBOX_CHARGEPAD, Direction.NORTH);
        pad.energy().insert(1000);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getInventory().setItem(0, new ItemStack(ModTools.DEBUG_ITEM.get()));
        helper.assertTrue(
                !pad.chargeInventory(player),
                "The pad must not report charging a debug item");
        helper.assertTrue(
                pad.storedEnergy() == 1000.0,
                "Charging a debug item must not drain the pad");
        helper.succeed();
    }

    /** Legacy ContainerChargepadBlock: the pad charges and drains its own item slots. */
    static void itemSlotsChargeAndDischarge(GameTestHelper helper) {
        var pad = pad(helper);
        pad.energy().insert(1000);
        var battery = new ItemStack(ModItems.RE_BATTERY.get());
        pad.inventory().set(ChargepadBlockEntity.CHARGE, ItemResource.of(battery), 1);
        pad.serverTick(helper.getLevel());
        double charged =
                ElectricItemEnergy.charge(pad.inventory().stack(ChargepadBlockEntity.CHARGE));
        helper.assertTrue(
                charged > 0 && pad.storedEnergy() == 1000.0 - charged,
                "The pad must charge a battery in its charge slot from its storage");
        var filled = pad.inventory().stack(ChargepadBlockEntity.CHARGE);
        pad.inventory()
                .set(ChargepadBlockEntity.DISCHARGE, ItemResource.of(filled), filled.getCount());
        pad.inventory().set(ChargepadBlockEntity.CHARGE, ItemResource.EMPTY, 0);
        pad.serverTick(helper.getLevel());
        double remaining =
                ElectricItemEnergy.charge(pad.inventory().stack(ChargepadBlockEntity.DISCHARGE));
        helper.assertTrue(
                remaining < charged && pad.storedEnergy() == 1000.0 - remaining,
                "The pad must drain a battery in its discharge slot back into storage");
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var menu = new MachineMenu(1, player.getInventory(), pad);
        helper.assertTrue(
                menu.slots.size() == 38
                        && menu.slots.get(0).mayPlace(new ItemStack(ModItems.RE_BATTERY.get()))
                        && !menu.slots.get(0)
                                .mayPlace(new ItemStack(net.minecraft.world.item.Items.STONE)),
                "The pad menu must expose the two legacy battery slots");
        helper.succeed();
    }

    /** The pad ticks every second tick; drive two ticks so the cycle lands on the active one. */
    private static void tickPad(GameTestHelper helper, ChargepadBlockEntity pad) {
        pad.serverTick(helper.getLevel());
        pad.serverTick(helper.getLevel());
    }

    private static ChargepadBlockEntity pad(GameTestHelper helper) {
        helper.setBlock(POSITION, ModMachines.block(MachineKind.MFE_CHARGEPAD));
        return helper.getBlockEntity(POSITION, ChargepadBlockEntity.class);
    }

    private static ChargepadBlockEntity padOf(GameTestHelper helper, MachineKind kind, Direction facing) {
        helper.setBlock(
                POSITION, ModMachines.block(kind).defaultBlockState().setValue(MachineBlock.FACING, facing));
        return helper.getBlockEntity(POSITION, ChargepadBlockEntity.class);
    }
}
