package ic2.neoforge.client.interop.jei;

import ic2.core.machine.MetalFormerMode;
import ic2.core.recipe.ProcessingMethod;
import ic2.neoforge.client.MachineScreen;
import ic2.neoforge.machine.MachineKind;

import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.handlers.IGuiClickableArea;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.recipe.types.IRecipeType;

import java.util.Collection;
import java.util.List;

/** Maps the controls painted in each machine GUI to the recipe categories they represent. */
final class MachineRecipeLinks implements IGuiContainerHandler<MachineScreen> {
    @Override
    public Collection<IGuiClickableArea> getGuiClickableAreas(
            MachineScreen screen, double mouseX, double mouseY) {
        MachineKind kind = screen.getMenu().kind();
        return switch (kind) {
            case IRON_FURNACE ->
                    List.of(
                            link(56, 36, 13, 13, RecipeTypes.SMELTING_FUEL),
                            link(80, 35, 22, 15, RecipeTypes.SMELTING));
            case GENERATOR -> List.of(link(57, 36, 13, 13, RecipeTypes.SMELTING_FUEL));
            case SOLID_HEAT_GENERATOR -> List.of(link(81, 29, 13, 13, RecipeTypes.SMELTING_FUEL));
            case ELECTRIC_FURNACE -> List.of(link(80, 35, 22, 15, RecipeTypes.SMELTING));
            case INDUCTION_FURNACE -> List.of(link(81, 35, 22, 15, RecipeTypes.SMELTING));
            case MACERATOR -> List.of(link(80, 38, 21, 11, processing(ProcessingMethod.MACERATOR)));
            case EXTRACTOR -> List.of(link(80, 35, 22, 15, processing(ProcessingMethod.EXTRACTOR)));
            case COMPRESSOR ->
                    List.of(link(80, 35, 22, 15, processing(ProcessingMethod.COMPRESSOR)));
            case METAL_FORMER ->
                    List.of(
                            link(
                                    52,
                                    39,
                                    46,
                                    9,
                                    processing(
                                            MetalFormerMode.byId(screen.getMenu().familyValue(0))
                                                    .method())));
            case BLOCK_CUTTER ->
                    List.of(
                            link(55, 33, 15, 19, processing(ProcessingMethod.BLOCK_CUTTER)),
                            link(86, 33, 15, 19, processing(ProcessingMethod.BLOCK_CUTTER)));
            case ORE_WASHING_PLANT -> List.of(link(103, 39, 18, 18, Ic2JeiPlugin.ORE_WASHING));
            case CENTRIFUGE -> List.of(link(84, 25, 3, 28, Ic2JeiPlugin.CENTRIFUGE));
            case BLAST_FURNACE -> List.of(link(80, 35, 22, 15, Ic2JeiPlugin.BLAST_FURNACE));
            case CANNER ->
                    List.of(
                            link(
                                    74,
                                    22,
                                    23,
                                    14,
                                    switch (Math.clamp(screen.getMenu().cannerMode(), 0, 3)) {
                                        case 1 -> Ic2JeiPlugin.CANNER_EMPTYING;
                                        case 2 -> Ic2JeiPlugin.CANNER_FILLING;
                                        case 3 -> Ic2JeiPlugin.CANNER_ENRICHING;
                                        default -> Ic2JeiPlugin.CANNER_BOTTLING;
                                    }));
            case FERMENTER -> List.of(link(38, 88, 40, 7, Ic2JeiPlugin.FERMENTING));
            case LIQUID_HEAT_EXCHANGER -> List.of(link(48, 58, 94, 24, Ic2JeiPlugin.COOLING));
            case STIRLING_KINETIC_GENERATOR -> List.of(link(48, 58, 94, 12, Ic2JeiPlugin.HEATING));
            case ELECTROLYZER -> List.of(link(39, 36, 96, 23, Ic2JeiPlugin.ELECTROLYZING));
            case MATTER_GENERATOR -> List.of(link(96, 22, 20, 55, Ic2JeiPlugin.MATTER_FABRICATOR));
            case BATCH_CRAFTER -> List.of(link(90, 35, 22, 15, RecipeTypes.CRAFTING));
            default -> List.of();
        };
    }

    private static IRecipeType<?> processing(ProcessingMethod method) {
        return Ic2JeiPlugin.TYPES.get(method);
    }

    private static IGuiClickableArea link(
            int x, int y, int width, int height, IRecipeType<?>... types) {
        return IGuiClickableArea.createBasic(x, y, width, height, types);
    }
}
