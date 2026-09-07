package ic2.neoforge.test;

import com.mojang.serialization.JsonOps;

import ic2.neoforge.component.ModDataComponents;
import ic2.neoforge.item.UpgradeItem;
import ic2.neoforge.machine.CannerBlockEntity;
import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.machine.OreWashingBlockEntity;
import ic2.neoforge.recipe.WashingRecipe;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.registration.ModUpgrades;

import io.netty.buffer.Unpooled;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.List;

final class WashingTests {
    static void atomicOutputs(GameTestHelper helper) {
        var machine = machine(helper);
        machine.inventory().set(0, item("crushed_iron"), 1);
        machine.energy().insert(8000);
        machine.serverTick(helper.getLevel());
        helper.assertTrue(
                machine.progress() == 0 && machine.energy().stored() == 8000,
                "Dry washer must not start or spend EU");
        machine.tank().set(0, FluidResource.of(Fluids.WATER), 1000);
        for (int tick = 0; tick < 499; tick++) machine.serverTick(helper.getLevel());
        machine.inventory().set(3, ItemResource.of(Items.DIRT), 64);
        machine.inventory().set(4, ItemResource.of(Items.DIRT), 64);
        machine.serverTick(helper.getLevel());
        helper.assertTrue(
                machine.progress() == 499
                        && machine.energy().stored() == 16
                        && machine.inventory().stack(1).isEmpty()
                        && machine.tank().getAmountAsInt(0) == 1000
                        && machine.inventory().getAmountAsInt(0) == 1,
                "A blocked byproduct must roll back every simulated output and pause completion"
                    + " without spending water or EU");
        var restored =
                (OreWashingBlockEntity)
                        BlockEntity.loadStatic(
                                machine.getBlockPos(),
                                machine.getBlockState(),
                                machine.saveWithFullMetadata(helper.getLevel().registryAccess()),
                                helper.getLevel().registryAccess());
        restored.inventory().set(3, ItemResource.EMPTY, 0);
        restored.inventory().set(4, ItemResource.EMPTY, 0);
        restored.serverTick(helper.getLevel());
        helper.assertTrue(
                restored.energy().stored() == 0
                        && restored.tank().getAmountAsInt(0) == 0
                        && restored.inventory().getAmountAsInt(0) == 0,
                "Reloaded completion must consume exactly 8000 EU, one crushed ore and 1000 mB"
                    + " water");
        helper.assertTrue(
                restored.inventory().getResource(1).equals(item("purified_iron"))
                        && restored.inventory().getResource(3).equals(item("small_iron_dust"))
                        && restored.inventory().getAmountAsInt(3) == 2
                        && restored.inventory().getResource(4).equals(item("stone_dust")),
                "All three outputs must appear together");
        helper.succeed();
    }

    static void containers(GameTestHelper helper) {
        var machine = machine(helper);
        machine.inventory().set(5, ItemResource.of(Items.WATER_BUCKET), 1);
        machine.inventory().set(6, ItemResource.of(Items.DIRT), 64);
        machine.serverTick(helper.getLevel());
        helper.assertTrue(
                machine.tank().getAmountAsInt(0) == 0
                        && machine.inventory().stack(5).is(Items.WATER_BUCKET),
                "Blocked empty bucket slot must roll back container transfer");
        machine.inventory().set(6, ItemResource.EMPTY, 0);
        machine.serverTick(helper.getLevel());
        helper.assertTrue(
                machine.tank().getAmountAsInt(0) == 1000
                        && machine.inventory().stack(5).isEmpty()
                        && machine.inventory().stack(6).is(Items.BUCKET),
                "Water bucket must enter the tank and return an empty bucket");
        try (var tx = Transaction.openRoot()) {
            helper.assertTrue(
                    machine.fluidAutomation(Direction.NORTH)
                                    .insert(0, FluidResource.of(Fluids.LAVA), 1000, tx)
                            == 0,
                    "Water tank rejects lava");
            helper.assertTrue(
                    machine.fluidAutomation(Direction.NORTH)
                                    .extract(0, FluidResource.of(Fluids.WATER), 1000, tx)
                            == 0,
                    "Water tank is externally insert-only");
            helper.assertTrue(
                    machine.automation(Direction.DOWN)
                                    .extract(6, ItemResource.of(Items.BUCKET), 1, tx)
                            == 1,
                    "Automation can retrieve empty containers");
        }
        helper.succeed();
    }

    static void pullingUpgrade(GameTestHelper helper) {
        var position = new BlockPos(3, 1, 3);
        helper.setBlock(position, ModMachines.block(MachineKind.ORE_WASHING_PLANT));
        helper.setBlock(position.north(), ModMachines.block(MachineKind.CANNER));
        var machine = helper.getBlockEntity(position, OreWashingBlockEntity.class);
        var source = helper.getBlockEntity(position.north(), CannerBlockEntity.class);
        source.outputTank().set(0, FluidResource.of(Fluids.WATER), 500);
        var upgrade = ModUpgrades.ALL.get(UpgradeItem.Kind.FLUID_PULLING).toStack();
        upgrade.set(ModDataComponents.UPGRADE_DIRECTION, Direction.NORTH);
        machine.inventory().set(machine.kind().upgradeStart(), ItemResource.of(upgrade), 1);
        machine.serverTick(helper.getLevel());
        helper.assertTrue(
                machine.tank().getAmountAsInt(0) == 50
                        && source.outputTank().getAmountAsInt(0) == 450,
                "Directional upgrade must use shared native fluid capabilities");
        helper.succeed();
    }

    static void codec(GameTestHelper helper) {
        var recipe =
                new WashingRecipe(
                        Ingredient.of(Items.GRAVEL),
                        2,
                        List.of(new ItemStackTemplate(Items.FLINT, 3)),
                        2000);
        var ops = helper.getLevel().registryAccess().createSerializationContext(JsonOps.INSTANCE);
        var restored =
                WashingRecipe.CODEC
                        .codec()
                        .parse(
                                ops,
                                WashingRecipe.CODEC.codec().encodeStart(ops, recipe).getOrThrow())
                        .getOrThrow();
        helper.assertTrue(
                restored.inputCount() == 2
                        && restored.water() == 2000
                        && restored.outputs().equals(recipe.outputs()),
                "JSON round trip preserves count, water and all outputs");
        var buffer =
                new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        try {
            WashingRecipe.STREAM_CODEC.encode(buffer, recipe);
            var decoded = WashingRecipe.STREAM_CODEC.decode(buffer);
            helper.assertTrue(
                    decoded.inputCount() == 2
                            && decoded.water() == 2000
                            && decoded.outputs().equals(recipe.outputs()),
                    "Network recipe must preserve all fields");
        } finally {
            buffer.release();
        }
        helper.succeed();
    }

    private static OreWashingBlockEntity machine(GameTestHelper helper) {
        return new OreWashingBlockEntity(
                helper.absolutePos(new BlockPos(1, 1, 1)),
                ModMachines.block(MachineKind.ORE_WASHING_PLANT).defaultBlockState());
    }

    private static ItemResource item(String id) {
        return ItemResource.of(BuiltInRegistries.ITEM.getValue(Identifier.parse("ic2:" + id)));
    }

    private WashingTests() {}
}
