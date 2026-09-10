package ic2.neoforge.test;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;

import ic2.core.recipe.ProcessingMethod;
import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.machine.SingleInputBlockEntity;
import ic2.neoforge.recipe.ProcessingRecipe;
import ic2.neoforge.registration.ModCannerRecipes;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.registration.ModProcessingRecipes;
import ic2.neoforge.registration.ModThermalRecipes;

import io.netty.buffer.Unpooled;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.transfer.item.ItemResource;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

final class ProcessingTests {
    static void loadedRecipes(GameTestHelper helper) {
        try (var stream =
                ProcessingTests.class.getResourceAsStream("/ic2_tests/converted-recipes.json")) {
            var ids =
                    JsonParser.parseReader(
                                    new InputStreamReader(
                                            Objects.requireNonNull(stream), StandardCharsets.UTF_8))
                            .getAsJsonArray();
            for (var id : ids) {
                ResourceKey<Recipe<?>> key =
                        ResourceKey.create(Registries.RECIPE, Identifier.parse(id.getAsString()));
                helper.assertTrue(
                        helper.getLevel().recipeAccess().byKey(key).isPresent(),
                        "Converted recipe failed to load: " + id.getAsString());
            }
            helper.assertTrue(!ids.isEmpty(), "Recipe manifest must not be empty");
        } catch (IOException error) {
            throw new AssertionError(error);
        }
        helper.succeed();
    }

    /** Each processing family the JEI plugin renders must have server-side recipes. */
    static void jeiCategoriesNonEmpty(GameTestHelper helper) {
        List<RecipeType<?>> categories = new ArrayList<>();
        for (ProcessingMethod method : ProcessingMethod.values())
            categories.add(ModProcessingRecipes.type(method));
        categories.add(ModCannerRecipes.SOLID.get());
        categories.add(ModCannerRecipes.ENRICH.get());
        categories.add(ModThermalRecipes.FERMENTING.get());
        categories.add(ModThermalRecipes.COOLING.get());
        categories.add(ModThermalRecipes.ELECTROLYZING.get());
        categories.add(ModProcessingRecipes.WASHING_TYPE.get());
        categories.add(ModProcessingRecipes.CENTRIFUGE_TYPE.get());
        categories.add(ModProcessingRecipes.BLAST_FURNACE_TYPE.get());
        categories.add(ModProcessingRecipes.MATTER_FABRICATOR_TYPE.get());
        for (RecipeType<?> type : categories) {
            long count =
                    helper.getLevel()
                            .recipeAccess()
                            .getRecipes()
                            .stream()
                            .filter(holder -> holder.value().getType() == type)
                            .count();
            helper.assertTrue(
                    count > 0,
                    "No recipes for " + type + "; JEI category would be empty");
        }
        helper.succeed();
    }

    private static SingleInputBlockEntity machine(GameTestHelper helper, MachineKind kind) {
        return new SingleInputBlockEntity(
                helper.absolutePos(new BlockPos(1, 1, 1)),
                ModMachines.block(kind).defaultBlockState());
    }

    static void processing(GameTestHelper helper) {
        var macerator = machine(helper, MachineKind.MACERATOR);
        macerator.energy().insert(600);
        macerator.inventory().set(0, ItemResource.of(Items.IRON_INGOT), 1);
        for (int tick = 0; tick < 300; tick++) macerator.serverTick(helper.getLevel());
        helper.assertTrue(
                macerator.inventory().stack(0).isEmpty()
                        && macerator
                                .inventory()
                                .getResource(1)
                                .getItem()
                                .toString()
                                .equals("ic2:iron_dust"),
                "Macerator must use migrated iron dust recipe");
        helper.assertTrue(macerator.energy().stored() == 0, "Maceration costs 600 EU");
        var compressor = machine(helper, MachineKind.COMPRESSOR);
        compressor.energy().insert(600);
        compressor.inventory().set(0, ItemResource.of(Items.ICE), 1);
        compressor.serverTick(helper.getLevel());
        helper.assertTrue(
                compressor.progress() == 0 && compressor.energy().stored() == 600,
                "Counted recipe must reject insufficient input");
        compressor.inventory().set(0, ItemResource.of(Items.ICE), 3);
        for (int tick = 0; tick < 300; tick++) compressor.serverTick(helper.getLevel());
        helper.assertTrue(
                compressor.inventory().stack(0).getCount() == 1
                        && compressor.inventory().stack(1).is(Items.PACKED_ICE),
                "Compression must consume exactly two ice");
        var extractor = machine(helper, MachineKind.EXTRACTOR);
        extractor.energy().insert(600);
        extractor.inventory().set(0, ItemResource.of(Items.BAMBOO), 1);
        for (int tick = 0; tick < 300; tick++) extractor.serverTick(helper.getLevel());
        helper.assertTrue(
                extractor.inventory().stack(1).is(Items.STICK),
                "Extractor must load and execute its own recipe family");
        helper.succeed();
    }

