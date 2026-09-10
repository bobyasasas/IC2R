package ic2.neoforge.client.interop.jei;

import ic2.core.recipe.ProcessingMethod;
import ic2.neoforge.IndustrialCraft;
import ic2.neoforge.fluid.FluidDefinition;
import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.recipe.BlastFurnaceRecipe;
import ic2.neoforge.recipe.CentrifugeRecipe;
import ic2.neoforge.recipe.CoolingRecipe;
import ic2.neoforge.recipe.ElectrolyzingRecipe;
import ic2.neoforge.recipe.EnrichingRecipe;
import ic2.neoforge.recipe.FermentingRecipe;
import ic2.neoforge.recipe.MatterFabricatorRecipe;
import ic2.neoforge.recipe.ProcessingRecipe;
import ic2.neoforge.recipe.SolidCanningRecipe;
import ic2.neoforge.recipe.WashingRecipe;
import ic2.neoforge.registration.ModCannerRecipes;
import ic2.neoforge.registration.ModFluids;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.registration.ModProcessingRecipes;
import ic2.neoforge.registration.ModThermalRecipes;

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
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.material.Fluids;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * JEI integration (M14). Loaded only when JEI is installed: JEI discovers this class
 * through the plugin annotation, nothing else references it, so an IC2 install without
 * JEI never loads it. Covers the counted-input processing families and the remaining
 * machine recipe families (canner, thermal, ore washing, centrifuge, blast furnace,
 * matter fabricator). ElectricCraftingRecipe stays on the vanilla crafting category.
 */
@JeiPlugin
public final class Ic2JeiPlugin implements IModPlugin {
    private static final Map<ProcessingMethod, IRecipeType<RecipeHolder<ProcessingRecipe>>> TYPES =
            buildTypes();

    private static final IRecipeType<RecipeHolder<SolidCanningRecipe>> CANNER_BOTTLING =
            type("canner_bottle");
    private static final IRecipeType<RecipeHolder<EnrichingRecipe>> CANNER_ENRICHING =
            type("canner_enrich");
    private static final IRecipeType<RecipeHolder<FermentingRecipe>> FERMENTING =
            type("fermenting");
    private static final IRecipeType<RecipeHolder<CoolingRecipe>> COOLING = type("cooling");
    private static final IRecipeType<RecipeHolder<ElectrolyzingRecipe>> ELECTROLYZING =
            type("electrolyzing");
    private static final IRecipeType<RecipeHolder<WashingRecipe>> ORE_WASHING = type("ore_washing");
    private static final IRecipeType<RecipeHolder<CentrifugeRecipe>> CENTRIFUGE = type("centrifuge");
    private static final IRecipeType<RecipeHolder<BlastFurnaceRecipe>> BLAST_FURNACE =
            type("blast_furnace");
    private static final IRecipeType<RecipeHolder<MatterFabricatorRecipe>> MATTER_FABRICATOR =
            type("matter_fabricator");

    @SuppressWarnings("unchecked")
    private static <T extends Recipe<?>> IRecipeType<RecipeHolder<T>> type(String path) {
        Class<RecipeHolder<T>> clazz = (Class<RecipeHolder<T>>) (Class<?>) RecipeHolder.class;
        return IRecipeType.create(IndustrialCraft.MOD_ID, path, clazz);
    }

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

