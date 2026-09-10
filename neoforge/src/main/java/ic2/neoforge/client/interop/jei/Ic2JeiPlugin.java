package ic2.neoforge.client.interop.jei;

import ic2.core.recipe.ProcessingMethod;
import ic2.neoforge.IndustrialCraft;
import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.recipe.ProcessingRecipe;
import ic2.neoforge.registration.ModMachines;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.recipe.types.IRecipeType;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * JEI integration (M14). Loaded only when JEI is installed: JEI discovers this class
 * through the plugin annotation, nothing else references it, so an IC2 install without
 * JEI never loads it. Covers the counted-input processing families this slice; other
 * machine recipe types (canner, thermal, centrifuge, ...) follow in later slices.
 */
@JeiPlugin
public final class Ic2JeiPlugin implements IModPlugin {
    private static final Map<ProcessingMethod, IRecipeType<RecipeHolder<ProcessingRecipe>>> TYPES =
            buildTypes();

    @SuppressWarnings("unchecked")
    private static Map<ProcessingMethod, IRecipeType<RecipeHolder<ProcessingRecipe>>> buildTypes() {
        Class<RecipeHolder<ProcessingRecipe>> clazz =
                (Class<RecipeHolder<ProcessingRecipe>>) (Class<?>) RecipeHolder.class;
        Map<ProcessingMethod, IRecipeType<RecipeHolder<ProcessingRecipe>>> types =
                new EnumMap<>(ProcessingMethod.class);
        for (ProcessingMethod method : ProcessingMethod.values()) {
            types.put(method, IRecipeType.create(IndustrialCraft.MOD_ID, method.id(), clazz));
        }
        return Collections.unmodifiableMap(types);
    }

    private static MachineKind machineOf(ProcessingMethod method) {
        return switch (method) {
            case MACERATOR -> MachineKind.MACERATOR;
            case EXTRACTOR -> MachineKind.EXTRACTOR;
            case COMPRESSOR -> MachineKind.COMPRESSOR;
            case METAL_FORMER_EXTRUDING, METAL_FORMER_ROLLING, METAL_FORMER_CUTTING ->
                    MachineKind.METAL_FORMER;
            case BLOCK_CUTTER -> MachineKind.BLOCK_CUTTER;
        };
    }

    @Override
    public Identifier getPluginUid() {
        return Identifier.fromNamespaceAndPath(IndustrialCraft.MOD_ID, "jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper guiHelper = registration.getJeiHelpers().getGuiHelper();
        IDrawable arrow = guiHelper.getRecipeArrow();
        List<IRecipeCategory<?>> categories = new ArrayList<>();
        for (ProcessingMethod method : ProcessingMethod.values()) {
            IDrawable icon =
                    guiHelper.createDrawableItemLike(ModMachines.block(machineOf(method)));
            categories.add(
                    new MachineProcessingCategory(
                            TYPES.get(method),
                            Component.translatable("jei.ic2.category." + method.id()),
                            icon,
                            arrow));
        }
        registration.addRecipeCategories(categories.toArray(IRecipeCategory[]::new));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        for (ProcessingMethod method : ProcessingMethod.values()) {
            registration.addRecipes(TYPES.get(method), ClientRecipeCache.processing(method));
        }
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        for (ProcessingMethod method : ProcessingMethod.values()) {
            registration.addCraftingStation(
                    TYPES.get(method), ModMachines.block(machineOf(method)));
        }
    }
}
