package ic2.neoforge.uu;

import ic2.core.recipe.ProcessingMethod;
import ic2.core.uu.UuValueGraph;
import ic2.neoforge.registration.ModProcessingRecipes;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the shared UU value graph lazily: base-resource seeds plus every processing-family
 * transformation (cost 0) from the loaded recipe manager. Crafting and smelting stay out until
 * their value semantics are ported (see agent.md §5).
 */
public final class UuValues {
    private static final Object LOCK = new Object();
    private static UuValueGraph graph;
    private static ServerLevel builtFor;

    public static UuValueGraph graph(ServerLevel level) {
        synchronized (LOCK) {
            if (graph == null || builtFor != level) {
                var fresh = new UuValueGraph();
                seed(fresh);
                collect(fresh, level);
                fresh.build();
                graph = fresh;
                builtFor = level;
            }
            return graph;
        }
    }

    /**
     * Seed table with the classic per-resource values: cheap crops (1), rubber and organics (3),
     * redstone (10), the base metal family (14), precious metals (56) and gems (112).
     */
    private static void seed(UuValueGraph graph) {
        if (!datapackSeeds.isEmpty()) {
            for (var seed : datapackSeeds) graph.setInitial(seed.item(), seed.value());
            return;
        }
        graph.setInitial("minecraft:iron_ore", 14);
        graph.setInitial("minecraft:deepslate_iron_ore", 14);
        graph.setInitial("minecraft:copper_ore", 14);
        graph.setInitial("minecraft:gold_ore", 56);
        graph.setInitial("ic2:uranium_ore", 14);
        graph.setInitial("ic2:lead_ore", 14);
        graph.setInitial("ic2:tin_ore", 14);
        graph.setInitial("minecraft:iron_ingot", 14);
        graph.setInitial("minecraft:copper_ingot", 14);
        graph.setInitial("minecraft:gold_ingot", 56);
        graph.setInitial("ic2:rubber", 3);
        graph.setInitial("ic2:sticky_resin", 3);
        graph.setInitial("minecraft:redstone", 10);
        graph.setInitial("minecraft:lapis_lazuli", 14);
        graph.setInitial("minecraft:coal", 8);
        graph.setInitial("minecraft:diamond", 112);
        graph.setInitial("minecraft:wheat", 1);
        graph.setInitial("minecraft:potato", 1);
        graph.setInitial("minecraft:carrot", 1);
        graph.setInitial("ic2:scrap", 1);
    }

    private static void collect(UuValueGraph graph, ServerLevel level) {
        var recipeMap = level.recipeAccess().recipeMap();
        for (ProcessingMethod family : ProcessingMethod.values()) {
            var type = ModProcessingRecipes.type(family);
            for (var holder : recipeMap.byType(type)) {
                var recipe = holder.value();
                var alternatives = new ArrayList<List<String>>();
                // items() also resolves custom ingredients (e.g. ic2:fluid); getValues() throws.
                recipe.ingredient()
                        .items()
                        .forEach(itemHolder ->
                                alternatives.add(List.of(
                                        ic2.neoforge.util.ItemKeys.id(itemHolder.value()))));
                var outputs = new ArrayList<String>();
                for (var output : recipe.outputs()) {
                    outputs.add(ic2.neoforge.util.ItemKeys.id(
                            output.stack().item().value()));
                }
                graph.addTransformation(
                        new UuValueGraph.Transformation(0.0, alternatives, outputs));
            }
        }
    }

    /** Replaces the datapack seed set and invalidates the built graph. */
    public static void applySeeds(Iterable<UuSeed> seeds) {
        datapackSeeds.clear();
        for (var seed : seeds) datapackSeeds.add(seed);
        graph = null;
    }

    private static final List<UuSeed> datapackSeeds = new ArrayList<>();

    private UuValues() {}
}