    private static FluidDefinition fluidOf(MachineKind machine) {
        return switch (machine) {
            case BLAST_FURNACE -> FluidDefinition.AIR;
            case MATTER_GENERATOR -> FluidDefinition.UU_MATTER;
            default -> throw new IllegalArgumentException(machine.name());
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
            categories.add(
                    new MachineProcessingCategory(
                            TYPES.get(method),
                            Component.translatable("jei.ic2.category." + method.id()),
                            icon(guiHelper, machineOf(method)),
                            arrow));
        }
        IDrawable canner = icon(guiHelper, MachineKind.CANNER);
        categories.add(
                new MachineCategory<>(
                        CANNER_BOTTLING,
                        Component.translatable("jei.ic2.category.canner_bottle"),
                        canner,
                        arrow,
                        60,
                        (builder, recipe) -> {
                            CategoryLayouts.itemInput(builder, 4, 22, recipe.container());
                            CategoryLayouts.itemInput(builder, 38, 22, recipe.additive());
                            CategoryLayouts.itemOutput(builder, 88, 22, recipe.result());
                        }));
        categories.add(
                new MachineCategory<>(
                        CANNER_ENRICHING,
                        Component.translatable("jei.ic2.category.canner_enrich"),
                        canner,
                        arrow,
                        60,
                        (builder, recipe) -> {
                            CategoryLayouts.fluidInput(builder, 4, 22, recipe.inputFluid());
                            CategoryLayouts.itemInput(builder, 38, 22, recipe.additive());
                            CategoryLayouts.fluidOutput(builder, 88, 22, recipe.result());
                        }));
        categories.add(
                new MachineCategory<>(
                        FERMENTING,
                        Component.translatable("jei.ic2.category.fermenting"),
                        icon(guiHelper, MachineKind.FERMENTER),
                        arrow,
                        46,
                        (builder, recipe) -> {
                            CategoryLayouts.fluidInput(
                                    builder,
                                    4,
                                    22,
                                    recipe.input(),
                                    Component.translatable("jei.ic2.requires_heat", recipe.heat()));
                            CategoryLayouts.fluidOutput(
                                    builder,
                                    88,
                                    22,
                                    recipe.result(),
                                    Component.translatable(
                                            "jei.ic2.fertilizer_interval", recipe.fertilizerInterval()));
                        }));
        categories.add(
                new MachineCategory<>(
                        COOLING,
                        Component.translatable("jei.ic2.category.cooling"),
                        icon(guiHelper, MachineKind.LIQUID_HEAT_EXCHANGER),
                        arrow,
                        46,
                        (builder, recipe) -> {
                            CategoryLayouts.fluidInput(
                                    builder,
                                    4,
                                    22,
                                    recipe.input(),
                                    Component.translatable("jei.ic2.requires_heat", recipe.heat()));
                            CategoryLayouts.fluidOutput(builder, 88, 22, recipe.result());
                        }));
        categories.add(
                new MachineCategory<>(
                        ELECTROLYZING,
                        Component.translatable("jei.ic2.category.electrolyzing"),
                        icon(guiHelper, MachineKind.ELECTROLYZER),
                        arrow,
                        46,
                        (builder, recipe) -> {
                            CategoryLayouts.fluidInput(
                                    builder,
                                    4,
                                    22,
                                    recipe.input(),
                                    Component.translatable("jei.ic2.eu_per_tick", recipe.euPerTick()),
                                    Component.translatable("jei.ic2.duration", recipe.ticks()));
                            int y = 4;
                            for (ElectrolyzingRecipe.Output output : recipe.outputs()) {
                                CategoryLayouts.fluidOutput(
                                        builder,
                                        88,
                                        y,
                                        output.fluid(),
                                        Component.translatable(
                                                "jei.ic2.side", output.direction().getSerializedName()));
                                y += 20;
                            }
                        }));
        categories.add(
                new MachineCategory<>(
                        ORE_WASHING,
                        Component.translatable("jei.ic2.category.ore_washer"),
                        icon(guiHelper, MachineKind.ORE_WASHING_PLANT),
                        arrow,
                        46,
                        (builder, recipe) -> {
                            CategoryLayouts.itemInput(
                                    builder, 4, 22, recipe.ingredient(), recipe.inputCount());
                            CategoryLayouts.fluidInput(
                                    builder, 26, 22, Fluids.WATER, recipe.water());
                            int y = 4;
                            for (var output : recipe.outputs()) {
                                CategoryLayouts.itemOutput(builder, 88, y, output);
                                y += 20;
                            }
                        }));
        categories.add(
                new MachineCategory<>(
                        CENTRIFUGE,
                        Component.translatable("jei.ic2.category.centrifuge"),
                        icon(guiHelper, MachineKind.CENTRIFUGE),
                        arrow,
                        46,
                        (builder, recipe) -> {
                            CategoryLayouts.itemInput(
                                    builder,
                                    4,
                                    22,
                                    recipe.ingredient(),
                                    recipe.inputCount(),
                                    Component.translatable("jei.ic2.requires_heat", recipe.minHeat()));
                            int y = 4;
                            for (var output : recipe.outputs()) {
                                CategoryLayouts.itemOutput(builder, 88, y, output);
                                y += 20;
                            }
                        }));
        categories.add(
                new MachineCategory<>(
                        BLAST_FURNACE,
                        Component.translatable("jei.ic2.category.blast_furnace"),
                        icon(guiHelper, MachineKind.BLAST_FURNACE),
                        arrow,
                        46,
                        (builder, recipe) -> {
                            CategoryLayouts.itemInput(
                                    builder,
                                    4,
                                    22,
                                    recipe.ingredient(),
                                    recipe.inputCount(),
                                    Component.translatable("jei.ic2.duration", recipe.duration()));
                            if (recipe.fluid() > 0)
                                CategoryLayouts.fluidInput(
                                        builder,
                                        26,
                                        22,
                                        ModFluids.FAMILIES.get(fluidOf(MachineKind.BLAST_FURNACE))
                                                .source()
                                                .get(),
                                        recipe.fluid());
                            int y = 4;
                            for (var output : recipe.outputs()) {
                                CategoryLayouts.itemOutput(builder, 88, y, output);
                                y += 20;
                            }
                        }));
        categories.add(
                new MachineCategory<>(
                        MATTER_FABRICATOR,
                        Component.translatable("jei.ic2.category.matter_fabricator"),
                        icon(guiHelper, MachineKind.MATTER_GENERATOR),
                        arrow,
                        46,
                        (builder, recipe) -> {
                            CategoryLayouts.itemInput(
                                    builder, 4, 22, recipe.ingredient(), recipe.inputCount());
                            CategoryLayouts.fluidOutput(
                                    builder,
                                    88,
                                    22,
                                    ModFluids.FAMILIES.get(fluidOf(MachineKind.MATTER_GENERATOR))
                                            .source()
                                            .get(),
                                    recipe.result());
                        }));
        registration.addRecipeCategories(categories.toArray(IRecipeCategory[]::new));
    }