    static void weightedPersistence(GameTestHelper helper) {
        var machine = machine(helper, MachineKind.MACERATOR);
        machine.energy().insert(600);
        machine.inventory().set(0, ItemResource.of(Items.IRON_ORE), 1);
        machine.inventory().set(1, ItemResource.of(Items.COBBLESTONE), 64);
        machine.serverTick(helper.getLevel());
        var saved = machine.saveWithFullMetadata(helper.getLevel().registryAccess());
        var selected =
                ItemStackTemplate.CODEC
                        .parse(
                                helper.getLevel()
                                        .registryAccess()
                                        .createSerializationContext(NbtOps.INSTANCE),
                                saved.getCompound("selectedOutput").orElseThrow())
                        .getOrThrow();
        helper.assertTrue(
                selected.count() >= 2 && selected.count() <= 4,
                "Weighted output must be one of the legacy outcomes");
        var restored =
                (SingleInputBlockEntity)
                        BlockEntity.loadStatic(
                                machine.getBlockPos(),
                                machine.getBlockState(),
                                saved,
                                helper.getLevel().registryAccess());
        for (int tick = 0; tick < 10; tick++) restored.serverTick(helper.getLevel());
        helper.assertTrue(
                restored.energy().stored() == 600 && restored.progress() == 0,
                "Blocked weighted output must not consume energy");
        helper.assertTrue(
                restored.saveWithFullMetadata(helper.getLevel().registryAccess())
                        .getCompound("selectedOutput")
                        .equals(saved.getCompound("selectedOutput")),
                "Waiting and reload must not reroll the selected output");
        restored.inventory().set(1, ItemResource.EMPTY, 0);
        for (int tick = 0; tick < 300; tick++) restored.serverTick(helper.getLevel());
        helper.assertTrue(
                restored.inventory().stack(1).is(Items.RAW_IRON)
                        && restored.inventory().stack(1).getCount() == selected.count(),
                "Completion must deliver exactly the saved weighted outcome");
        helper.succeed();
    }

    static void recipeCodec(GameTestHelper helper) {
        var original =
                new ProcessingRecipe(
                        ProcessingMethod.COMPRESSOR,
                        Ingredient.of(Items.ICE),
                        2,
                        List.of(
                                new ProcessingRecipe.Output(
                                        new ItemStackTemplate(Items.PACKED_ICE), 1)));
        var codec = ProcessingRecipe.codec(ProcessingMethod.COMPRESSOR).codec();
        var ops = helper.getLevel().registryAccess().createSerializationContext(JsonOps.INSTANCE);
        var encoded = codec.encodeStart(ops, original).getOrThrow();
        var decoded = codec.parse(ops, encoded).getOrThrow();
        helper.assertTrue(
                decoded.inputCount() == 2 && decoded.outputs().equals(original.outputs()),
                "Recipe disk codec must preserve quantities and outcomes");
        var buffer =
                new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        try {
            var streamCodec = ProcessingRecipe.streamCodec(ProcessingMethod.COMPRESSOR);
            streamCodec.encode(buffer, original);
            var network = streamCodec.decode(buffer);
            helper.assertTrue(
                    network.inputCount() == 2 && network.outputs().equals(original.outputs()),
                    "Recipe stream codec must preserve quantities and outcomes");
        } finally {
            buffer.release();
        }
        encoded.getAsJsonObject().addProperty("input_count", 0);
        helper.assertTrue(
                codec.parse(ops, encoded).error().isPresent(),
                "Zero-input recipes must be rejected");
        helper.succeed();
    }

    private ProcessingTests() {}
}
