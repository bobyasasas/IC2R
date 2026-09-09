package ic2.neoforge.test;

import ic2.neoforge.component.ModDataComponents;
import ic2.neoforge.item.FuelRodItem;
import ic2.neoforge.registration.ModItems;
import ic2.neoforge.registration.ModReactorItems;

import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;

final class FuelRodTests {
    private static RecipeHolder<Recipe<CraftingInput>> shaped(GameTestHelper helper, String path) {
        var key = ResourceKey.create(Registries.RECIPE, Identifier.parse(path));
        return (RecipeHolder<Recipe<CraftingInput>>)
                (RecipeHolder<?>) helper.getLevel().recipeAccess().byKey(key).orElseThrow();
    }

    private static ItemStack rod(FuelRodItem item, int use) {
        var stack = item.getDefaultInstance();
        if (use != 0) stack.set(ModDataComponents.REACTOR_USE, use);
        return stack;
    }

    private static ItemStack ironPlate() {
        return ModItems.MATERIALS
                .get(ic2.neoforge.registration.MaterialDefinition.IRON_PLATE)
                .get()
                .getDefaultInstance();
    }

    static void freshRodsCraftDualRod(GameTestHelper helper) {
        var recipe = shaped(helper, "ic2:shaped/dual_uranium_fuel_rod");
        var input =
                CraftingInput.of(
                        3,
                        1,
                        List.of(
                                rod(ModReactorItems.URANIUM_FUEL_ROD.get(), 0),
                                ironPlate(),
                                rod(ModReactorItems.URANIUM_FUEL_ROD.get(), 0)));
        helper.assertTrue(
                recipe.value().matches(input, helper.getLevel()),
                "Two fresh uranium rods and an iron plate craft the dual rod");
        var result = recipe.value().assemble(input);
        helper.assertTrue(
                result.is(ModReactorItems.DUAL_URANIUM_FUEL_ROD.get()),
                "The dual uranium fuel rod is the crafted result");
        helper.succeed();
    }

    static void usedRodsAreRejected(GameTestHelper helper) {
        var recipe = shaped(helper, "ic2:shaped/dual_uranium_fuel_rod");
        var input =
                CraftingInput.of(
                        3,
                        1,
                        List.of(
                                rod(ModReactorItems.URANIUM_FUEL_ROD.get(), 100),
                                ironPlate(),
                                rod(ModReactorItems.URANIUM_FUEL_ROD.get(), 0)));
        helper.assertTrue(
                !recipe.value().matches(input, helper.getLevel()),
                "A depleted rod is rejected by the dual rod recipe");
        helper.succeed();
    }

    static void depletedRodsChainToCentrifuge(GameTestHelper helper) {
        var depleted = ModReactorItems.DEPLETED_QUAD_MOX_FUEL_ROD.get();
        helper.assertTrue(
                !depleted.getDefaultInstance().isEmpty(),
                "The depleted quad MOX rod is registered for the centrifuge chain");
        var recipeKey =
                ResourceKey.create(
                        Registries.RECIPE,
                        Identifier.parse(
                                "ic2:centrifuge/depleted_quad_mox_fuel_rod_to_small_plutonium"));
        helper.assertTrue(
                helper.getLevel().recipeAccess().byKey(recipeKey).isPresent(),
                "The depleted rod centrifuge recipe is loaded");
        helper.succeed();
    }

    private FuelRodTests() {}
}
