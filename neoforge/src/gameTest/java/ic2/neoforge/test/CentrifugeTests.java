package ic2.neoforge.test;

import com.mojang.serialization.JsonOps;

import ic2.neoforge.machine.CentrifugeBlockEntity;
import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.recipe.CentrifugeRecipe;
import ic2.neoforge.registration.ModMachines;

import io.netty.buffer.Unpooled;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.transfer.item.ItemResource;

import java.util.List;

final class CentrifugeTests {
    static void processingAndPersistence(GameTestHelper helper) {
        var machine = machine(helper);
        machine.inventory().set(0, item("purified_iron"), 1);
        machine.energy().insert(40000);
        for (int tick = 0; tick < 1500; tick++) machine.serverTick(helper.getLevel());
        helper.assertTrue(
                machine.progress() == 0
                        && machine.heat() == 1500
                        && machine.energy().stored() == 38500,
                "Centrifuge must preheat before processing and spend one EU per heat unit");
        for (int tick = 0; tick < 200; tick++) machine.serverTick(helper.getLevel());
        var restored =
                (CentrifugeBlockEntity)
                        BlockEntity.loadStatic(
                                machine.getBlockPos(),
                                machine.getBlockState(),
                                machine.saveWithFullMetadata(helper.getLevel().registryAccess()),
                                helper.getLevel().registryAccess());
        helper.assertTrue(
                restored.progress() == 200 && restored.heat() == 1501,
                "In-flight heat and progress must survive reload");
        for (int tick = 0; tick < 300; tick++) restored.serverTick(helper.getLevel());
        helper.assertTrue(
                restored.inventory().getResource(1).equals(item("iron_dust"))
                        && restored.inventory().getResource(3).equals(item("small_gold_dust"))
                        && restored.inventory().stack(0).isEmpty(),
                "Both centrifuge outputs must complete once after reload");
        helper.assertTrue(
                restored.energy().stored() == 14001,
                "EU accounting must include 1500 preheat, 24000 processing and 499 maintenance");
        helper.assertTrue(
                restored.energyNode().input().orElseThrow().voltage() == 128
                        && restored.upgradeProfile().itemTier() == 2,
                "Native energy node and battery charging must use MV");
        helper.succeed();
    }

    static void redstoneAndOutputBlocking(GameTestHelper helper) {
        var pos = new BlockPos(3, 1, 3);
        helper.setBlock(pos, ModMachines.block(MachineKind.CENTRIFUGE));
        var machine = helper.getBlockEntity(pos, CentrifugeBlockEntity.class);
        machine.inventory().set(0, item("purified_iron"), 1);
        for (int slot : new int[] {1, 3, 4})
            machine.inventory().set(slot, ItemResource.of(Items.DIRT), 64);
        machine.energy().insert(1000);
        for (int tick = 0; tick < 10; tick++) machine.serverTick(helper.getLevel());
        helper.assertTrue(
                machine.heat() == 0 && machine.energy().stored() == 1000,
                "Blocked products must not request idle heating");
        helper.setBlock(pos.west(), Blocks.REDSTONE_BLOCK);
        for (int tick = 0; tick < 10; tick++) machine.serverTick(helper.getLevel());
        helper.assertTrue(
                machine.heat() == 10
                        && machine.energy().stored() == 990
                        && machine.menuValue(1) == 5000,
                "Redstone must preheat even with blocked products");
        helper.setBlock(pos.west(), Blocks.AIR);
        machine.serverTick(helper.getLevel());
        helper.assertTrue(
                machine.heat() == 9 && machine.energy().stored() == 990,
                "Removing redstone must cool without spending EU");
        helper.succeed();
    }

    static void recipeCodec(GameTestHelper helper) {
        var recipe =
                new CentrifugeRecipe(
                        Ingredient.of(Items.IRON_INGOT),
                        2,
                        List.of(new ItemStackTemplate(Items.IRON_NUGGET, 3)),
                        1500);
        var ops = helper.getLevel().registryAccess().createSerializationContext(JsonOps.INSTANCE);
        var restored =
                CentrifugeRecipe.CODEC
                        .codec()
                        .parse(
                                ops,
                                CentrifugeRecipe.CODEC
                                        .codec()
                                        .encodeStart(ops, recipe)
                                        .getOrThrow())
                        .getOrThrow();
        helper.assertTrue(
                restored.inputCount() == 2
                        && restored.minHeat() == 1500
                        && restored.outputs().equals(recipe.outputs()),
                "JSON recipe must preserve temperature and outputs");
        var buffer =
                new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        try {
            CentrifugeRecipe.STREAM_CODEC.encode(buffer, recipe);
            var decoded = CentrifugeRecipe.STREAM_CODEC.decode(buffer);
            helper.assertTrue(
                    decoded.inputCount() == 2
                            && decoded.minHeat() == 1500
                            && decoded.outputs().equals(recipe.outputs()),
                    "Network recipe must preserve temperature and outputs");
        } finally {
            buffer.release();
        }
        helper.succeed();
    }

    private static CentrifugeBlockEntity machine(GameTestHelper helper) {
        return new CentrifugeBlockEntity(
                helper.absolutePos(new BlockPos(1, 1, 1)),
                ModMachines.block(MachineKind.CENTRIFUGE).defaultBlockState());
    }

    private static ItemResource item(String id) {
        return ItemResource.of(BuiltInRegistries.ITEM.getValue(Identifier.parse("ic2:" + id)));
    }

    private CentrifugeTests() {}
}
