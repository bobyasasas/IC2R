package ic2.neoforge.client.interop.jei;

import ic2.neoforge.recipe.CountedIngredient;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStackTemplate;

/** Slot helpers shared by the machine categories: backgrounds plus detail tooltips. */
final class CategoryLayouts {
    private static final int SLOT = 18;

    static void itemInput(
            IRecipeLayoutBuilder builder, int x, int y, Ingredient ingredient, int count,
            Component... tooltip) {
        IRecipeSlotBuilder slot = builder.addInputSlot(x, y).setStandardSlotBackground();
        if (count > 1)
            slot.addRichTooltipCallback(
                    (view, lines) -> lines.add(Component.translatable("jei.ic2.input_count", count)));
        addTooltip(slot, tooltip);
        slot.add(ingredient);
    }

    static void itemInput(
            IRecipeLayoutBuilder builder, int x, int y, CountedIngredient input, Component... tooltip) {
        itemInput(builder, x, y, input.ingredient(), input.count(), tooltip);
    }

    static void itemOutput(
            IRecipeLayoutBuilder builder, int x, int y, ItemStackTemplate stack, Component... tooltip) {
        addTooltip(builder.addOutputSlot(x, y).setOutputSlotBackground(), tooltip).add(stack);
    }

    static void fluidInput(
            IRecipeLayoutBuilder builder, int x, int y, FluidStackTemplate fluid, Component... tooltip) {
        fluidSlot(builder.addInputSlot(x, y), fluid, tooltip);
    }

    static void fluidInput(
            IRecipeLayoutBuilder builder, int x, int y, Fluid fluid, int amount, Component... tooltip) {
        fluidSlot(builder.addInputSlot(x, y), fluid, amount, tooltip);
    }

    static void fluidOutput(
            IRecipeLayoutBuilder builder, int x, int y, FluidStackTemplate fluid, Component... tooltip) {
        fluidSlot(builder.addOutputSlot(x, y), fluid, tooltip);
    }

    static void fluidOutput(
            IRecipeLayoutBuilder builder, int x, int y, Fluid fluid, int amount, Component... tooltip) {
        fluidSlot(builder.addOutputSlot(x, y), fluid, amount, tooltip);
    }

    private static void fluidSlot(
            IRecipeSlotBuilder slot, FluidStackTemplate fluid, Component... tooltip) {
        fluidSlot(slot, fluid.fluid().value(), fluid.amount(), tooltip);
    }

    private static void fluidSlot(IRecipeSlotBuilder slot, Fluid fluid, int amount, Component... tooltip) {
        slot.setStandardSlotBackground()
                .setFluidRenderer(Math.max(amount, 1), false, SLOT, SLOT)
                .add(fluid, amount);
        addTooltip(slot, tooltip);
    }

    private static IRecipeSlotBuilder addTooltip(IRecipeSlotBuilder slot, Component... tooltip) {
        if (tooltip.length > 0)
            slot.addRichTooltipCallback(
                    (view, lines) -> {
                        for (Component line : tooltip) lines.add(line);
                    });
        return slot;
    }

    private CategoryLayouts() {}
}
