package ic2.neoforge.test;

import ic2.neoforge.item.ElectricItemEnergy;
import ic2.neoforge.machine.ChargepadBlockEntity;
import ic2.neoforge.machine.EnergyStorageBlockEntity;
import ic2.neoforge.machine.MachineBlock;
import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.registration.ModItems;
import ic2.neoforge.registration.ModMachines;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

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

    private static ChargepadBlockEntity pad(GameTestHelper helper) {
        helper.setBlock(POSITION, ModMachines.block(MachineKind.MFE_CHARGEPAD));
        return helper.getBlockEntity(POSITION, ChargepadBlockEntity.class);
    }
}
