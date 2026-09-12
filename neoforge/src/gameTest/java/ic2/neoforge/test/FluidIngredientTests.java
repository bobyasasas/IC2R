package ic2.neoforge.test;

import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.machine.SingleInputBlockEntity;
import ic2.neoforge.registration.MaterialDefinition;
import ic2.neoforge.registration.ModCells;
import ic2.neoforge.registration.ModItems;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.registration.ModReactorItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.neoforged.neoforge.transfer.item.ItemResource;

import java.util.List;

/** Legacy RecipeInputFluidContainer ingredients: cells satisfy fluid inputs and drain to the
 *  empty cell, both in the crafting grid and inside machines. */
final class FluidIngredientTests {
    private static CraftingRecipe recipe(GameTestHelper helper, String id) {
        return (CraftingRecipe)
                helper.getLevel()
                        .recipeAccess()
                        .byKey(ResourceKey.create(Registries.RECIPE, Identifier.parse(id)))
                        .orElseThrow()
                        .value();
    }

    static void shapelessWaterCellCraftsAndDrains(GameTestHelper helper) {
        var recipe = recipe(helper, "ic2:shapeless/cold_coffee_mug");
        var mug = new ItemStack(ModItems.EMPTY_MUG.get());
        var coffee = new ItemStack(ModItems.MATERIALS.get(MaterialDefinition.COFFEE_POWDER).get());
        var water = new ItemStack(ModCells.WATER.get());
        var input = CraftingInput.of(3, 3, List.of(
                mug.copy(), coffee.copy(), water.copy(),
                ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
                ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY));
        helper.assertTrue(
                recipe.matches(input, helper.getLevel()),
                "A water cell must satisfy the recipe's 1000 mB water request");
        helper.assertTrue(
                recipe.assemble(input).is(ModItems.COLD_COFFEE_MUG.get()),
                "The converted recipe must assemble the registered cold coffee mug");
        var remainders = recipe.getRemainingItems(input);
        helper.assertTrue(
                remainders.get(2).is(ModCells.EMPTY.get()),
                "A drained water cell must hand back the empty cell");
        helper.assertTrue(
                remainders.get(0).isEmpty() && remainders.get(1).isEmpty(),
                "Plain ingredients must be consumed without remainders");
        var wrongFluid = CraftingInput.of(3, 3, List.of(
                mug.copy(), coffee.copy(), new ItemStack(ModCells.LAVA.get()),
                ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
                ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY));
        helper.assertTrue(
                !recipe.matches(wrongFluid, helper.getLevel()),
                "A lava cell must not satisfy a water ingredient");
        helper.succeed();
    }

    static void shapedCoolantCellMatches(GameTestHelper helper) {
        var recipe = recipe(helper, "ic2:shaped/reactor_coolant_cell");
        var plate = new ItemStack(ModItems.MATERIALS.get(MaterialDefinition.TIN_PLATE).get());
        var coolant = new ItemStack(ModCells.CELLS.get("coolant_cell").get());
        var input = CraftingInput.of(3, 3, List.of(
                ItemStack.EMPTY, plate.copy(), ItemStack.EMPTY,
                plate.copy(), coolant.copy(), plate.copy(),
                ItemStack.EMPTY, plate.copy(), ItemStack.EMPTY));
        helper.assertTrue(
                recipe.matches(input, helper.getLevel()),
                "A coolant cell must satisfy the shaped coolant request");
        helper.assertTrue(
                recipe.assemble(input).is(ModReactorItems.REACTOR_COOLANT_CELL.get()),
                "The coolant recipe must assemble the reactor coolant cell");
        var remainders = recipe.getRemainingItems(input);
        helper.assertTrue(
                remainders.get(4).is(ModCells.EMPTY.get()),
                "The drained coolant cell must hand back the empty cell");
        var water = CraftingInput.of(3, 3, List.of(
                ItemStack.EMPTY, plate.copy(), ItemStack.EMPTY,
                plate.copy(), new ItemStack(ModCells.WATER.get()), plate.copy(),
                ItemStack.EMPTY, plate.copy(), ItemStack.EMPTY));
        helper.assertTrue(
                !recipe.matches(water, helper.getLevel()),
                "A water cell must not satisfy a coolant ingredient");
        helper.succeed();
    }

    static void compressorWaterCellToSnow(GameTestHelper helper) {
        var compressor = new SingleInputBlockEntity(
                helper.absolutePos(new BlockPos(1, 1, 1)),
                ModMachines.block(MachineKind.COMPRESSOR).defaultBlockState());
        compressor.energy().insert(600);
        compressor.inventory().set(0, ItemResource.of(ModCells.WATER.get()), 1);
        for (int tick = 0; tick < 300; tick++) compressor.serverTick(helper.getLevel());
        helper.assertTrue(
                compressor.inventory().stack(0).is(ModCells.EMPTY.get())
                        && compressor.inventory().stack(0).getCount() == 1,
                "The machine must drain the water cell and keep the empty cell in the input slot");
        helper.assertTrue(
                compressor.inventory().stack(1).is(Items.SNOW_BLOCK),
                "The converted fluid recipe must compress a water cell into a snow block");
        helper.assertTrue(
                compressor.energy().stored() == 0,
                "Compressing the water cell costs 600 EU");
        helper.succeed();
    }

    private FluidIngredientTests() {}
}
