package ic2.neoforge.test;

import com.mojang.serialization.JsonOps;

import ic2.neoforge.component.ModDataComponents;
import ic2.neoforge.fluid.FluidDefinition;
import ic2.neoforge.item.UpgradeItem;
import ic2.neoforge.machine.*;
import ic2.neoforge.menu.MachineMenu;
import ic2.neoforge.registration.*;

import io.netty.buffer.Unpooled;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

final class CondenserTests {
    private static final BlockPos POSITION = new BlockPos(2, 1, 2);

    static void passiveReload(GameTestHelper helper) {
        var machine = machine(helper);
        fill(machine.inputTank(), fluid(FluidDefinition.STEAM), 10000);
        for (int tick = 0; tick < 100; tick++) machine.serverTick(helper.getLevel());
        helper.assertTrue(
                machine.progress() == 10000
                        && machine.outputTank().getAmountAsInt(0) == 0
                        && machine.storedEnergy() == 0,
                "Passive steam is prepaid before the next water batch");
        var restored =
                (CondenserBlockEntity)
                        BlockEntity.loadStatic(
                                machine.getBlockPos(),
                                machine.getBlockState(),
                                machine.saveWithFullMetadata(helper.getLevel().registryAccess()),
                                helper.getLevel().registryAccess());
        helper.getLevel().removeBlockEntity(machine.getBlockPos());
        helper.getLevel().setBlockEntity(restored);
        restored.serverTick(helper.getLevel());
        helper.assertTrue(
                restored.progress() == 0 && restored.outputTank().getAmountAsInt(0) == 100,
                "Paid credit survives real block entity replacement and needs neither steam nor"
                    + " power to finish");
        fill(restored.inputTank(), fluid(FluidDefinition.SUPERHEATED_STEAM), 10000);
        for (int tick = 0; tick < 101; tick++) restored.serverTick(helper.getLevel());
        helper.assertTrue(
                restored.outputTank().getAmountAsInt(0) == 200 && restored.progress() == 0,
                "Both steam families conserve the original 100-to-1 ratio");
        helper.succeed();
    }

    static void nativePowerChain(GameTestHelper helper) {
        var machine = machine(helper);
        for (int slot = 3; slot < 7; slot++)
            machine.inventory().set(slot, ItemResource.of(ModReactorItems.HEAT_VENT.get()), 1);
        fill(machine.inputTank(), fluid(FluidDefinition.STEAM), 10000);
        helper.setBlock(
                POSITION.west(),
                ModMachines.block(MachineKind.MFE)
                        .defaultBlockState()
                        .setValue(MachineBlock.FACING, Direction.EAST));
        var source = helper.getBlockEntity(POSITION.west(), EnergyStorageBlockEntity.class);
        source.energy().insert(1024);
        helper.runAfterDelay(
                50,
                () -> {
                    helper.assertTrue(
                            machine.outputTank().getAmountAsInt(0) == 100
                                    && machine.progress() == 0,
                            "MFE powers the complete native condenser tick chain");
                    helper.assertValueEqual(
                            source.storedEnergy() + machine.storedEnergy(),
                            864.0,
                            "Native HV packets conserve energy after twenty eight-EU cooling"
                                + " operations");
                    helper.succeed();
                });
    }

