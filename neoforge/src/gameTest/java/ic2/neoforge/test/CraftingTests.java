package ic2.neoforge.test;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;

import ic2.neoforge.item.ElectricItemEnergy;
import ic2.neoforge.registration.MaterialDefinition;
import ic2.neoforge.registration.ModCraftingRecipes;
import ic2.neoforge.registration.ModItems;

import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;

import java.util.List;

final class CraftingTests {
    static void charge(GameTestHelper helper) {
        ResourceKey<Recipe<?>> key =
                ResourceKey.create(
                        Registries.RECIPE, Identifier.parse("ic2:shaped/lapotron_crystal"));
        var recipe =
                (CraftingRecipe) helper.getLevel().recipeAccess().byKey(key).orElseThrow().value();
        var crystal = new ItemStack(ModItems.ENERGY_CRYSTAL.get());
        ElectricItemEnergy.charge(crystal, 900000, Integer.MAX_VALUE, true, false);
        var dust = new ItemStack(ModItems.MATERIALS.get(MaterialDefinition.LAPIS_DUST).get());
        var circuit =
                new ItemStack(ModItems.MATERIALS.get(MaterialDefinition.ADVANCED_CIRCUIT).get());
        var input =
                CraftingInput.of(
                        3,
                        3,
                        List.of(
                                dust.copy(),
                                circuit.copy(),
                                dust.copy(),
                                dust.copy(),
                                crystal,
                                dust.copy(),
                                dust.copy(),
                                circuit.copy(),
                                dust.copy()));
        helper.assertTrue(
                recipe.matches(input, helper.getLevel()),
                "Migrated shaped recipe must match its original pattern and common tags");
        var result = recipe.assemble(input);
        helper.assertTrue(
                result.is(ModItems.LAPOTRON_CRYSTAL.get())
                        && ElectricItemEnergy.charge(result) == 900000,
                "Crafting must preserve input electric charge");
        helper.assertTrue(
                ElectricItemEnergy.charge(crystal) == 900000,
                "Assemble preview must not mutate its input");
        helper.succeed();
    }

    static void remainder(GameTestHelper helper) {
        var ops = helper.getLevel().registryAccess().createSerializationContext(JsonOps.INSTANCE);
        var json =
                JsonParser.parseString(
                        """
                        {"ingredients":["minecraft:water_bucket"],"result":{"id":"minecraft:clay_ball"},"consuming":false,"hidden":true}
                        """);
        var recipe =
                ModCraftingRecipes.SHAPELESS.get().codec().codec().parse(ops, json).getOrThrow();
        var input = CraftingInput.of(1, 1, List.of(new ItemStack(Items.WATER_BUCKET)));
        helper.assertTrue(
                recipe.matches(input, helper.getLevel()),
                "Shapeless wrapper must delegate matching");
        helper.assertTrue(
                recipe.getRemainingItems(input).getFirst().is(Items.BUCKET),
                "Non-consuming recipe must preserve crafting containers");
        helper.assertTrue(
                recipe.isSpecial() && recipe.display().isEmpty(),
                "Hidden recipes must stay out of the recipe book");
        json.getAsJsonObject().addProperty("consuming", true);
        var consuming =
                ModCraftingRecipes.SHAPELESS.get().codec().codec().parse(ops, json).getOrThrow();
        helper.assertTrue(
                consuming.getRemainingItems(input).getFirst().isEmpty(),
                "Consuming recipe must honor legacy remainder suppression");
        helper.succeed();
    }

    private CraftingTests() {}
}
