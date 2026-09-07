package ic2.neoforge.test;

import com.mojang.serialization.JsonOps;

import ic2.neoforge.component.ModDataComponents;
import ic2.neoforge.component.RemoteLinks;
import ic2.neoforge.item.ElectricItemEnergy;
import ic2.neoforge.registration.ModItems;

import io.netty.buffer.Unpooled;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStackTemplate;

import java.util.ArrayList;

final class ComponentTests {
    static void materials(GameTestHelper helper) {
        helper.assertValueEqual(
                ModItems.MATERIALS.size(), 140, "Material count against migration manifest");
        ModItems.MATERIALS.forEach(
                (definition, item) -> {
                    ItemStack stack = new ItemStack(item.get());
                    helper.assertTrue(!stack.isEmpty(), "Material must create a nonempty stack");
                    helper.assertValueEqual(
                            BuiltInRegistries.ITEM.getKey(stack.getItem()),
                            Identifier.fromNamespaceAndPath("ic2", definition.id()),
                            "Preserved material ID");
                });
        helper.succeed();
    }

    static void roundTrip(GameTestHelper helper) {
        ItemStack original = new ItemStack(ModItems.RE_BATTERY.get());
        original.set(ModDataComponents.CHARGE.get(), 1234.5);
        original.set(ModDataComponents.FLUID.get(), new FluidStackTemplate(Fluids.WATER, 500));
        original.set(ModDataComponents.HYDRATION_USES.get(), 42);
        var coordinates = new ArrayList<GlobalPos>();
        var mutablePos = new BlockPos.MutableBlockPos(1, 2, 3);
        coordinates.add(GlobalPos.of(Level.OVERWORLD, mutablePos));
        var links = new RemoteLinks(coordinates);
        original.set(ModDataComponents.REMOTE_LINKS.get(), links);
        coordinates.clear();
        mutablePos.set(8, 9, 10);
        helper.assertValueEqual(
                links.targets().getFirst().pos(),
                new BlockPos(1, 2, 3),
                "Links are a defensive immutable snapshot");
        var ops = helper.getLevel().registryAccess().createSerializationContext(JsonOps.INSTANCE);
        var encoded = ItemStack.CODEC.encodeStart(ops, original).getOrThrow();
        var restored = ItemStack.CODEC.parse(ops, encoded).getOrThrow();
        helper.assertTrue(
                ItemStack.matches(original, restored), "All components survive item persistence");
        var buffer =
                new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        try {
            ItemStack.STREAM_CODEC.encode(buffer, original);
            var received = ItemStack.STREAM_CODEC.decode(buffer);
            helper.assertTrue(
                    ItemStack.matches(original, received),
                    "All components survive network serialization");
        } finally {
            buffer.release();
        }
        helper.succeed();
    }

    static void battery(GameTestHelper helper) {
        var stack = new ItemStack(ModItems.RE_BATTERY.get());
        var before = stack.copy();
        helper.assertValueEqual(
                ElectricItemEnergy.charge(stack, 500, 1, false, true),
                100.0,
                "Simulated charge is limited");
        helper.assertTrue(
                ItemStack.matches(before, stack), "Simulation must not add a charge component");
        ElectricItemEnergy.charge(stack, 500, 1, false, false);
        before = stack.copy();
        helper.assertValueEqual(
                ElectricItemEnergy.discharge(stack, 40, 1, false, true, true),
                40.0,
                "Simulated discharge");
        helper.assertTrue(
                ItemStack.matches(before, stack), "Simulated discharge must not mutate charge");
        ElectricItemEnergy.discharge(stack, 40, 1, false, true, false);
        helper.assertValueEqual(ElectricItemEnergy.charge(stack), 60.0, "Committed charge balance");
        stack.setCount(2);
        helper.assertValueEqual(
                ElectricItemEnergy.charge(stack, 100, 1, false, false),
                0.0,
                "Stacked batteries cannot duplicate energy");
        helper.succeed();
    }

    static void invalidCharge(GameTestHelper helper) {
        var codec = ModDataComponents.CHARGE.get().codec();
        for (double value : new double[] {-1, Double.NaN, Double.POSITIVE_INFINITY}) {
            helper.assertTrue(
                    codec.encodeStart(JsonOps.INSTANCE, value).error().isPresent(),
                    "Invalid charge must be rejected on disk");
        }
        var buffer =
                new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        try {
            buffer.writeDouble(Double.NaN);
            boolean rejected = false;
            try {
                ModDataComponents.CHARGE.get().streamCodec().decode(buffer);
            } catch (IllegalArgumentException expected) {
                rejected = true;
            }
            helper.assertTrue(rejected, "Invalid charge must be rejected on the network");
        } finally {
            buffer.release();
        }
        helper.succeed();
    }
}
