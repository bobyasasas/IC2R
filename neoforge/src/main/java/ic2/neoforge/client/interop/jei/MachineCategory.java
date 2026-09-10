package ic2.neoforge.client.interop.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;

/** Shared input/arrow/outputs canvas for the remaining machine recipe families. */
public final class MachineCategory<T extends Recipe<?>> extends AbstractRecipeCategory<RecipeHolder<T>> {
    public static final int WIDTH = 116;
    public static final int HEIGHT = 62;

    /** Draws one recipe's slots; recipes differ too much to share a hardcoded layout. */
    public interface Layout<T> {
        void build(IRecipeLayoutBuilder builder, T recipe);
    }

    private final IDrawable arrow;
    private final int arrowX;
    private final Layout<T> layout;

    public MachineCategory(
            IRecipeType<RecipeHolder<T>> type,
            Component title,
            IDrawable icon,
            IDrawable arrow,
            int arrowX,
            Layout<T> layout) {
        super(type, title, icon, WIDTH, HEIGHT);
        this.arrow = arrow;
        this.arrowX = arrowX;
        this.layout = layout;
    }

    @Override
    public void setRecipe(
            IRecipeLayoutBuilder builder,
            RecipeHolder<T> holder,
            IFocusGroup focuses) {
        layout.build(builder, holder.value());
    }

    @Override
    public void draw(
            RecipeHolder<T> recipe,
            IRecipeSlotsView recipeSlotsView,
            GuiGraphicsExtractor guiGraphics,
            double mouseX,
            double mouseY) {
        arrow.draw(guiGraphics, arrowX, 23);
    }
}