    static void ventsAndPower(GameTestHelper helper) {
        var machine = machine(helper);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var menu = new MachineMenu(1, player.getInventory(), machine);
        for (int i = 0; i < 4; i++) {
            player.getInventory().setItem(9, new ItemStack(ModReactorItems.HEAT_VENT.get()));
            menu.quickMoveStack(player, 8);
        }
        helper.assertValueEqual(
                machine.vents(), 4, "Shift installation reaches every single-part slot");
        fill(machine.inputTank(), fluid(FluidDefinition.STEAM), 1000);
        machine.energy().insert(8);
        machine.serverTick(helper.getLevel());
        helper.assertTrue(
                machine.progress() == 500 && machine.storedEnergy() == 0,
                "Four vents consume eight EU for at most 500 steam mB");
        machine.serverTick(helper.getLevel());
        helper.assertValueEqual(
                machine.progress(), 500, "Installed unpowered vents pause even passive cooling");
        for (int slot = 3; slot < 7; slot++) machine.inventory().set(slot, ItemResource.EMPTY, 0);
        machine.serverTick(helper.getLevel());
        helper.assertValueEqual(
                machine.progress(),
                600,
                "Removing vents resumes passive cooling without losing credit");
        try (var transaction = Transaction.openRoot()) {
            helper.assertValueEqual(
                    machine.inventory().insert(3, ItemResource.of(Items.DIAMOND), 1, transaction),
                    0,
                    "Installed slots reject unrelated items in the backend");
            helper.assertValueEqual(
                    machine.inventory()
                            .insert(
                                    7,
                                    ItemResource.of(
                                            ModUpgrades.ALL
                                                    .get(UpgradeItem.Kind.OVERCLOCKER)
                                                    .get()),
                                    1,
                                    transaction),
                    0,
                    "Condenser rejects speed upgrades");
        }
        machine.inventory()
                .set(
                        7,
                        ItemResource.of(ModUpgrades.ALL.get(UpgradeItem.Kind.TRANSFORMER).get()),
                        2);
        helper.assertValueEqual(
                machine.energyNode().input().orElseThrow().voltage(),
                8192,
                "Two transformer upgrades raise HV to IV");
        machine.inventory().set(7, ItemResource.EMPTY, 0);
        helper.assertValueEqual(
                machine.energyNode().input().orElseThrow().voltage(),
                512,
                "Removing transformers restores HV");
        helper.succeed();
    }

    static void blockedOutput(GameTestHelper helper) {
        var machine = machine(helper);
        fill(machine.inputTank(), fluid(FluidDefinition.STEAM), 10100);
        for (int tick = 0; tick < 100; tick++) machine.serverTick(helper.getLevel());
        fill(machine.outputTank(), fluid(FluidDefinition.DISTILLED_WATER), 901);
        machine.serverTick(helper.getLevel());
        helper.assertTrue(
                machine.progress() == 10000 && machine.inputTank().getAmountAsInt(0) == 100,
                "Less than a full water batch of space must preserve steam and credit");
        try (var transaction = Transaction.openRoot()) {
            machine.outputTank().extract(0, fluid(FluidDefinition.DISTILLED_WATER), 1, transaction);
            transaction.commit();
        }
        machine.serverTick(helper.getLevel());
        helper.assertTrue(
                machine.outputTank().getAmountAsInt(0) == 1000
                        && machine.inputTank().getAmountAsInt(0) == 0
                        && machine.progress() == 100,
                "Filling the final output batch retains the same-tick input as paid credit");
        machine.serverTick(helper.getLevel());
        helper.assertValueEqual(machine.progress(), 100, "Full output cannot erase the remainder");
        helper.succeed();
    }

