package ic2.neoforge.test;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;

import ic2.neoforge.item.ElectricItemEnergy;
import ic2.neoforge.registration.MaterialDefinition;
import ic2.neoforge.registration.ModArmor;
import ic2.neoforge.registration.ModCraftingRecipes;
import ic2.neoforge.registration.ModItems;
import ic2.neoforge.registration.ModMaterialBlocks;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.registration.ModNuke;
import ic2.neoforge.registration.ModReactorItems;
import ic2.neoforge.registration.ModTools;

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

    static void powerArmorLine(GameTestHelper helper) {
        var carbon = material(MaterialDefinition.CARBON_PLATE);
        var alloy = material(MaterialDefinition.ALLOY);
        var iridium = material(MaterialDefinition.IRIDIUM);
        var crystal = ModItems.ENERGY_CRYSTAL.toStack();
        var lapotron = ModItems.LAPOTRON_CRYSTAL.toStack();
        assertCraft(
                helper,
                "ic2:shaped/nano_helmet",
                List.of(
                        carbon.copy(),
                        crystal.copy(),
                        carbon.copy(),
                        carbon.copy(),
                        ModArmor.NIGHT_VISION_GOGGLES.toStack(),
                        carbon.copy(),
                        ItemStack.EMPTY,
                        ItemStack.EMPTY,
                        ItemStack.EMPTY),
                ModArmor.NANO_HELMET.toStack());
        assertCraft(
                helper,
                "ic2:shaped/quantum_chestplate",
                List.of(
                        alloy.copy(),
                        ModArmor.NANO_CHESTPLATE.toStack(),
                        alloy.copy(),
                        iridium.copy(),
                        lapotron.copy(),
                        iridium.copy(),
                        iridium.copy(),
                        alloy.copy(),
                        iridium.copy()),
                ModArmor.QUANTUM_CHESTPLATE.toStack());
        helper.succeed();
    }

    static void packsCraftTheirStorageIn(GameTestHelper helper) {
        var battery = ModItems.RE_BATTERY.toStack();
        var circuit = material(MaterialDefinition.CIRCUIT);
        var advancedCircuit = material(MaterialDefinition.ADVANCED_CIRCUIT);
        var casing = material(MaterialDefinition.IRON_CASING);
        var crystal = ModItems.ENERGY_CRYSTAL.toStack();
        var planks = new ItemStack(Items.OAK_PLANKS);
        assertCraft(
                helper,
                "ic2:shaped/batpack",
                List.of(
                        battery.copy(),
                        circuit.copy(),
                        battery.copy(),
                        battery.copy(),
                        planks.copy(),
                        battery.copy(),
                        battery.copy(),
                        ItemStack.EMPTY,
                        battery.copy()),
                ModArmor.BATPACK.toStack());
        assertCraft(
                helper,
                "ic2:shaped/energy_pack",
                List.of(
                        advancedCircuit.copy(),
                        casing.copy(),
                        advancedCircuit.copy(),
                        crystal.copy(),
                        casing.copy(),
                        crystal.copy(),
                        casing.copy(),
                        ItemStack.EMPTY,
                        casing.copy()),
                ModArmor.ENERGY_PACK.toStack());
        helper.succeed();
    }

    static void chainsawCraftsFromBothRoutes(GameTestHelper helper) {
        var plate = material(MaterialDefinition.IRON_PLATE);
        var steel = material(MaterialDefinition.STEEL_INGOT);
        var circuit = material(MaterialDefinition.CIRCUIT);
        var powerUnit = material(MaterialDefinition.POWER_UNIT);
        assertCraft(
                helper,
                "ic2:shaped/chainsaw",
                List.of(
                        ItemStack.EMPTY,
                        plate.copy(),
                        plate.copy(),
                        plate.copy(),
                        plate.copy(),
                        plate.copy(),
                        powerUnit.copy(),
                        plate.copy(),
                        ItemStack.EMPTY),
                ModTools.CHAINSAW.toStack());
        assertCraft(
                helper,
                "ic2:shaped/chainsaw_from_steel",
                List.of(
                        ItemStack.EMPTY,
                        steel.copy(),
                        steel.copy(),
                        steel.copy(),
                        circuit.copy(),
                        steel.copy(),
                        ModItems.RE_BATTERY.toStack(),
                        steel.copy(),
                        ItemStack.EMPTY),
                ModTools.CHAINSAW.toStack());
        var wrongCore =
                List.of(
                        ItemStack.EMPTY,
                        steel.copy(),
                        steel.copy(),
                        steel.copy(),
                        material(MaterialDefinition.IRON_CASING).copy(),
                        steel.copy(),
                        ModItems.RE_BATTERY.toStack(),
                        steel.copy(),
                        ItemStack.EMPTY);
        var recipe =
                (CraftingRecipe)
                        helper.getLevel()
                                .recipeAccess()
                                .byKey(ResourceKey.create(
                                        Registries.RECIPE,
                                        Identifier.parse("ic2:shaped/chainsaw_from_steel")))
                                .orElseThrow()
                                .value();
        helper.assertTrue(
                !recipe.matches(CraftingInput.of(3, 3, wrongCore), helper.getLevel()),
                "Swapping the circuit for a casing must break the match");
        helper.succeed();
    }

    static void utilityItemsCraft(GameTestHelper helper) {
        var cable = new ItemStack(ModMachines.CABLES.get("insulated_copper_cable").get());
        var reflector = ModReactorItems.THICK_NEUTRON_REFLECTOR.toStack();
        var advancedCircuit = material(MaterialDefinition.ADVANCED_CIRCUIT);
        var advancedMachine =
                new ItemStack(ModMaterialBlocks.MATERIALS.get("advanced_machine").get());
        assertCraft(
                helper,
                "ic2:shaped/static_boots_from_boots",
                List.of(
                        ItemStack.EMPTY,
                        new ItemStack(Items.IRON_BOOTS),
                        ItemStack.EMPTY,
                        ItemStack.EMPTY,
                        new ItemStack(Items.WHITE_WOOL),
                        ItemStack.EMPTY,
                        cable.copy(),
                        cable.copy(),
                        cable.copy()),
                ModArmor.STATIC_BOOTS_ITEM.toStack());
        assertCraft(
                helper,
                "ic2:shaped/nuke",
                List.of(
                        reflector.copy(),
                        advancedCircuit.copy(),
                        reflector.copy(),
                        reflector.copy(),
                        advancedMachine.copy(),
                        reflector.copy(),
                        reflector.copy(),
                        advancedCircuit.copy(),
                        reflector.copy()),
                ModNuke.NUKE_ITEM.toStack());
        helper.succeed();
    }

    private static ItemStack material(MaterialDefinition definition) {
        return new ItemStack(ModItems.MATERIALS.get(definition).get());
    }

    private static void assertCraft(
            GameTestHelper helper, String recipeId, List<ItemStack> grid, ItemStack expected) {
        var recipe =
                (CraftingRecipe)
                        helper.getLevel()
                                .recipeAccess()
                                .byKey(ResourceKey.create(Registries.RECIPE, Identifier.parse(recipeId)))
                                .orElseThrow()
                                .value();
        var input = CraftingInput.of(3, 3, grid);
        helper.assertTrue(
                recipe.matches(input, helper.getLevel()),
                recipeId + " must match its legacy grid");
        var result = recipe.assemble(input);
        helper.assertTrue(
                result.getItem() == expected.getItem() && result.getCount() == expected.getCount(),
                recipeId + " must craft the legacy result item");
    }

    private CraftingTests() {}
}
