package ic2.neoforge.client.interop.jei;

import ic2.neoforge.recipe.ProcessingRecipe;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeHolder;

/** One layout shared by the counted-input processing families; input, arrow, outputs. */
public final class MachineProcessingCategory
        extends AbstractRecipeCategory<RecipeHolder<ProcessingRecipe>> {
    public static final int WIDTH = 100;
    public static final int HEIGHT = 62;

    private final IDrawable arrow;

    public MachineProcessingCategory(
            IRecipeType<RecipeHolder<ProcessingRecipe>> type,
            Component title,
            IDrawable icon,
            IDrawable arrow) {
        super(type, title, icon, WIDTH, HEIGHT);
        this.arrow = arrow;
    }

    @Override
    public void setRecipe(
            IRecipeLayoutBuilder builder,
            RecipeHolder<ProcessingRecipe> holder,
            IFocusGroup focuses) {
        ProcessingRecipe recipe = holder.value();
        builder.addInputSlot(4, 22)
                .setStandardSlotBackground()
                .add(recipe.ingredient())
                .addRichTooltipCallback(
                        (view, tooltip) -> {
                            if (recipe.inputCount() > 1)
                                tooltip.add(
                                        Component.translatable(
                                                "jei.ic2.input_count", recipe.inputCount()));
                        });
        int y = 4;
        for (ProcessingRecipe.Output output : recipe.outputs()) {
            int weight = output.weight();
            builder.addOutputSlot(78, y)
                    .setOutputSlotBackground()
                    .add(output.stack())
                    .addRichTooltipCallback(
                            (view, tooltip) ->
                                    tooltip.add(
                                            Component.translatable(
                                                    "jei.ic2.output_weight", weight)));
            y += 20;
        }
    }

    @Override
    public void draw(
            RecipeHolder<ProcessingRecipe> recipe,
            IRecipeSlotsView recipeSlotsView,
            GuiGraphicsExtractor guiGraphics,
            double mouseX,
            double mouseY) {
        arrow.draw(guiGraphics, 32, 23);
    }
}