    static void portsAndContainers(GameTestHelper helper) {
        var machine = machine(helper);
        for (var side : Direction.values()) {
            var port =
                    helper.getLevel()
                            .getCapability(Capabilities.Fluid.BLOCK, machine.getBlockPos(), side);
            helper.assertTrue(port != null, "Native sided fluid capability is registered");
            try (var transaction = Transaction.openRoot()) {
                helper.assertValueEqual(
                        port.insert(
                                0, fluid(FluidDefinition.SUPERHEATED_STEAM), 100001, transaction),
                        100000,
                        "Steam capacity is 100,000 mB");
                helper.assertValueEqual(
                        port.extract(0, fluid(FluidDefinition.SUPERHEATED_STEAM), 1, transaction),
                        0,
                        "Input steam cannot be extracted");
                helper.assertValueEqual(
                        port.insert(1, fluid(FluidDefinition.DISTILLED_WATER), 1, transaction),
                        0,
                        "Output forbids external insertion");
            }
        }
        helper.assertValueEqual(
                machine.inputTank().getAmountAsInt(0), 0, "Aborted transfers leave steam empty");
        try (var transaction = Transaction.openRoot()) {
            helper.assertValueEqual(
                    machine.fluidAutomation(Direction.UP)
                            .insert(0, FluidResource.of(Fluids.WATER), 1000, transaction),
                    0,
                    "Ordinary water is not steam");
            helper.assertValueEqual(
                    machine.automation(Direction.UP)
                            .insert(0, ItemResource.of(Items.BUCKET), 1, transaction),
                    0,
                    "Containers enter from below");
            helper.assertValueEqual(
                    machine.automation(Direction.DOWN)
                            .insert(0, ItemResource.of(Items.BUCKET), 1, transaction),
                    1,
                    "Bottom port accepts containers");
            transaction.commit();
        }
        fill(machine.outputTank(), fluid(FluidDefinition.DISTILLED_WATER), 1000);
        machine.serverTick(helper.getLevel());
        helper.assertTrue(
                machine.outputTank().getAmountAsInt(0) == 0
                        && machine.inventory()
                                .stack(1)
                                .is(
                                        ModFluids.FAMILIES
                                                .get(FluidDefinition.DISTILLED_WATER)
                                                .bucket()
                                                .get()),
                "Output container exchange works without power");
        helper.setBlock(POSITION.east(), ModMachines.block(MachineKind.TANK));
        var target = helper.getBlockEntity(POSITION.east(), TankBlockEntity.class);
        fill(machine.outputTank(), fluid(FluidDefinition.DISTILLED_WATER), 100);
        var ejector = new ItemStack(ModUpgrades.ALL.get(UpgradeItem.Kind.FLUID_EJECTOR).get());
        ejector.set(ModDataComponents.UPGRADE_DIRECTION, Direction.EAST);
        machine.inventory().set(7, ItemResource.of(ejector), 1);
        machine.serverTick(helper.getLevel());
        helper.assertValueEqual(
                target.tank().getAmountAsInt(0),
                50,
                "Directional output upgrade uses the same native port");
        helper.succeed();
    }

    static void heatComponent(GameTestHelper helper) {
        var item = ModReactorItems.HEAT_VENT.get();
        var stack = new ItemStack(item);
        item.exchangeHeat(stack, 999);
        helper.assertValueEqual(
                item.exchangeHeat(stack, 2), 1, "Overflow returns positive unaccepted heat");
        helper.assertValueEqual(
                item.dissipate(stack), 6, "Ordinary vent dissipates six actual heat per pass");
        var ops = helper.getLevel().registryAccess().createSerializationContext(JsonOps.INSTANCE);
        var restored =
                ItemStack.CODEC
                        .parse(ops, ItemStack.CODEC.encodeStart(ops, stack).getOrThrow())
                        .getOrThrow();
        var buffer =
                new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        try {
            ItemStack.STREAM_CODEC.encode(buffer, restored);
            var received = ItemStack.STREAM_CODEC.decode(buffer);
            helper.assertTrue(
                    ItemStack.matches(stack, received) && item.heat(received).stored() == 994,
                    "Component heat survives save and network codecs");
        } finally {
            buffer.release();
        }
        item.exchangeHeat(stack, -993);
        helper.assertValueEqual(
                item.dissipate(stack), 1, "Vent cannot emit more heat than it holds");
        helper.assertTrue(
                !stack.has(ModDataComponents.REACTOR_HEAT)
                        && !stack.isDamageableItem()
                        && stack.getMaxStackSize() == 1,
                "Empty heat is canonical, cannot be repaired away and cannot stack");
        helper.assertTrue(
                ModDataComponents.REACTOR_HEAT
                        .get()
                        .codec()
                        .parse(JsonOps.INSTANCE, new com.google.gson.JsonPrimitive(-1))
                        .error()
                        .isPresent(),
                "Negative saved heat is rejected");
        helper.succeed();
    }

    private static CondenserBlockEntity machine(GameTestHelper helper) {
        helper.setBlock(POSITION, ModMachines.block(MachineKind.CONDENSER));
        return helper.getBlockEntity(POSITION, CondenserBlockEntity.class);
    }

    private static FluidResource fluid(FluidDefinition definition) {
        return FluidResource.of(ModFluids.FAMILIES.get(definition).source().get());
    }

    private static void fill(
            ic2.neoforge.transfer.MachineFluidTank tank, FluidResource fluid, int amount) {
        try (var transaction = Transaction.openRoot()) {
            if (tank.insert(0, fluid, amount, transaction) != amount)
                throw new IllegalStateException("Test tank cannot fit input");
            transaction.commit();
        }
    }

    private CondenserTests() {}
}
