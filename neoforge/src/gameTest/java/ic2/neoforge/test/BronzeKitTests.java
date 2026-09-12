package ic2.neoforge.test;

import ic2.neoforge.item.MetalArmorLike;
import ic2.neoforge.registration.MaterialDefinition;
import ic2.neoforge.registration.ModArmor;
import ic2.neoforge.registration.ModItems;
import ic2.neoforge.registration.ModTools;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;

import java.util.List;

final class BronzeKitTests {
    static void toolsCraftWithLegacyStats(GameTestHelper helper) {
        var ingot =
                new ItemStack(ModItems.MATERIALS.get(MaterialDefinition.BRONZE_INGOT).get());
        var stick = new ItemStack(Items.STICK);
        var recipe =
                (CraftingRecipe)
                        helper.getLevel()
                                .recipeAccess()
                                .byKey(ResourceKey.create(
                                        Registries.RECIPE,
                                        Identifier.parse("ic2:shaped/bronze_pickaxe")))
                                .orElseThrow()
                                .value();
        var grid =
                List.of(
                        ingot.copy(), ingot.copy(), ingot.copy(),
                        ItemStack.EMPTY, stick.copy(), ItemStack.EMPTY,
                        ItemStack.EMPTY, stick.copy(), ItemStack.EMPTY);
        var input = CraftingInput.of(3, 3, grid);
        helper.assertTrue(
                recipe.matches(input, helper.getLevel()),
                "Bronze pickaxe must craft from 3 ingots over 2 sticks");
        helper.assertTrue(
                recipe.assemble(input).is(ModTools.BRONZE_PICKAXE.get()),
                "The pickaxe recipe must assemble the registered item");
        var pickaxe = new ItemStack(ModTools.BRONZE_PICKAXE.get());
        Tool tool = pickaxe.get(DataComponents.TOOL);
        helper.assertTrue(
                tool != null && pickaxe.getMaxDamage() == 350,
                "Legacy Ic2ToolMaterials.BRONZE: 350 uses with a working tool component");
        var sword = new ItemStack(ModTools.BRONZE_SWORD.get());
        helper.assertTrue(
                sword.get(DataComponents.TOOL) != null && sword.getMaxDamage() == 350,
                "The sword shares the bronze tool material");
        helper.succeed();
    }

    static void armorCraftsAndCarriesLegacyDefence(GameTestHelper helper) {
        var ingot =
                new ItemStack(ModItems.MATERIALS.get(MaterialDefinition.BRONZE_INGOT).get());
        var recipe =
                (CraftingRecipe)
                        helper.getLevel()
                                .recipeAccess()
                                .byKey(ResourceKey.create(
                                        Registries.RECIPE,
                                        Identifier.parse("ic2:shaped/bronze_helmet")))
                                .orElseThrow()
                                .value();
        var input =
                CraftingInput.of(
                        3,
                        3,
                        List.of(
                                ingot.copy(),
                                ingot.copy(),
                                ingot.copy(),
                                ingot.copy(),
                                ItemStack.EMPTY,
                                ingot.copy(),
                                ItemStack.EMPTY,
                                ItemStack.EMPTY,
                                ItemStack.EMPTY));
        helper.assertTrue(
                recipe.matches(input, helper.getLevel()),
                "Bronze helmet must craft from 5 ingots");
        var armor = List.of(
                new ItemStack(ModArmor.BRONZE_HELMET.get()),
                new ItemStack(ModArmor.BRONZE_CHESTPLATE.get()),
                new ItemStack(ModArmor.BRONZE_LEGGINGS.get()),
                new ItemStack(ModArmor.BRONZE_BOOTS.get()));
        var durability = List.of(165, 240, 225, 195);
        for (int i = 0; i < armor.size(); i++) {
            var stack = armor.get(i);
            helper.assertTrue(
                    stack.getItem() instanceof MetalArmorLike,
                    "Legacy ItemArmorIC2 is metal armor: " + stack);
            helper.assertTrue(
                    stack.getMaxDamage() == durability.get(i),
                    "Legacy bronze durability 15× per piece, expected "
                            + durability.get(i)
                            + " on "
                            + stack);
        }
        helper.assertTrue(
                new ItemStack(ModArmor.BRONZE_HELMET.get())
                                .getItem()
                        instanceof ic2.neoforge.item.BronzeArmorItem,
                "Bronze armor ships as the metal-armor item class");
        helper.succeed();
    }

    private BronzeKitTests() {}
}
