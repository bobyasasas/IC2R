package ic2.neoforge.test;

import ic2.neoforge.component.ModDataComponents;
import ic2.neoforge.item.CondensatorItem;
import ic2.neoforge.recipe.GradualRecipe;
import ic2.neoforge.registration.ModReactorItems;

import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;

final class GradualRecipeTests {
    private static RecipeHolder<GradualRecipe> recipe(GameTestHelper helper, String path) {
        var key = ResourceKey.create(Registries.RECIPE, Identifier.parse(path));
        var holder = helper.getLevel().recipeAccess().byKey(key).orElseThrow();
        return (RecipeHolder<GradualRecipe>) (RecipeHolder<?>) holder;
    }

    private static ItemStack condensator(CondensatorItem item, int heat) {
        var stack = item.getDefaultInstance();
        stack.set(ModDataComponents.REACTOR_HEAT, heat);
        return stack;
    }

    static void ventsStoredHeat(GameTestHelper helper) {
        var recipe = recipe(helper, "ic2:shapeless/rsh_condensator_redstone");
        var input =
                CraftingInput.of(
                        2,
                        1,
                        List.of(
                                condensator(ModReactorItems.RSH_CONDENSATOR.get(), 15000),
                                new ItemStack(Items.REDSTONE)));
        helper.assertTrue(recipe.value().matches(input, helper.getLevel()), "The vent matches");
        var result = recipe.value().assemble(input);
        helper.assertTrue(
                result.is(ModReactorItems.RSH_CONDENSATOR.get()) && result.getCount() == 1,
                "The condensator is recharged in place");
        helper.assertTrue(
                result.getOrDefault(ModDataComponents.REACTOR_HEAT, -1) == 5000,
                "15000 stored heat minus one 10000 redstone vent keeps 5000");
        helper.succeed();
    }

    static void partialVentKeepsRemainder(GameTestHelper helper) {
        var recipe = recipe(helper, "ic2:shapeless/lzh_condensator_lapis");
        var input =
                CraftingInput.of(
                        2,
                        2,
                        List.of(
                                condensator(ModReactorItems.LZH_CONDENSATOR.get(), 45000),
                                new ItemStack(Items.LAPIS_LAZULI),
                                ItemStack.EMPTY,
                                ItemStack.EMPTY));
        var result = recipe.value().assemble(input);
        helper.assertTrue(
                result.getOrDefault(ModDataComponents.REACTOR_HEAT, -1) == 5000,
                "45000 stored heat minus one 40000 lapis vent keeps 5000");
        helper.succeed();
    }

    static void freshCondensatorIsRejected(GameTestHelper helper) {
        var recipe = recipe(helper, "ic2:shapeless/rsh_condensator_redstone");
        var input =
                CraftingInput.of(
                        2,
                        1,
                        List.of(
                                condensator(ModReactorItems.RSH_CONDENSATOR.get(), 0),
                                new ItemStack(Items.REDSTONE)));
        helper.assertTrue(
                !recipe.value().matches(input, helper.getLevel()),
                "An already-empty condensator rejects the vent");
        helper.assertTrue(
                recipe.value().assemble(input).isEmpty(),
                "Assembling an empty condensator yields nothing");
        var wrongMaterial =
                CraftingInput.of(
                        2,
                        1,
                        List.of(
                                condensator(ModReactorItems.RSH_CONDENSATOR.get(), 15000),
                                new ItemStack(Items.DIAMOND)));
        helper.assertTrue(
                !recipe.value().matches(wrongMaterial, helper.getLevel()),
                "The vent only accepts its own charge material");
        helper.succeed();
    }

    private GradualRecipeTests() {}
}
