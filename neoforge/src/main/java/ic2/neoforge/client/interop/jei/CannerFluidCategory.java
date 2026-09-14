package ic2.neoforge.client.interop.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** JEI presentation for the canner's capability-driven container filling and emptying modes. */
final class CannerFluidCategory extends AbstractRecipeCategory<CannerFluidRecipe> {
    enum Operation {
        FILL,
        EMPTY
    }

    private final IDrawable arrow;
    private final Operation operation;

    CannerFluidCategory(
            IRecipeType<CannerFluidRecipe> type,
            Component title,
            IDrawable icon,
            IDrawable arrow,
            Operation operation) {
        super(type, title, icon, MachineCategory.WIDTH, MachineCategory.HEIGHT);
        this.arrow = arrow;
        this.operation = operation;
    }

    @Override
    public void setRecipe(
            IRecipeLayoutBuilder builder, CannerFluidRecipe recipe, IFocusGroup focuses) {
        if (operation == Operation.FILL) {
            CategoryLayouts.fluidInput(builder, 4, 22, recipe.fluid().getFluid(), recipe.amount());
            CategoryLayouts.itemInput(builder, 28, 22, recipe.emptyStack());
            CategoryLayouts.itemOutput(builder, 92, 22, recipe.filledStack());
        } else {
            CategoryLayouts.itemInput(builder, 4, 22, recipe.filledStack());
            if (!recipe.emptyContainer().isEmpty())
                CategoryLayouts.itemOutput(builder, 72, 22, recipe.emptyStack());
            CategoryLayouts.fluidOutput(
                    builder, 94, 22, recipe.fluid().getFluid(), recipe.amount());
        }
    }

    @Override
    public void draw(
            CannerFluidRecipe recipe,
            IRecipeSlotsView recipeSlotsView,
            GuiGraphicsExtractor graphics,
            double mouseX,
            double mouseY) {
        arrow.draw(graphics, operation == Operation.FILL ? 60 : 38, 23);
    }
}
