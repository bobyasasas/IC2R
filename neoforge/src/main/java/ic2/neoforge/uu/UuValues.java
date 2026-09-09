package ic2.neoforge.uu;

import ic2.core.recipe.ProcessingMethod;
import ic2.core.uu.UuValueGraph;
import ic2.neoforge.registration.ModProcessingRecipes;

import net.minecraft.server.level.ServerLevel;
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

    private static void seed(UuValueGraph graph) {
        graph.setInitial("minecraft:iron_ore", 14);
        graph.setInitial("minecraft:deepslate_iron_ore", 14);
        graph.setInitial("minecraft:copper_ore", 14);
        graph.setInitial("minecraft:gold_ore", 56);
        graph.setInitial("ic2:uranium_ore", 14);
        graph.setInitial("ic2:lead_ore", 14);
        graph.setInitial("ic2:tin_ore", 14);
    }

    private static void collect(UuValueGraph graph, ServerLevel level) {
        var recipeMap = level.recipeAccess().recipeMap();
        for (ProcessingMethod family : ProcessingMethod.values()) {
            var type = ModProcessingRecipes.type(family);
            for (var holder : recipeMap.byType(type)) {
                var recipe = holder.value();
                var alternatives = new ArrayList<List<String>>();
                for (var itemHolder : recipe.ingredient().getValues()) {
                    var group = new ArrayList<String>(1);
                    group.add(ic2.neoforge.util.ItemKeys.id(itemHolder.value()));
                    alternatives.add(group);
                }
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

    private UuValues() {}
}