    private static IDrawable icon(IGuiHelper guiHelper, MachineKind machine) {
        return guiHelper.createDrawableItemLike(ModMachines.block(machine));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        for (ProcessingMethod method : ProcessingMethod.values()) {
            registration.addRecipes(TYPES.get(method), ClientRecipeCache.processing(method));
        }
        registration.addRecipes(
                CANNER_BOTTLING, ClientRecipeCache.byType(ModCannerRecipes.SOLID.get()));
        registration.addRecipes(
                CANNER_ENRICHING, ClientRecipeCache.byType(ModCannerRecipes.ENRICH.get()));
        registration.addRecipes(
                FERMENTING, ClientRecipeCache.byType(ModThermalRecipes.FERMENTING.get()));
        registration.addRecipes(COOLING, ClientRecipeCache.byType(ModThermalRecipes.COOLING.get()));
        registration.addRecipes(
                ELECTROLYZING, ClientRecipeCache.byType(ModThermalRecipes.ELECTROLYZING.get()));
        registration.addRecipes(
                ORE_WASHING, ClientRecipeCache.byType(ModProcessingRecipes.WASHING_TYPE.get()));
        registration.addRecipes(
                CENTRIFUGE, ClientRecipeCache.byType(ModProcessingRecipes.CENTRIFUGE_TYPE.get()));
        registration.addRecipes(
                BLAST_FURNACE,
                ClientRecipeCache.byType(ModProcessingRecipes.BLAST_FURNACE_TYPE.get()));
        registration.addRecipes(
                MATTER_FABRICATOR,
                ClientRecipeCache.byType(ModProcessingRecipes.MATTER_FABRICATOR_TYPE.get()));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        for (ProcessingMethod method : ProcessingMethod.values()) {
            registration.addCraftingStation(
                    TYPES.get(method), ModMachines.block(machineOf(method)));
        }
        registration.addCraftingStation(CANNER_BOTTLING, ModMachines.block(MachineKind.CANNER));
        registration.addCraftingStation(CANNER_ENRICHING, ModMachines.block(MachineKind.CANNER));
        registration.addCraftingStation(FERMENTING, ModMachines.block(MachineKind.FERMENTER));
        registration.addCraftingStation(
                COOLING, ModMachines.block(MachineKind.LIQUID_HEAT_EXCHANGER));
        registration.addCraftingStation(
                ELECTROLYZING, ModMachines.block(MachineKind.ELECTROLYZER));
        registration.addCraftingStation(
                ORE_WASHING, ModMachines.block(MachineKind.ORE_WASHING_PLANT));
        registration.addCraftingStation(CENTRIFUGE, ModMachines.block(MachineKind.CENTRIFUGE));
        registration.addCraftingStation(
                BLAST_FURNACE, ModMachines.block(MachineKind.BLAST_FURNACE));
        registration.addCraftingStation(
                MATTER_FABRICATOR, ModMachines.block(MachineKind.MATTER_GENERATOR));
    }
}
