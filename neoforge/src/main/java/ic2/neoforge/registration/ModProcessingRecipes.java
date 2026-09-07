package ic2.neoforge.registration;

import ic2.neoforge.IndustrialCraft;
import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.recipe.ProcessingRecipe;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.Map;

public final class ModProcessingRecipes {
    private static final DeferredRegister<RecipeType<?>> TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, IndustrialCraft.MOD_ID);
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, IndustrialCraft.MOD_ID);
    private static final DeferredRegister<RecipeBookCategory> CATEGORIES =
            DeferredRegister.create(Registries.RECIPE_BOOK_CATEGORY, IndustrialCraft.MOD_ID);
    public static final DeferredHolder<RecipeBookCategory, RecipeBookCategory> CATEGORY =
            CATEGORIES.register("processing", RecipeBookCategory::new);
    private static final Map<
                    MachineKind, DeferredHolder<RecipeType<?>, RecipeType<ProcessingRecipe>>>
            RECIPE_TYPES = new EnumMap<>(MachineKind.class);
    private static final Map<
                    MachineKind,
                    DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ProcessingRecipe>>>
            RECIPE_SERIALIZERS = new EnumMap<>(MachineKind.class);

    static {
        for (var kind :
                new MachineKind[] {
                    MachineKind.MACERATOR, MachineKind.EXTRACTOR, MachineKind.COMPRESSOR
                }) {
            RECIPE_TYPES.put(
                    kind,
                    TYPES.register(
                            kind.getSerializedName(),
                            () ->
                                    new RecipeType<ProcessingRecipe>() {
                                        @Override
                                        public String toString() {
                                            return "ic2:" + kind.getSerializedName();
                                        }
                                    }));
            RECIPE_SERIALIZERS.put(
                    kind,
                    SERIALIZERS.register(
                            kind.getSerializedName(),
                            () ->
                                    new RecipeSerializer<>(
                                            ProcessingRecipe.codec(kind),
                                            ProcessingRecipe.streamCodec(kind))));
        }
    }

    public static RecipeType<ProcessingRecipe> type(MachineKind kind) {
        return RECIPE_TYPES.get(kind).get();
    }

    public static RecipeSerializer<ProcessingRecipe> serializer(MachineKind kind) {
        return RECIPE_SERIALIZERS.get(kind).get();
    }

    public static void register(IEventBus bus) {
        TYPES.register(bus);
        SERIALIZERS.register(bus);
        CATEGORIES.register(bus);
    }

    private ModProcessingRecipes() {}
}
